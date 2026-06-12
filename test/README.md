# Selenium + pytest UI Test Framework

该目录是基于 Selenium、pytest 和 Page Object Model 的 UI 自动化测试框架骨架，当前只包含框架代码，不包含实际测试用例。

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

当前没有编写实际测试用例。后续在 `tests/` 下新增 `test_*.py` 用例文件后，再执行：

```powershell
pytest
```

Selenium 4 会优先使用 Selenium Manager 自动管理浏览器驱动。运行前请确保本机已安装对应浏览器。
当前 Chrome 默认使用本机驱动路径：`D:\chromedriver-win64\chromedriver-win64\chromedriver.exe`。

## 常用配置

配置可通过 `.env` 环境变量覆盖，常用项如下：

```text
BASE_URL=http://localhost:5173
BROWSER=chrome
CHROME_DRIVER_PATH=D:\chromedriver-win64\chromedriver-win64\chromedriver.exe
HEADLESS=false
EXPLICIT_WAIT=10
SCREENSHOT_ON_FAILURE=true
```

支持的浏览器值：`chrome`、`edge`、`firefox`。

## 后续编写用例约定

页面行为封装在 `pages/` 下，测试用例只通过页面对象完成操作和断言。新增用例时放在 `tests/` 目录下，文件名使用 `test_*.py`。
