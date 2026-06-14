import csv
from pathlib import Path

import pytest

from pages.login_page import LoginPage
from utils.db_helper import DbHelper


def _load_password_data():
    path = Path(__file__).parents[1] / "data" / "password_login_test_data.csv"
    with open(path, newline="", encoding="utf-8") as f:
        rows = list(csv.DictReader(f))
    return [pytest.param(row, id=row["case_id"]) for row in rows]


BANNED_EMAIL = "banned@test.com"
TEST_USER_EMAIL = "b1196530136@163.com"


class TestPasswordLogin:

    @pytest.mark.smoke
    @pytest.mark.ui
    @pytest.mark.parametrize("case", _load_password_data())
    def test_password_login(self, driver, base_url, case):
        case_id = case["case_id"]
        email = case["email"].strip() if case["email"] else ""
        password = case["password"].strip() if case["password"] else ""
        expected = case["expected_result"]
        expected_msg = case["expected_message"]

        # -- Reset shared state for deterministic test execution --
        with DbHelper() as db:
            db.reset_user_fail_count(TEST_USER_EMAIL)

        page = LoginPage(driver, base_url).open()

        # -- Handle lockout / banned test data setup --
        if case_id == "LGN-PWD-008":
            # 4 wrong attempts; each shows a distinct remaining-count message
            remaining = ["还剩4次机会", "还剩3次机会", "还剩2次机会", "还剩1次机会"]
            for hint in remaining:
                page.login_with_password(email, "wrongpassword")
                page.wait_for_toast_contains(hint)

        elif case_id == "LGN-PWD-009":
            # 5 wrong attempts to lock the account, first 4 show countdown, 5th locks
            for i in range(4):
                page.login_with_password(email, "wrongpassword")
                page.wait_for_toast_contains(["还剩4次机会", "还剩3次机会", "还剩2次机会", "还剩1次机会"][i])
            # 5th attempt triggers lockout
            page.login_with_password(email, "wrongpassword")
            page.wait_for_toast_contains("密码错误已达5次")

        elif case_id == "LGN-PWD-010":
            pytest.skip("Requires 30-minute lockout wait; run manually")

        elif case_id == "LGN-PWD-011":
            with DbHelper() as db:
                db.ensure_banned_user(BANNED_EMAIL)
            # Will be cleaned up in teardown

        elif case_id == "LGN-PWD-012":
            # Make one wrong attempt so there is a fail count to reset
            page.login_with_password(TEST_USER_EMAIL, "wrongpassword")
            page.wait_for_toast_contains("邮箱或密码错误")

        # -- Main test action --
        page.login_with_password(email, password)

        # -- Assertions --
        if expected == "success":
            page.wait.until(lambda d: "auth/login" not in d.current_url)
            assert page.session_token() is not None, "sessionStorage token should exist"
        else:
            if case_id in ("LGN-PWD-003", "LGN-PWD-004", "LGN-PWD-005"):
                page.wait.until(lambda d: any(len(t) > 0 for t in page.password_error_texts()))
                errors = page.password_error_texts()
                assert any(expected_msg in err for err in errors), \
                    f"Expected '{expected_msg}' in form errors, got: {errors}"
            elif case_id == "LGN-PWD-008":
                page.wait_for_toast_contains("密码错误已达5次")
                assert "密码错误已达5次" in page.toast_text()
            elif case_id == "LGN-PWD-009":
                # Lockout text may include a dynamic minute count, match by invariant prefix
                page.wait_for_toast_contains("账号已锁定")
                assert "账号已锁定" in page.toast_text()
            else:
                page.wait_for_toast_contains(expected_msg)
                assert expected_msg in page.toast_text()

        # -- Teardown: clean up test-only data --
        if case_id == "LGN-PWD-011":
            with DbHelper() as db:
                db.remove_user(BANNED_EMAIL)
