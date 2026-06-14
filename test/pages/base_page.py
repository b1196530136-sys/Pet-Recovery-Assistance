from selenium.webdriver import Keys
from selenium.webdriver.remote.webdriver import WebDriver
from selenium.webdriver.remote.webelement import WebElement
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.support.ui import WebDriverWait

from config.settings import settings


class BasePage:
    path = ""

    def __init__(self, driver: WebDriver, base_url: str):
        self.driver = driver
        self.base_url = base_url.rstrip("/")
        self.wait = WebDriverWait(driver, settings.explicit_wait)

    @property
    def url(self) -> str:
        if not self.path:
            return self.base_url
        return f"{self.base_url}/{self.path.lstrip('/')}"

    def open(self):
        self.driver.get(self.url)
        return self

    def find(self, locator: tuple[str, str]) -> WebElement:
        return self.wait.until(EC.presence_of_element_located(locator))

    def find_all(self, locator: tuple[str, str]) -> list[WebElement]:
        return self.wait.until(EC.presence_of_all_elements_located(locator))

    def visible(self, locator: tuple[str, str]) -> WebElement:
        return self.wait.until(EC.visibility_of_element_located(locator))

    def clickable(self, locator: tuple[str, str]) -> WebElement:
        return self.wait.until(EC.element_to_be_clickable(locator))

    def click(self, locator: tuple[str, str]):
        self.clickable(locator).click()
        return self

    def type_text(self, locator: tuple[str, str], text: str, clear: bool = True):
        element = self.visible(locator)
        if clear:
            element.send_keys(Keys.CONTROL, "a")
            element.send_keys(Keys.BACKSPACE)
        element.send_keys(text)
        return self


    def text_of(self, locator: tuple[str, str]) -> str:
        return self.visible(locator).text

    def current_url(self) -> str:
        return self.driver.current_url
