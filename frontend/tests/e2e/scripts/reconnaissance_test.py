"""
学院专业管理模块 Playwright 浏览器侦查测试
使用 webapp-testing skill 的 Playwright 同步 API
"""
from playwright.sync_api import sync_playwright
import os

BASE_URL = "http://localhost:3000"
SCREENSHOT_DIR = os.path.join(os.path.dirname(__file__), "screenshots")
os.makedirs(SCREENSHOT_DIR, exist_ok=True)

def screenshot(page, name):
    path = os.path.join(SCREENSHOT_DIR, f"{name}.png")
    page.screenshot(path=path, full_page=True)
    print(f"  [截图] {path}")
    return path

def test_login_and_nav():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        context = browser.new_context(viewport={"width": 1440, "height": 900})
        page = context.new_page()
        
        # 收集控制台日志
        console_logs = []
        page.on("console", lambda msg: console_logs.append(f"[{msg.type}] {msg.text}"))
        
        print("=" * 60)
        print("1. 登录页面加载测试")
        print("=" * 60)
        page.goto(f"{BASE_URL}/login")
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(1000)
        screenshot(page, "01-login-page")
        print(f"  URL: {page.url}")
        print(f"  页面标题: {page.title()}")
        
        # 填写登录表单
        inputs = page.locator(".el-input__inner").all()
        print(f"  找到 {len(inputs)} 个输入框")
        if len(inputs) >= 2:
            inputs[0].fill("admin")
            inputs[1].fill("123456")
        
        # 点击登录按钮
        login_btn = page.locator(".el-button--primary").first
        print(f"  登录按钮可见: {login_btn.is_visible()}")
        login_btn.click()
        
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(2000)
        screenshot(page, "02-after-login")
        print(f"  登录后 URL: {page.url}")
        
        # 检查侧边栏
        print("\n" + "=" * 60)
        print("2. 侧边栏菜单检查")
        print("=" * 60)
        nav_items = page.locator(".nav-item").all()
        print(f"  找到 {len(nav_items)} 个导航项:")
        for item in nav_items:
            text = item.inner_text().strip()
            print(f"    - {text}")
        
        # 查找学院专业管理菜单
        college_major_btn = page.locator(".nav-item").filter(has_text="学院专业管理")
        has_menu = college_major_btn.count() > 0
        print(f"\n  '学院专业管理' 菜单可见: {has_menu}")
        
        if has_menu:
            # 点击进入学院专业管理页面
            college_major_btn.first.click()
            page.wait_for_load_state("networkidle")
            page.wait_for_timeout(2000)
            screenshot(page, "03-college-major-page")
            print(f"  当前 URL: {page.url}")
            
            # 检查页面内容
            print("\n" + "=" * 60)
            print("3. 学院专业管理页面内容")
            print("=" * 60)
            
            # 检查 Tabs
            tabs = page.locator(".el-tabs__item").all()
            print(f"  找到 {len(tabs)} 个 Tab:")
            for tab in tabs:
                text = tab.inner_text().strip()
                is_active = "is-active" in (tab.get_attribute("class") or "")
                print(f"    - {text} {'[当前]' if is_active else ''}")
            
            # 检查表格
            table_headers = page.locator(".el-table th").all()
            print(f"\n  表格列数: {len(table_headers)}")
            for th in table_headers:
                print(f"    - {th.inner_text().strip()}")
            
            # 检查表格数据行数
            rows = page.locator(".el-table__body tbody tr").all()
            print(f"  表格行数: {len(rows)}")
            if rows:
                first_row = rows[0].locator("td").all()
                row_data = [td.inner_text().strip() for td in first_row]
                print(f"  第一行数据: {row_data}")
            
            # 检查按钮
            buttons = page.locator("button").all()
            print(f"\n  页面按钮 ({len(buttons)}):")
            for btn in buttons:
                text = btn.inner_text().strip()
                if text:
                    print(f"    - {text}")
            
            # 切换到专业管理 Tab
            major_tab = page.locator(".el-tabs__item").filter(has_text="专业管理")
            if major_tab.count() > 0:
                print("\n" + "=" * 60)
                print("4. 切换到专业管理 Tab")
                print("=" * 60)
                major_tab.first.click()
                page.wait_for_timeout(1500)
                page.wait_for_load_state("networkidle")
                screenshot(page, "04-major-tab")
                
                rows = page.locator(".el-table__body tbody tr").all()
                print(f"  专业表格行数: {len(rows)}")
                if rows:
                    first_row = rows[0].locator("td").all()
                    row_data = [td.inner_text().strip() for td in first_row]
                    print(f"  第一行数据: {row_data}")
            
            # 测试新增学院弹窗
            print("\n" + "=" * 60)
            print("5. 测试新增学院弹窗")
            print("=" * 60)
            college_tab = page.locator(".el-tabs__item").first
            college_tab.click()
            page.wait_for_timeout(1000)
            
            add_btn = page.locator("button").filter(has_text="添加").first
            if add_btn.is_visible():
                add_btn.click()
                page.wait_for_timeout(1000)
                page.wait_for_selector(".el-dialog", timeout=5000)
                screenshot(page, "05-college-add-dialog")
                
                # 检查弹窗内容
                dialog = page.locator(".el-dialog")
                print(f"  弹窗可见: {dialog.is_visible()}")
                print(f"  弹窗标题: {page.locator('.el-dialog__title').inner_text()}")
                
                # 关闭弹窗
                cancel_btn = page.locator(".el-dialog__footer button").first
                if cancel_btn.is_visible():
                    cancel_btn.click()
                    page.wait_for_timeout(500)
        
        # 学生登录测试
        print("\n" + "=" * 60)
        print("6. 学生权限测试")
        print("=" * 60)
        page.goto(f"{BASE_URL}/login")
        page.wait_for_load_state("networkidle")
        
        # 清除 storage 重新登录
        page.evaluate("localStorage.clear()")
        inputs = page.locator(".el-input__inner").all()
        inputs[0].fill("S001")
        inputs[1].fill("123456")
        page.locator(".el-button--primary").first.click()
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(2000)
        screenshot(page, "06-student-login")
        
        nav_items = page.locator(".nav-item").all()
        menu_texts = [item.inner_text().strip() for item in nav_items]
        print(f"  学生导航菜单: {menu_texts}")
        has_admin_menu = any("学院专业管理" in t for t in menu_texts)
        print(f"  '学院专业管理' 在学生菜单中: {has_admin_menu}")
        
        # 尝试直接访问管理页面
        page.goto(f"{BASE_URL}/admin/college-major")
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(2000)
        print(f"  访问后被重定向到: {page.url}")
        
        # 控制台错误检查
        errors = [log for log in console_logs if "error" in log.lower()]
        if errors:
            print(f"\n⚠ 控制台错误 ({len(errors)}):")
            for err in errors[:5]:
                print(f"  {err}")
        else:
            print("\n✓ 无控制台错误")
        
        browser.close()
        print("\n" + "=" * 60)
        print("侦查测试完成！")
        print("=" * 60)

if __name__ == "__main__":
    test_login_and_nav()