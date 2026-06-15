"""
侦察 dialog 交互 — 点击保存后实际发生了什么
"""
from playwright.sync_api import sync_playwright
import os, time

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
    page.wait_for_timeout(2000)
    print(f"当前 URL: {page.url}")
    
    # ============================================
    print("=" * 60)
    print("测试: 学院添加并保存")
    print("=" * 60)
    
    unique_name = f"侦察测试_{int(time.time())}"
    page.locator("button").filter(has_text="添加学院").first.click()
    page.wait_for_timeout(2000)
    
    # 检查 dialog
    dialogs = page.locator(".el-dialog").all()
    print(f"Dialog 数量: {len(dialogs)}")
    for i, d in enumerate(dialogs):
        visible = d.is_visible()
        display = d.evaluate("el => window.getComputedStyle(el).display")
        print(f"  [{i}] visible={visible} display={display}")
    
    dialog = page.locator(".el-dialog").last
    print(f"Dialog visible: {dialog.is_visible()}")
    
    # 填写名称
    dialog.locator(".el-input__inner").first.fill(unique_name)
    print(f"✓ 填写名称: {unique_name}")
    
    page.screenshot(path=os.path.join(SCREENSHOT_DIR, "recon-before-save.png"), full_page=True)
    
    # 点击保存
    save_btn = dialog.locator(".el-dialog__footer .el-button--primary").last
    print(f"保存按钮: visible={save_btn.is_visible()} text={save_btn.inner_text()}")
    save_btn.click()
    print("✓ 点击保存")
    
    # 等待并检查
    page.wait_for_timeout(2000)
    
    # 检查 dialog 是否关闭
    dialogs = page.locator(".el-dialog").all()
    print(f"\n保存后 Dialog 数量: {len(dialogs)}")
    for i, d in enumerate(dialogs):
        visible = d.is_visible()
        display = d.evaluate("el => window.getComputedStyle(el).display")
        print(f"  [{i}] visible={visible} display={display}")
    
    # 检查消息
    msg = page.locator(".el-message--success, .el-message--error").first
    try:
        if msg.is_visible():
            print(f"消息: {msg.inner_text()}")
    except:
        print("无消息")
    
    page.screenshot(path=os.path.join(SCREENSHOT_DIR, "recon-after-save.png"), full_page=True)
    
    # 检查表格
    table = page.locator(".el-table__body").first
    if table.is_visible():
        text = table.inner_text()
        if unique_name in text:
            print(f"✓ 表格中包含 {unique_name}")
        else:
            print(f"✗ 表格中不包含 {unique_name}")
            print(f"  表格内容: {text[:200]}")
    
    # ============================================
    # 切换到专业管理Tab
    print("\n" + "=" * 60)
    print("测试: 专业添加 — 调试 select 下拉")
    print("=" * 60)
    
    page.locator(".el-tabs__item").filter(has_text="专业管理").first.click()
    page.wait_for_timeout(2000)
    page.wait_for_load_state("networkidle")
    
    page.locator("button").filter(has_text="添加专业").first.click()
    page.wait_for_timeout(2000)
    
    dialog = page.locator(".el-dialog").last
    print(f"Dialog visible: {dialog.is_visible()}")
    
    # 尝试点击 select
    select_wrapper = dialog.locator(".el-select__wrapper").first
    print(f"select_wrapper: visible={select_wrapper.is_visible()}")
    
    select_wrapper.click()
    page.wait_for_timeout(1000)
    
    # 检查下拉
    dropdowns = page.locator(".el-select-dropdown").all()
    print(f"el-select-dropdown 数量: {len(dropdowns)}")
    for i, dd in enumerate(dropdowns):
        visible = dd.is_visible()
        display = dd.evaluate("el => window.getComputedStyle(el).display")
        items = dd.locator(".el-select-dropdown__item").all()
        print(f"  [{i}] visible={visible} display={display} items={len(items)}")
    
    # 尝试点击 el-select 本身
    page.keyboard.press("Escape")
    page.wait_for_timeout(500)
    
    el_select = dialog.locator(".el-select").first
    print(f"\nel-select 直接点击:")
    el_select.click()
    page.wait_for_timeout(1000)
    
    dropdowns = page.locator(".el-select-dropdown").all()
    for i, dd in enumerate(dropdowns):
        visible = dd.is_visible()
        display = dd.evaluate("el => window.getComputedStyle(el).display")
        print(f"  [{i}] visible={visible} display={display}")
    
    page.screenshot(path=os.path.join(SCREENSHOT_DIR, "recon-major-select.png"), full_page=True)
    
    browser.close()
    print("\n侦察完成")