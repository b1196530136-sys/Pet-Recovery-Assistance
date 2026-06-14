from pathlib import Path

from selenium import webdriver
from selenium.webdriver.chrome.options import Options as ChromeOptions
from selenium.webdriver.chrome.service import Service as ChromeService
from selenium.webdriver.edge.options import Options as EdgeOptions
from selenium.webdriver.firefox.options import Options as FirefoxOptions

from config.settings import settings


class BrowserFactory:
    @staticmethod
    def create():
        browser = settings.browser
        if browser == "chrome":
            if settings.chrome_driver_path and Path(settings.chrome_driver_path).is_file():
                service = ChromeService(executable_path=settings.chrome_driver_path)
                return webdriver.Chrome(service=service, options=_chrome_options())
            return webdriver.Chrome(options=_chrome_options())
        if browser == "edge":
            return webdriver.Edge(options=_edge_options())
        if browser == "firefox":
            return webdriver.Firefox(options=_firefox_options())
        raise ValueError(f"Unsupported browser: {browser}")


def _chrome_options() -> ChromeOptions:
    options = ChromeOptions()
    if settings.headless:
        options.add_argument("--headless=new")
    options.add_argument("--disable-gpu")
    options.add_argument("--no-sandbox")
    return options


def _edge_options() -> EdgeOptions:
    options = EdgeOptions()
    if settings.headless:
        options.add_argument("--headless=new")
    options.add_argument("--disable-gpu")
    return options


def _firefox_options() -> FirefoxOptions:
    options = FirefoxOptions()
    if settings.headless:
        options.add_argument("--headless")
    return options
