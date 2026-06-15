"""
学院专业管理模块 Playwright 浏览器测试 v2 - 修正角色选择
"""
from playwright.sync_api import sync_playwright
import os, sys

BASE_URL = "http://localhost:3000"
SCREENSHOT_DIR = os.path.join(os.path.dirname(__file__), "screenshots2")
os.makedirs(SCREENSHOT_DIR, exist_ok=True)

def screenshot(page, name):
    try:
        path = os.path.join(SCREENSHOT_DIR, f"{name}.png")
        page.screenshot(path=path, full_page=True)
        print(f"  [截图] {name}.png")
        return path
    except Exception as e:
        print(f"  [截图失败] {name}: {e}")
        return None

def run_tests():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        context = browser.new_context(viewport={"width": 1440, "height": 900})
        page = context.new_page()
        
        # 收集网络请求
        api_calls = []
        page.on("response", lambda resp: 
            api_calls.append(f"[{resp.status}] {resp.request.method} {resp.request.url}") 
            if "/api/" in resp.request.url else None
        )
        
        # ============================================================
        print("=" * 60)
        print("1. 管理员登录测试")
        print("=" * 60)
        
        page.goto(f"{BASE_URL}/login")
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(1500)
        
        screenshot(page, "01-login-page")
        
        # 选择管理员角色 - 点击 role-tab 按钮
        role_tabs = page.locator(".role-tab").all()
        print(f"  角色标签数: {len(role_tabs)}")
        for i, tab in enumerate(role_tabs):
            try:
                text = tab.inner_text().strip()
                print(f"    [{i}] {text}")
            except:
                pass
        
        # 点击 "管理员" 按钮
        admin_tab = page.locator(".role-tab").filter(has_text="管理员")
        if admin_tab.count() > 0 and admin_tab.first.is_visible():
            admin_tab.first.click()
            page.wait_for_timeout(500)
            print("  ✓ 已选择管理员角色")
            # 确认 active 状态
            is_active = "active" in (admin_tab.first.get_attribute("class") or "")
            print(f"    管理员 tab active: {is_active}")
        else:
            print("  ✗ 未找到管理员角色标签")
            # 尝试备用方式
            for tab in role_tabs:
                if "管理员" in tab.inner_text():
                    tab.click()
                    print("  ✓ 通过备用方式选择了管理员")
                    break
        
        # 填写表单
        inputs = page.locator(".el-input__inner").all()
        visible_inputs = [i for i in inputs if i.is_visible()]
        print(f"  可见输入框: {len(visible_inputs)}")
        
        if len(visible_inputs) >= 2:
            visible_inputs[0].click()
            page.wait_for_timeout(200)
            visible_inputs[0].fill("admin")
            page.wait_for_timeout(200)
            visible_inputs[1].click()
            page.wait_for_timeout(200)
            visible_inputs[1].fill("123456")
            print("  ✓ 已填写账号密码")
        
        screenshot(page, "02-before-login")
        
        # 点击登录按钮
        login_btn = page.locator(".login-btn").first
        if login_btn.is_visible():
            login_btn.click()
            print("  ✓ 已点击登录按钮")
        
        page.wait_for_timeout(5000)
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(1000)
        screenshot(page, "03-after-login")
        print(f"  登录后 URL: {page.url}")
        
        # 检查 API 调用
        login_apis = [c for c in api_calls if "/login" in c]
        for call in login_apis:
            print(f"  API: {call}")
        
        # 检查错误消息
        error_msgs = page.locator(".el-message--error").all()
        for msg in error_msgs:
            try:
                if msg.is_visible():
                    print(f"  ✗ 错误消息: {msg.inner_text()}")
            except:
                pass
        
        # 检查是否成功进入管理后台
        if "admin" in page.url or "dashboard" in page.url.lower() or "home" in page.url.lower():
            success = True
            print("\n✓ 管理员登录成功!")
        else:
            success = False
            print(f"\n✗ 登录可能失败，URL: {page.url}")
            # 检查页面内容
            try:
                body_text = page.locator("body").inner_text()[:500]
                print(f"  页面内容片段: {body_text}")
            except:
                pass
        
        # ============================================================
        if success:
            print("\n" + "=" * 60)
            print("2. 管理员后台 — 侧边栏菜单")
            print("=" * 60)
            page.wait_for_timeout(2000)
            
            # 侧边栏
            sidebar = page.locator(".el-menu, aside, nav, .sidebar").first
            if sidebar.is_visible():
                nav_items = sidebar.locator(".el-menu-item, .nav-item, a").all()
                print(f"  导航项: {len(nav_items)}")
                for item in nav_items:
                    try:
                        if item.is_visible():
                            text = item.inner_text().strip()
                            print(f"    - {text}")
                    except:
                        pass
            
            screenshot(page, "04-admin-dashboard")
            
            # 查找「学院专业管理」菜单
            college_nav = page.locator("text=学院专业管理").first
            if college_nav.is_visible():
                print("\n  ✓ 找到「学院专业管理」菜单!")
                college_nav.click()
                page.wait_for_load_state("networkidle")
                page.wait_for_timeout(3000)
                screenshot(page, "05-college-major-page")
                print(f"  当前 URL: {page.url}")
                
                # 检查页面 Tab
                tabs = page.locator(".el-tabs__item").all()
                print(f"  Tab 数量: {len(tabs)}")
                for tab in tabs:
                    if tab.is_visible():
                        is_active = "is-active" in (tab.get_attribute("class") or "")
                        print(f"    - {tab.inner_text().strip()} {'[当前]' if is_active else ''}")
                
                # 表格行数
                rows = page.locator(".el-table__body tbody tr").all()
                visible_rows = [r for r in rows if r.is_visible()]
                print(f"  学院表格行数: {len(visible_rows)}")
                
                # 切换到专业管理 Tab
                major_tab = page.locator(".el-tabs__item").filter(has_text="专业管理")
                if major_tab.count() > 0 and major_tab.first.is_visible():
                    major_tab.first.click()
                    page.wait_for_timeout(2000)
                    page.wait_for_load_state("networkidle")
                    screenshot(page, "06-major-tab")
                    print("  ✓ 切换到「专业管理」Tab")
                    rows = page.locator(".el-table__body tbody tr").all()
                    visible_rows = [r for r in rows if r.is_visible()]
                    print(f"  专业表格行数: {len(visible_rows)}")
                
                # ============================================================
                print("\n" + "=" * 60)
                print("3. 新增学院弹窗测试")
                print("=" * 60)
                
                # 切换回学院 Tab
                college_tab = page.locator(".el-tabs__item").first
                if college_tab.is_visible():
                    college_tab.click()
                    page.wait_for_timeout(1000)
                    print("  ✓ 切换回学院管理 Tab")
                
                # 点击添加按钮
                add_btn = page.locator("button").filter(has_text="添加").first
                if add_btn.is_visible():
                    add_btn.click()
                    page.wait_for_timeout(2000)
                    try:
                        page.wait_for_selector(".el-dialog", timeout=3000)
                        screenshot(page, "07-add-college-dialog")
                        
                        dialog_title = page.locator(".el-dialog__title")
                        title_text = dialog_title.inner_text() if dialog_title.is_visible() else "N/A"
                        print(f"  ✓ 弹窗标题: {title_text}")
                        
                        # 填写学院名
                        dialog_inputs = page.locator(".el-dialog .el-input__inner").all()
                        if dialog_inputs:
                            test_name = f"测试学院_Playwright"
                            dialog_inputs[0].click()
                            dialog_inputs[0].fill(test_name)
                            print(f"  ✓ 填写名称: {test_name}")
                            
                            # 提交
                            confirm_btn = page.locator(".el-dialog__footer .el-button--primary").last
                            if confirm_btn.is_visible():
                                confirm_btn.click()
                                page.wait_for_timeout(3000)
                                page.wait_for_load_state("networkidle")
                                screenshot(page, "08-after-add-college")
                                print(f"  提交后 URL: {page.url}")
                                
                                # 检查结果
                                success_msgs = page.locator(".el-message--success").all()
                                for msg in success_msgs:
                                    try:
                                        if msg.is_visible():
                                            print(f"  ✓ 成功消息: {msg.inner_text()}")
                                    except:
                                        pass
                                
                                error_msgs = page.locator(".el-message--error").all()
                                for msg in error_msgs:
                                    try:
                                        if msg.is_visible():
                                            print(f"  ✗ 错误消息: {msg.inner_text()}")
                                    except:
                                        pass
                    except Exception as e:
                        print(f"  ✗ 弹窗未出现: {e}")
                        screenshot(page, "07-no-dialog")
                else:
                    print("  ✗ 未找到添加按钮")
                    # 列出所有按钮
                    all_btns = page.locator("button").all()
                    print(f"  页面上有 {len(all_btns)} 个按钮:")
                    for b in all_btns[:10]:
                        try:
                            if b.is_visible():
                                print(f"    - {b.inner_text().strip()[:50]}")
                        except:
                            pass
            else:
                print("\n  ✗ 未找到「学院专业管理」菜单")
                # 直接导航
                print("  尝试直接导航到 /admin/college-major")
                page.goto(f"{BASE_URL}/admin/college-major")
                page.wait_for_load_state("networkidle")
                page.wait_for_timeout(3000)
                screenshot(page, "05-college-major-direct")
                print(f"  直接导航后 URL: {page.url}")
        
        # ============================================================
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
        page.wait_for_timeout(1000)
        
        # 选择学生角色
        student_tab = page.locator(".role-tab").filter(has_text="学生")
        if student_tab.count() > 0 and student_tab.first.is_visible():
            student_tab.first.click()
            page.wait_for_timeout(300)
        
        inputs = page.locator(".el-input__inner").all()
        visible_inputs = [i for i in inputs if i.is_visible()]
        if len(visible_inputs) >= 2:
            visible_inputs[0].fill("S001")
            visible_inputs[1].fill("123456")
        
        page.locator(".login-btn").first.click()
        page.wait_for_timeout(4000)
        page.wait_for_load_state("networkidle")
        screenshot(page, "09-student-login")
        print(f"  学生登录后 URL: {page.url}")
        
        if "student" in page.url:
            nav_items = page.locator(".el-menu-item, .nav-item, nav a, aside a").all()
            menu_texts = []
            for item in nav_items:
                try:
                    if item.is_visible():
                        menu_texts.append(item.inner_text().strip())
                except:
                    pass
            print(f"  学生菜单: {menu_texts}")
            has_college_menu = any("学院专业管理" in t for t in menu_texts)
            print(f"  学院专业管理菜单: {'有(权限漏洞!)' if has_college_menu else '无(正确)'}")
        
        # 访问管理页面测试
        page.goto(f"{BASE_URL}/admin/college-major")
        page.wait_for_load_state("networkidle")
        page.wait_for_timeout(3000)
        screenshot(page, "10-student-redirect")
        print(f"  访问管理页面后被重定向到: {page.url}")
        if "admin" not in page.url:
            print("  ✓ 权限拦截正确")
        else:
            print("  ✗ 权限拦截失败!")
        
        # ============================================================
        print("\n" + "=" * 60)
        print("5. 所有 API 请求汇总")
        print("=" * 60)
        for call in api_calls:
            print(f"  {call}")
        
        browser.close()
        print("\n" + "=" * 60)
        print("测试完成!")
        print("=" * 60)

if __name__ == "__main__":
    run_tests()