from selenium.webdriver.common.by import By

from pages.base_page import BasePage


class LoginPage(BasePage):
    path = "/auth/login"

    EMAIL_INPUT = (By.CSS_SELECTOR, "input[placeholder='邮箱']")
    PASSWORD_INPUT = (By.CSS_SELECTOR, "input[placeholder='密码']")
    LOGIN_BUTTON = (By.XPATH, "//button[.//span[normalize-space()='登录']]")

    def login_with_password(self, email: str, password: str):
        self.type_text(self.EMAIL_INPUT, email)
        self.type_text(self.PASSWORD_INPUT, password)
        self.click(self.LOGIN_BUTTON)
        return self
