"""Database helper for test state management (verification codes, user state)."""

import pymysql

DB_CONFIG = {
    "host": "localhost",
    "port": 3306,
    "user": "root",
    "password": "123456",
    "database": "pet_recovery",
}


class DbHelper:
    """Minimal MySQL helper for test setup/teardown."""

    def __init__(self):
        self.conn = pymysql.connect(**DB_CONFIG)

    def get_latest_code(self, email: str) -> str | None:
        """Retrieve the latest unused, non-expired verification code for an email."""
        with self.conn.cursor() as cur:
            cur.execute(
                """SELECT code FROM sys_verify_code
                   WHERE email = %s AND used = 0 AND expire_at > NOW()
                   ORDER BY create_time DESC LIMIT 1""",
                (email,),
            )
            row = cur.fetchone()
        return row[0] if row else None

    def insert_expired_code(self, email: str, code: str = "000000"):
        """Insert an already-expired verification code for testing."""
        with self.conn.cursor() as cur:
            cur.execute(
                """INSERT INTO sys_verify_code (email, code, expire_at, used, create_time)
                   VALUES (%s, %s, DATE_SUB(NOW(), INTERVAL 1 MINUTE), 0, NOW())""",
                (email, code),
            )
        self.conn.commit()

    def get_and_mark_code_used(self, email: str) -> str | None:
        """Retrieve the latest valid code and mark it used (so it can't be reused)."""
        code = self.get_latest_code(email)
        if code:
            with self.conn.cursor() as cur:
                cur.execute(
                    "UPDATE sys_verify_code SET used = 1 WHERE email = %s AND code = %s ORDER BY create_time DESC LIMIT 1",
                    (email, code),
                )
            self.conn.commit()
        return code

    def reset_user_fail_count(self, email: str):
        """Reset login fail count and lock time for a user."""
        with self.conn.cursor() as cur:
            cur.execute(
                "UPDATE sys_user SET login_fail_count = 0, lock_time = NULL WHERE email = %s",
                (email,),
            )
        self.conn.commit()

    def ensure_banned_user(self, email: str):
        """Create a banned test user (status=0) if not already present."""
        with self.conn.cursor() as cur:
            cur.execute("SELECT id FROM sys_user WHERE email = %s", (email,))
            if not cur.fetchone():
                cur.execute(
                    """INSERT INTO sys_user (email, password, nickname, role, status, create_time)
                       VALUES (%s, 'dummy', %s, 'USER', 0, NOW())""",
                    (email, email.split("@")[0]),
                )
        self.conn.commit()

    def remove_user(self, email: str):
        """Remove a test user by email."""
        with self.conn.cursor() as cur:
            cur.execute("DELETE FROM sys_user WHERE email = %s", (email,))
        self.conn.commit()

    def close(self):
        self.conn.close()

    def __enter__(self):
        return self

    def __exit__(self, *args):
        self.close()
