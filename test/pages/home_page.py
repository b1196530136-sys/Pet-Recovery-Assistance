from selenium.webdriver.common.by import By

from pages.base_page import BasePage


class HomePage(BasePage):
    path = "/"

    HERO_TITLE = (By.CSS_SELECTOR, ".hero h1")

    def hero_title(self) -> str:
        return self.text_of(self.HERO_TITLE)
