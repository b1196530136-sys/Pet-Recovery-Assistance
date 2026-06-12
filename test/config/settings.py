import os
from dataclasses import dataclass
from pathlib import Path

from dotenv import load_dotenv


ROOT_DIR = Path(__file__).resolve().parents[1]
load_dotenv(ROOT_DIR / ".env")


def _bool_env(name: str, default: bool) -> bool:
    value = os.getenv(name)
    if value is None:
        return default
    return value.strip().lower() in {"1", "true", "yes", "y", "on"}


def _int_env(name: str, default: int) -> int:
    value = os.getenv(name)
    if value is None or not value.strip():
        return default
    return int(value)


@dataclass(frozen=True)
class Settings:
    base_url: str = os.getenv("BASE_URL", "http://localhost:3030")
    browser: str = os.getenv("BROWSER", "chrome").lower()
    chrome_driver_path: str = os.getenv(
        "CHROME_DRIVER_PATH",
        r"D:\chromedriver-win64\chromedriver-win64\chromedriver.exe",
    )
    headless: bool = _bool_env("HEADLESS", False)
    implicit_wait: int = _int_env("IMPLICIT_WAIT", 10)
    explicit_wait: int = _int_env("EXPLICIT_WAIT", 10)
    window_width: int = _int_env("WINDOW_WIDTH", 1440)
    window_height: int = _int_env("WINDOW_HEIGHT", 900)
    screenshot_on_failure: bool = _bool_env("SCREENSHOT_ON_FAILURE", True)
    screenshot_dir: str = os.getenv("SCREENSHOT_DIR", str(ROOT_DIR / "screenshots"))


settings = Settings()
