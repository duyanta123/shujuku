"""
DOM 结构侦查 — 查看学院专业管理页面实际结构
"""
from playwright.sync_api import sync_playwright
import os

BASE_URL = "http://localhost:3000"
SCREENSHOT_DIR = os.path.join(os.path.dirname(__file__), "screenshots2")
os.makedirs(SCREENSHOT_DIR, exist_ok=True)

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    context = browser.new_context(viewport={"width": 1440, "height": 900})
    page = context.new_page()
    
    # 登录
    page.goto(f"{BASE_URL}/login")
    page.wait_for_load_state("networkidle")
    page.wait_for_timeout(1000)
    
    page.locator(".role-tab").filter(has_text="管理员").first.click()
    page.wait_for_timeout(300)
    inputs = page.locator(".el-input__inner").all()
    visible = [i for i in inputs if i.is_visible()]
    visible[0].fill("admin")
    visible[1].fill("123456")
    page.locator(".login-btn").first.click()
    page.wait_for_timeout(5000)
    page.wait_for_load_state("networkidle")
    
    # 导航到学院专业管理
    page.locator("text=学院专业管理").first.click()
    page.wait_for_load_state("networkidle")
    page.wait_for_timeout(3000)
    
    page.screenshot(path=os.path.join(SCREENSHOT_DIR, "dom-college-page.png"), full_page=True)
    
    # 打印 DOM 结构
    print("=" * 60)
    print("页面 DOM 结构分析")
    print("=" * 60)
    
    # Tabs
    tabs = page.locator(".el-tabs__item").all()
    print(f"\nTabs ({len(tabs)}):")
    for t in tabs:
        if t.is_visible():
            active = "is-active" in (t.get_attribute("class") or "")
            print(f"  {t.inner_text().strip()} {'[active]' if active else ''}")
    
    # 搜索区域
    print("\n搜索区域:")
    search_inputs = page.locator("input[placeholder], .el-input__inner").all()
    for inp in search_inputs:
        if inp.is_visible():
            placeholder = inp.get_attribute("placeholder") or ""
            print(f"  placeholder='{placeholder}' class='{inp.get_attribute('class')}'")
    
    # 按钮
    print("\n按钮:")
    buttons = page.locator("button").all()
    for btn in buttons:
        if btn.is_visible():
            cls = btn.get_attribute("class") or ""
            print(f"  '{btn.inner_text().strip()[:30]}' class='{cls[:80]}'")
    
    # 表格
    print("\n表格:")
    table = page.locator(".el-table").first
    if table.is_visible():
        headers = table.locator("th").all()
        print(f"  表头 ({len(headers)}):")
        for h in headers:
            if h.is_visible():
                print(f"    {h.inner_text().strip()}")
        
        rows = table.locator("tbody tr").all()
        visible_rows = [r for r in rows if r.is_visible()]
        print(f"  数据行: {len(visible_rows)}")
        for i, row in enumerate(visible_rows[:3]):
            cells = row.locator("td").all()
            texts = [c.inner_text().strip() for c in cells if c.is_visible()]
            print(f"    Row {i}: {texts}")
    
    # 点击添加按钮，查看弹窗结构
    add_btn = page.locator("button").filter(has_text="添加").first
    if add_btn.is_visible():
        add_btn.click()
        page.wait_for_timeout(2000)
        page.wait_for_selector(".el-dialog", timeout=3000)
        page.screenshot(path=os.path.join(SCREENSHOT_DIR, "dom-add-dialog.png"), full_page=True)
        
        print("\n弹窗结构:")
        dialog = page.locator(".el-dialog").last
        print(f"  标题: {dialog.locator('.el-dialog__title').inner_text()}")
        
        # 弹窗中的输入框
        dialog_inputs = dialog.locator(".el-input__inner").all()
        print(f"  输入框 ({len(dialog_inputs)}):")
        for i, inp in enumerate(dialog_inputs):
            if inp.is_visible():
                placeholder = inp.get_attribute("placeholder") or ""
                print(f"    [{i}] placeholder='{placeholder}'")
        
        # 弹窗中的 select
        dialog_selects = dialog.locator(".el-select").all()
        print(f"  Selects ({len(dialog_selects)}):")
        for i, sel in enumerate(dialog_selects):
            if sel.is_visible():
                print(f"    [{i}] class='{sel.get_attribute('class')}'")
        
        # 弹窗中的按钮
        dialog_btns = dialog.locator("button").all()
        print(f"  按钮 ({len(dialog_btns)}):")
        for btn in dialog_btns:
            if btn.is_visible():
                print(f"    '{btn.inner_text().strip()}'")
        
        # 关闭弹窗
        cancel_btn = dialog.locator("button").filter(has_text="取消").first
        if cancel_btn.is_visible():
            cancel_btn.click()
    
    # 切换到专业管理 Tab
    major_tab = page.locator(".el-tabs__item").filter(has_text="专业管理")
    if major_tab.first.is_visible():
        major_tab.first.click()
        page.wait_for_timeout(2000)
        page.wait_for_load_state("networkidle")
        page.screenshot(path=os.path.join(SCREENSHOT_DIR, "dom-major-tab.png"), full_page=True)
        
        print("\n专业管理 Tab:")
        # 表格
        rows = page.locator(".el-table__body tbody tr").all()
        visible_rows = [r for r in rows if r.is_visible()]
        print(f"  数据行: {len(visible_rows)}")
        for i, row in enumerate(visible_rows[:3]):
            cells = row.locator("td").all()
            texts = [c.inner_text().strip() for c in cells if c.is_visible()]
            print(f"    Row {i}: {texts}")
        
        # 搜索
        search_inputs = page.locator("input[placeholder]").all()
        print(f"  搜索输入框:")
        for inp in search_inputs:
            if inp.is_visible():
                placeholder = inp.get_attribute("placeholder") or ""
                print(f"    placeholder='{placeholder}'")
        
        # 打开专业添加弹窗
        add_btn = page.locator("button").filter(has_text="添加").first
        if add_btn.is_visible():
            add_btn.click()
            page.wait_for_timeout(2000)
            page.wait_for_selector(".el-dialog", timeout=3000)
            page.screenshot(path=os.path.join(SCREENSHOT_DIR, "dom-major-add-dialog.png"), full_page=True)
            
            print("\n专业添加弹窗:")
            dialog = page.locator(".el-dialog").last
            print(f"  标题: {dialog.locator('.el-dialog__title').inner_text()}")
            
            dialog_inputs = dialog.locator(".el-input__inner").all()
            print(f"  输入框:")
            for i, inp in enumerate(dialog_inputs):
                if inp.is_visible():
                    placeholder = inp.get_attribute("placeholder") or ""
                    print(f"    [{i}] placeholder='{placeholder}'")
            
            dialog_selects = dialog.locator(".el-select").all()
            print(f"  Selects:")
            for i, sel in enumerate(dialog_selects):
                if sel.is_visible():
                    print(f"    [{i}] class='{sel.get_attribute('class')}'")
    
    browser.close()
    print("\nDOM 侦查完成")