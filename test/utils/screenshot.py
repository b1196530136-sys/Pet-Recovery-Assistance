from datetime import datetime
from pathlib import Path
import re

from selenium.webdriver.remote.webdriver import WebDriver


def save_screenshot(driver: WebDriver, target_dir: Path, test_name: str) -> Path:
    target_dir.mkdir(parents=True, exist_ok=True)
    safe_name = re.sub(r"[^a-zA-Z0-9_-]+", "_", test_name).strip("_")
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    path = target_dir / f"{safe_name}_{timestamp}.png"
    driver.save_screenshot(str(path))
    return path
