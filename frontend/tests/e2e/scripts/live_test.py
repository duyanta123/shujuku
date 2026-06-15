"""
学院专业管理模块 Playwright 浏览器测试 - 修正登录端点
"""
from playwright.sync_api import sync_playwright
import os, json

BASE_URL = "http://localhost:3000"
SCREENSHOT_DIR = os.path.join(os.path.dirname(__file__), "screenshots")
os.makedirs(SCREENSHOT_DIR, exist_ok=True)

def screenshot(page, name):
    path = os.path.join(SCREENSHOT_DIR, f"{name}.png")
    page.screenshot(path=path, full_page=True)
    return path

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    context = browser.new_context(viewport={"width": 1440, "height": 900})
    page = context.new_page()
    
    # 收集网络响应
    page.on("response", lambda resp: 
        print(f"  [{resp.status}] {resp.request.method} {resp.request.url[:100]}") 
        if "/api/" in resp.request.url else None
    )
    
    print("=" * 60)
    print("1. 管理员登录测试")
    print("=" * 60)
    page.goto(f"{BASE_URL}/login")
    page.wait_for_load_state("networkidle")
    page.wait_for_timeout(1000)
    
    # 选择管理员角色
    role_tabs = page.locator(".login-role-tabs, .role-tabs, [class*='role']").first
    if not role_tabs.is_visible():
        # Check for alternative role selection
        tabs = page.locator("text=管理员").all()
        if tabs:
            for t in tabs:
                if t.is_visible():
                    t.click()
                    break
        # Check for role cards
        cards = page.locator("text=管理员").all()
        if cards:
            for c in cards:
                if c.is_visible() and c.evaluate("el => el.offsetParent !== null"):
                    c.click()
                    page.wait_for_timeout(500)
                    break
    
    page.wait_for_timeout(500)
    screenshot(page, "01-login-before")
    
    # 填写表单
    inputs = page.locator(".el-input__inner, input").all()
    visible_inputs = [i for i in inputs if i.is_visible()]
    print(f"  可见输入框: {len(visible_inputs)}")
    
    if len(visible_inputs) >= 2:
        visible_inputs[0].click()
        visible_inputs[0].fill("admin")
        visible_inputs[1].click()
        visible_inputs[1].fill("123456")
    
    # 点击登录
    login_btn = page.locator("button").filter(has_text="登录").or_(page.locator("button").filter(has_text="登 录")).first
    if login_btn.is_visible():
        login_btn.click()
    
    page.wait_for_timeout(4000)
    page.wait_for_load_state("networkidle")
    screenshot(page, "02-after-login")
    print(f"  登录后 URL: {page.url}")
    
    # 检查登录结果
    error_msgs = page.locator(".el-message--error, .el-message").all()
    for msg in error_msgs:
        try:
            if msg.is_visible():
                print(f"  消息: {msg.inner_text()}")
        except:
            pass
    
    # 检查是否成功进入管理后台
    if "admin" in page.url:
        print("\n" + "=" * 60)
        print("2. 管理员后台 — 侧边栏菜单")
        print("=" * 60)
        page.wait_for_timeout(1000)
        
        # 侧边栏
        nav_items = page.locator("a.nav-item, .nav-item, nav a, aside a").all()
        print(f"  导航项: {len(nav_items)}")
        for item in nav_items:
            try:
                if item.is_visible():
                    text = item.inner_text().strip()
                    print(f"    - {text}")
            except:
                pass
        
        # 查找学院专业管理
        college_nav = page.locator("text=学院专业管理")
        if college_nav.count() > 0 and college_nav.first.is_visible():
            print("\n  ✓ 找到「学院专业管理」菜单!")
            college_nav.first.click()
            page.wait_for_load_state("networkidle")
            page.wait_for_timeout(2000)
            screenshot(page, "03-college-major-page")
            print(f"  当前 URL: {page.url}")
            
            # 检查页面
            tabs = page.locator(".el-tabs__item").all()
            print(f"  Tab 数量: {len(tabs)}")
            for tab in tabs:
                if tab.is_visible():
                    is_active = "is-active" in (tab.get_attribute("class") or "")
                    print(f"    - {tab.inner_text().strip()} {'[当前]' if is_active else ''}")
            
            # 表格
            rows = page.locator(".el-table__body tbody tr").all()
            print(f"  表格行数: {len(rows)}")
            
            # 测试切换到专业管理
            major_tab = page.locator(".el-tabs__item").filter(has_text="专业管理")
            if major_tab.count() > 0 and major_tab.first.is_visible():
                major_tab.first.click()
                page.wait_for_timeout(1500)
                screenshot(page, "04-major-tab")
                print(f"  切换到专业管理 Tab")
                rows = page.locator(".el-table__body tbody tr").all()
                print(f"  专业表格行数: {len(rows)}")
            
            # 测试新增学院弹窗
            print("\n" + "=" * 60)
            print("3. 新增学院弹窗测试")
            print("=" * 60)
            # 切换回学院 Tab
            college_tab = page.locator(".el-tabs__item").first
            college_tab.click()
            page.wait_for_timeout(1000)
            
            add_btn = page.locator("button").filter(has_text="添加").first
            if add_btn.is_visible():
                add_btn.click()
                page.wait_for_timeout(1500)
                page.wait_for_selector(".el-dialog", timeout=3000)
                screenshot(page, "05-add-college-dialog")
                
                dialog_title = page.locator(".el-dialog__title")
                print(f"  弹窗标题: {dialog_title.inner_text() if dialog_title.is_visible() else 'N/A'}")
                
                # 填写学院名
                inputs = page.locator(".el-dialog .el-input__inner").all()
                if inputs:
                    test_name = f"测试学院_{os.getpid()}"
                    inputs[0].fill(test_name)
                    print(f"  填写名称: {test_name}")
                    
                    # 提交
                    confirm_btn = page.locator(".el-dialog__footer .el-button--primary")
                    if confirm_btn.is_visible():
                        confirm_btn.click()
                        page.wait_for_timeout(2000)
                        page.wait_for_load_state("networkidle")
                        screenshot(page, "06-after-add-college")
                        
                        # 检查是否成功
                        print(f"  提交后 URL: {page.url}")
        else:
            print("\n  ✗ 未找到「学院专业管理」菜单")
    
    # 学生权限测试
    print("\n" + "=" * 60)
    print("4. 学生权限测试")
    print("=" * 60)
    page.goto(f"{BASE_URL}/login")
    page.wait_for_load_state("networkidle")
    page.wait_for_timeout(1000)
    
    # 清除登录状态
    page.evaluate("localStorage.clear()")
    page.wait_for_timeout(500)
    page.reload()
    page.wait_for_load_state("networkidle")
    
    inputs = page.locator(".el-input__inner, input").all()
    visible_inputs = [i for i in inputs if i.is_visible()]
    if len(visible_inputs) >= 2:
        visible_inputs[0].fill("S001")
        visible_inputs[1].fill("123456")
    
    page.locator("button").filter(has_text="登录").or_(page.locator("button").filter(has_text="登 录")).first.click()
    page.wait_for_timeout(3000)
    page.wait_for_load_state("networkidle")
    screenshot(page, "07-student-login")
    print(f"  学生登录后 URL: {page.url}")
    
    if "student" in page.url:
        nav_items = page.locator("a.nav-item, .nav-item, nav a, aside a").all()
        menu_texts = [item.inner_text().strip() for item in nav_items if item.is_visible()]
        print(f"  学生菜单: {menu_texts}")
        has_menu = any("学院专业管理" in t for t in menu_texts)
        print(f"  学院专业管理菜单: {'有' if has_menu else '无'}")
    
    # 访问管理页面测试
    page.goto(f"{BASE_URL}/admin/college-major")
    page.wait_for_load_state("networkidle")
    page.wait_for_timeout(2000)
    screenshot(page, "08-student-redirect")
    print(f"  访问管理页面后被重定向到: {page.url}")
    
    browser.close()
    print("\n" + "=" * 60)
    print("测试完成!")
    print("=" * 60)