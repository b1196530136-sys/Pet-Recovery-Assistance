import csv
from pathlib import Path

import pytest

from pages.login_page import LoginPage


def _load_code_data():
    path = Path(__file__).parents[1] / "data" / "code_login_test_data.csv"
    with open(path, newline="", encoding="utf-8") as f:
        rows = list(csv.DictReader(f))
    seen = {}
    params = []
    for row in rows:
        cid = row["case_id"]
        idx = seen.get(cid, 0) + 1
        seen[cid] = idx
        params.append(pytest.param(row, id=f"{cid}_{idx}"))
    return params


class TestSendCode:

    @pytest.mark.smoke
    @pytest.mark.ui
    @pytest.mark.parametrize("case", _load_code_data())
    def test_send_code(self, driver, base_url, case):
        case_id = case["case_id"]
        email = case["email"].strip() if case["email"] else ""
        expected = case["expected_result"]
        expected_msg = case["expected_message"]

        page = LoginPage(driver, base_url).open()
        page.switch_to_code_login()

        if email:
            page.type_text(page.CODE_EMAIL_INPUT, email)

        page.click_send_code()

        if case_id == "LGN-CODE-012":
            page.wait_for_toast_contains("验证码已发送")
            assert not page.is_send_code_button_enabled(), \
                "Send-code button should be disabled during countdown"
            import time
            time.sleep(1)
            btn_text = page.send_code_button_text()
            assert btn_text.endswith("s"), f"Expected countdown format 'Xs', got: '{btn_text}'"
        else:
            page.wait_for_toast_contains(expected_msg)
            assert expected_msg in page.toast_text()
