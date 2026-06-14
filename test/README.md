# Selenium + pytest UI Test Framework

该目录是基于 Selenium、pytest 和 Page Object Model 的 UI 自动化测试框架。当前登录模块用例已迁移为 Selenium 写法，测试数据来自 `data/` 下的 CSV 文件。

## 目录结构

```text
test/
  config/          测试配置读取与默认值
  data/            测试数据目录
  pages/           Page Object 页面对象
  reports/         pytest-html 报告输出目录
  screenshots/     失败截图输出目录
  tests/           测试用例目录
  utils/           浏览器、日志、等待等通用工具
  conftest.py      pytest fixtures
  pytest.ini       pytest 配置
  requirements.txt Python 测试依赖
```

## 快速开始

```powershell
cd test
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r requirements.txt
Copy-Item .env.example .env
```

先启动后端服务和前端 Vite 服务。前端默认端口为 `3000`，对应测试配置中的 `BASE_URL=http://localhost:3000`。

再执行：

```powershell
pytest
```

Selenium 4 会优先使用 Selenium Manager 自动管理浏览器驱动。运行前请确保本机已安装对应浏览器。
如果 `.env` 中的 `CHROME_DRIVER_PATH` 指向一个真实文件，则 Chrome 会优先使用该驱动；否则自动回退到 Selenium Manager。

## 常用配置

配置可通过 `.env` 环境变量覆盖，常用项如下：

```text
BASE_URL=http://localhost:3000
BROWSER=chrome
CHROME_DRIVER_PATH=D:\chromedriver-win64\chromedriver-win64\chromedriver.exe
HEADLESS=false
IMPLICIT_WAIT=0
EXPLICIT_WAIT=10
SCREENSHOT_ON_FAILURE=true
```

支持的浏览器值：`chrome`、`edge`、`firefox`。

## 后续编写用例约定

页面行为封装在 `pages/` 下，测试用例只通过页面对象完成操作和断言。新增用例时放在 `tests/` 目录下，文件名使用 `test_*.py`。

## Playwright 到 Selenium 的对应关系

迁移后的测试代码不再依赖 Playwright fixture 或 locator API：

- 浏览器启动统一由 `utils/browser_factory.py` 中的 Selenium WebDriver 创建。
- 每个测试通过 `conftest.py` 的 `driver` fixture 获得独立浏览器实例。
- 页面交互封装在 `pages/` 的 Page Object 中，使用 Selenium `By` locator、`WebDriverWait` 和 `expected_conditions`。
- 截图使用 Selenium `driver.save_screenshot()`，失败时按日期保存到 `screenshots/`。
