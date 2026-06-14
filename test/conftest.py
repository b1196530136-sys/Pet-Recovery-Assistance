from pathlib import Path

import pytest
from selenium.common.exceptions import WebDriverException

from config.settings import settings
from utils.browser_factory import BrowserFactory
from utils.screenshot import save_screenshot


@pytest.fixture(scope="session")
def base_url() -> str:
    return settings.base_url


@pytest.fixture(autouse=True)
def driver(request):
    """Open a fresh browser before each test case, take a screenshot after, then close."""
    browser = BrowserFactory.create()
    try:
        browser.implicitly_wait(settings.implicit_wait)
        browser.set_window_size(settings.window_width, settings.window_height)
        _open_base_url(browser)
        yield browser
    finally:
        try:
            save_screenshot(browser, Path(settings.screenshot_dir), request.node.name)
        finally:
            browser.quit()


def _open_base_url(browser) -> None:
    try:
        browser.get(settings.base_url)
    except WebDriverException as exc:
        if "ERR_CONNECTION_REFUSED" not in str(exc):
            raise
