from selenium.webdriver.common.by import By
from selenium.webdriver.support import expected_conditions as EC

from pages.base_page import BasePage


class LoginPage(BasePage):
    path = "/auth/login"

    PASSWORD_TAB = (By.ID, "tab-password")
    CODE_TAB = (By.ID, "tab-code")

    PASSWORD_EMAIL_INPUT = (By.CSS_SELECTOR, "#pane-password input[placeholder='邮箱']")
    PASSWORD_INPUT = (By.CSS_SELECTOR, "#pane-password input[placeholder='密码']")
    PASSWORD_LOGIN_BUTTON = (
        By.XPATH,
        "//*[@id='pane-password']//button[.//span[normalize-space()='登录']]",
    )
    PASSWORD_FORM_ERRORS = (By.CSS_SELECTOR, "#pane-password .el-form-item__error")

    CODE_EMAIL_INPUT = (By.CSS_SELECTOR, "#pane-code input[placeholder='邮箱']")
    CODE_INPUT = (By.CSS_SELECTOR, "#pane-code input[placeholder='验证码']")
    SEND_CODE_BUTTON = (
        By.XPATH,
        "//*[@id='pane-code']//button[.//span[contains(normalize-space(), '发送验证码') or contains(normalize-space(), 's')]]",
    )
    CODE_LOGIN_BUTTON = (
        By.XPATH,
        "//*[@id='pane-code']//button[.//span[normalize-space()='登录']]",
    )
    CODE_FORM_ERRORS = (By.CSS_SELECTOR, "#pane-code .el-form-item__error")

    TOAST_MESSAGE = (By.CSS_SELECTOR, ".el-message .el-message__content")
    REGISTER_LINK = (By.XPATH, "//a[normalize-space()='没有账号？去注册']")
    BACK_HOME_LINK = (By.XPATH, "//a[contains(normalize-space(), '返回首页')]")
    FORGOT_PASSWORD_LINK = (By.XPATH, "//a[normalize-space()='忘记密码']")

    def login_with_password(self, email: str, password: str):
        self.switch_to_password_login()
        self.type_text(self.PASSWORD_EMAIL_INPUT, email)
        self.type_text(self.PASSWORD_INPUT, password)
        self.click(self.PASSWORD_LOGIN_BUTTON)
        return self

    def switch_to_password_login(self):
        self.click(self.PASSWORD_TAB)
        return self

    def switch_to_code_login(self):
        self.click(self.CODE_TAB)
        return self

    def send_login_code(self, email: str):
        self.switch_to_code_login()
        self.type_text(self.CODE_EMAIL_INPUT, email)
        self.click(self.SEND_CODE_BUTTON)
        return self

    def login_with_code(self, email: str, code: str):
        self.switch_to_code_login()
        self.type_text(self.CODE_EMAIL_INPUT, email)
        self.type_text(self.CODE_INPUT, code)
        self.click(self.CODE_LOGIN_BUTTON)
        return self

    def click_password_login(self):
        self.click(self.PASSWORD_LOGIN_BUTTON)
        return self

    def click_send_code(self):
        self.click(self.SEND_CODE_BUTTON)
        return self

    def click_code_login(self):
        self.click(self.CODE_LOGIN_BUTTON)
        return self

    def password_error_texts(self) -> list[str]:
        return [element.text for element in self.find_all(self.PASSWORD_FORM_ERRORS)]

    def code_error_texts(self) -> list[str]:
        return [element.text for element in self.find_all(self.CODE_FORM_ERRORS)]

    def toast_text(self) -> str:
        return self.text_of(self.TOAST_MESSAGE)

    def wait_for_toast_contains(self, expected_text: str):
        self.wait.until(EC.text_to_be_present_in_element(self.TOAST_MESSAGE, expected_text))
        return self

    def send_code_button_text(self) -> str:
        return self.text_of(self.SEND_CODE_BUTTON)

    def is_send_code_button_enabled(self) -> bool:
        return self.find(self.SEND_CODE_BUTTON).is_enabled()

    def session_token(self):
        return self.driver.execute_script("return sessionStorage.getItem('token')")

    def session_user_info(self):
        return self.driver.execute_script("return sessionStorage.getItem('userInfo')")

    def wait_for_url_contains(self, path: str):
        self.wait.until(EC.url_contains(path))
        return self
