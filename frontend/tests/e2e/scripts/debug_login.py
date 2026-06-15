"""
调试脚本 - 捕获完整的登录流程和错误信息
"""
from playwright.sync_api import sync_playwright
import os

BASE_URL = "http://localhost:3000"
SCREENSHOT_DIR = os.path.join(os.path.dirname(__file__), "screenshots2")
os.makedirs(SCREENSHOT_DIR, exist_ok=True)

def screenshot(page, name):
    try:
        path = os.path.join(SCREENSHOT_DIR, f"{name}.png")
        page.screenshot(path=path, full_page=True)
        print(f"  [截图] {name}")
        return path
    except Exception as e:
        print(f"  [截图失败] {name}: {e}")
        return None

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    context = browser.new_context(viewport={"width": 1440, "height": 900})
    page = context.new_page()
    
    # 收集控制台消息
    console_msgs = []
    page.on("console", lambda msg: console_msgs.append(f"[{msg.type}] {msg.text}"))
    
    # 收集网络请求体
    api_calls = []
    page.on("response", lambda resp: 
        api_calls.append(f"[{resp.status}] {resp.request.method} {resp.request.url}") 
        if "/api/" in resp.request.url else None
    )
    
    print("=" * 60)
    print("1. 管理员登录调试")
    print("=" * 60)
    
    page.goto(f"{BASE_URL}/login")
    page.wait_for_load_state("networkidle")
    page.wait_for_timeout(1500)
    
    # 选择管理员角色
    admin_tab = page.locator(".role-tab").filter(has_text="管理员")
    if admin_tab.count() > 0 and admin_tab.first.is_visible():
        admin_tab.first.click()
        page.wait_for_timeout(300)
        print("  ✓ 已选择管理员")
    
    # 填写表单
    inputs = page.locator(".el-input__inner").all()
    visible_inputs = [i for i in inputs if i.is_visible()]
    if len(visible_inputs) >= 2:
        visible_inputs[0].fill("admin")
        visible_inputs[1].fill("123456")
        print("  ✓ 已填写账号密码")
    
    screenshot(page, "debug-01-before-login")
    
    # 点击登录
    page.locator(".login-btn").first.click()
    print("  ✓ 已点击登录")
    
    page.wait_for_timeout(6000)
    page.wait_for_load_state("networkidle")
    page.wait_for_timeout(1000)
    
    screenshot(page, "debug-02-after-login")
    print(f"  URL: {page.url}")
    
    # 展示所有控制台消息
    print("\n  控制台消息:")
    for msg in console_msgs:
        print(f"    {msg}")
    
    # 展示所有 API 调用
    print("\n  API 调用:")
    for call in api_calls:
        print(f"    {call}")
    
    # 检查消息提示
    print("\n  消息提示:")
    for cls in [".el-message--error", ".el-message--success", ".el-message--warning", ".el-message"]:
        msgs = page.locator(cls).all()
        for msg in msgs:
            try:
                if msg.is_visible():
                    print(f"    [{cls}] {msg.inner_text()}")
            except:
                pass
    
    # 检查 localStorage
    try:
        user = page.evaluate("() => localStorage.getItem('user')")
        print(f"\n  localStorage user: {user}")
    except:
        print("  localStorage: 无法读取")
    
    # 检查是否有弹窗/遮挡
    try:
        body = page.locator("body").inner_text()[:1000]
        print(f"\n  页面内容:\n{body[:800]}")
    except:
        pass
    
    browser.close()
    print("\n" + "=" * 60)
    print("调试完成")
    print("=" * 60)