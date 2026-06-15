"""
调试 Cookie 传递 — 捕获登录响应中的 Set-Cookie 和后续请求的 Cookie
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
    
    # 收集所有请求和响应详情
    request_details = []
    
    def on_request(request):
        if "/api/" in request.url:
            headers = dict(request.headers)
            request_details.append({
                "type": "request",
                "url": request.url,
                "method": request.method,
                "has_cookie": "cookie" in headers,
                "cookie": headers.get("cookie", ""),
                "headers": headers
            })
    
    def on_response(response):
        if "/api/" in response.request.url:
            headers = dict(response.headers)
            request_details.append({
                "type": "response",
                "url": response.request.url,
                "status": response.status,
                "set_cookie": headers.get("set-cookie", ""),
                "headers": headers
            })
    
    page.on("request", on_request)
    page.on("response", on_response)
    
    console_msgs = []
    page.on("console", lambda msg: console_msgs.append(f"[{msg.type}] {msg.text}"))
    
    print("=" * 60)
    print("登录 Cookie 调试")
    print("=" * 60)
    
    page.goto(f"{BASE_URL}/login")
    page.wait_for_load_state("networkidle")
    page.wait_for_timeout(1500)
    
    # 选择管理员
    admin_tab = page.locator(".role-tab").filter(has_text="管理员")
    admin_tab.first.click()
    page.wait_for_timeout(300)
    
    # 填写
    inputs = page.locator(".el-input__inner").all()
    visible_inputs = [i for i in inputs if i.is_visible()]
    visible_inputs[0].fill("admin")
    visible_inputs[1].fill("123456")
    
    # 点击登录
    page.locator(".login-btn").first.click()
    page.wait_for_timeout(6000)
    page.wait_for_load_state("networkidle")
    
    print(f"\n  URL: {page.url}")
    
    # 打印所有请求/响应详情
    print("\n  请求/响应详情:")
    for detail in request_details:
        if detail["type"] == "request":
            print(f"    REQ [{detail['method']}] {detail['url']}")
            print(f"         Cookie: {'YES' if detail['has_cookie'] else 'NO'} | {detail['cookie'][:80] if detail['cookie'] else 'N/A'}")
        else:
            print(f"    RES [{detail['status']}] {detail['url']}")
            print(f"         Set-Cookie: {detail['set_cookie'][:120] if detail['set_cookie'] else 'N/A'}")
    
    # 打印控制台
    print("\n  控制台消息:")
    for msg in console_msgs:
        print(f"    {msg}")
    
    # 检查 localStorage
    user = page.evaluate("() => localStorage.getItem('user')")
    print(f"\n  localStorage user: {user}")
    
    # 检查 Cookie
    cookies = context.cookies()
    print(f"\n  Browser Cookies ({len(cookies)}):")
    for c in cookies:
        print(f"    {c['name']}={c['value'][:20]}... domain={c['domain']} path={c['path']} httpOnly={c['httpOnly']}")
    
    browser.close()