"""
调试 Cookie 传递 v2 — 用 fetch API 测试 cookies
"""
from playwright.sync_api import sync_playwright
import os

BASE_URL = "http://localhost:3000"

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    context = browser.new_context(viewport={"width": 1440, "height": 900})
    page = context.new_page()
    
    console_msgs = []
    page.on("console", lambda msg: console_msgs.append(f"[{msg.type}] {msg.text}"))
    
    print("=" * 60)
    print("Step 1: 登录并检查 Cookie")
    print("=" * 60)
    
    page.goto(f"{BASE_URL}/login")
    page.wait_for_load_state("networkidle")
    page.wait_for_timeout(1500)
    
    # 管理员登录
    page.locator(".role-tab").filter(has_text="管理员").first.click()
    page.wait_for_timeout(300)
    inputs = page.locator(".el-input__inner").all()
    visible_inputs = [i for i in inputs if i.is_visible()]
    visible_inputs[0].fill("admin")
    visible_inputs[1].fill("123456")
    page.locator(".login-btn").first.click()
    page.wait_for_timeout(5000)
    page.wait_for_load_state("networkidle")
    
    # 检查浏览器 Cookie
    cookies = context.cookies()
    print(f"  浏览器 Cookie 数量: {len(cookies)}")
    for c in cookies:
        print(f"    {c['name']} = {c['value'][:30]}... path={c['path']} httpOnly={c['httpOnly']}")
    
    # 用 fetch 测试 API 请求
    print("\n" + "=" * 60)
    print("Step 2: fetch API 测试 (credentials: 'include')")
    print("=" * 60)
    
    result = page.evaluate("""
        async () => {
            const response = await fetch('/api/college/list?status=ACTIVE&size=10', {
                credentials: 'include'
            });
            const data = await response.json();
            return { status: response.status, data: data };
        }
    """)
    print(f"  /api/college/list 状态: {result['status']}")
    print(f"  响应: {result['data']}")
    
    # 直接导航到 admin 页面
    print("\n" + "=" * 60)
    print("Step 3: 直接导航 /admin/college-major")
    print("=" * 60)
    
    page.goto(f"{BASE_URL}/admin/college-major")
    page.wait_for_load_state("networkidle")
    page.wait_for_timeout(3000)
    print(f"  URL: {page.url}")
    
    # 检查页面内容
    body_text = page.locator("body").inner_text()[:500]
    print(f"  页面内容: {body_text[:300]}")
    
    # 检查 localStorage
    user = page.evaluate("() => localStorage.getItem('user')")
    print(f"\n  localStorage user: {user}")
    
    # 打印所有控制台消息
    print("\n" + "=" * 60)
    print("控制台消息:")
    print("=" * 60)
    for msg in console_msgs:
        print(f"  {msg}")
    
    browser.close()