"""
学院专业管理模块 Playwright 详细测试 - 含网络请求追踪
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
    
    # 收集所有网络请求
    network_requests = []
    page.on("request", lambda req: network_requests.append({
        "url": req.url, 
        "method": req.method,
        "resource_type": req.resource_type
    }))
    page.on("response", lambda resp: 
        print(f"  [{resp.status}] {resp.request.method} {resp.url[:80]}") 
        if "api" in resp.url or "auth" in resp.url or resp.status >= 400 else None
    )
    
    # 收集控制台
    console_msgs = []
    page.on("console", lambda msg: console_msgs.append(f"[{msg.type}] {msg.text}"))
    
    # 收集页面错误
    page_errors = []
    page.on("pageerror", lambda err: page_errors.append(str(err)))
    
    print("=" * 60)
    print("1. 打开登录页")
    print("=" * 60)
    page.goto(f"{BASE_URL}/login")
    page.wait_for_load_state("networkidle")
    page.wait_for_timeout(2000)
    screenshot(page, "01-login")
    
    # 查看页面内容
    body_text = page.locator("body").inner_text()[:500]
    print(f"  页面内容预览: {body_text[:200]}")
    
    print("\n" + "=" * 60)
    print("2. 执行管理员登录")
    print("=" * 60)
    inputs = page.locator(".el-input__inner").all()
    print(f"  输入框数量: {len(inputs)}")
    if len(inputs) >= 2:
        inputs[0].click()
        inputs[0].fill("admin")
        inputs[1].click()
        inputs[1].fill("123456")
    
    page.locator(".el-button--primary").first.click()
    page.wait_for_timeout(3000)
    page.wait_for_load_state("networkidle")
    screenshot(page, "02-after-login-click")
    
    # 检查错误消息
    error_msg = page.locator(".el-message--error, .el-message").all()
    print(f"  错误消息弹窗数量: {len(error_msg)}")
    for msg in error_msg:
        try:
            text = msg.inner_text()
            print(f"    消息: {text}")
        except:
            pass
    
    # 检查当前URL
    print(f"  当前 URL: {page.url}")
    
    # 检查 localStorage
    local_storage = page.evaluate("() => JSON.stringify(localStorage)")
    print(f"  localStorage: {local_storage[:300]}")
    
    # 尝试用 API 直接测试登录
    print("\n" + "=" * 60)
    print("3. 直接测试 BFF 登录 API")
    print("=" * 60)
    api_resp = page.evaluate("""
        async () => {
            try {
                const resp = await fetch('/api/auth/login', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ username: 'admin', password: '123456' })
                });
                const data = await resp.json();
                return { status: resp.status, data: data };
            } catch (e) {
                return { error: e.message };
            }
        }
    """)
    print(f"  API 响应: {json.dumps(api_resp, ensure_ascii=False, indent=2)}")
    
    # 测试后端直接 API
    print("\n" + "=" * 60)
    print("4. 直接测试后端 API")
    print("=" * 60)
    api_resp2 = page.evaluate("""
        async () => {
            try {
                const resp = await fetch('http://localhost:8080/api/auth/login', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ username: 'admin', password: '123456' })
                });
                const data = await resp.json();
                return { status: resp.status, data: data };
            } catch (e) {
                return { error: e.message };
            }
        }
    """)
    print(f"  后端 API 响应: {json.dumps(api_resp2, ensure_ascii=False, indent=2)[:300]}")
    
    print("\n" + "=" * 60)
    print("5. API 请求汇总 (仅 /api/ 相关)")
    print("=" * 60)
    api_requests = [r for r in network_requests if "/api/" in r.get("url", "")]
    print(f"  API 请求数: {len(api_requests)}")
    for r in api_requests[:10]:
        print(f"    {r['method']} {r['url'][:100]}")
    
    if page_errors:
        print(f"\n  页面运行时错误 ({len(page_errors)}):")
        for err in page_errors:
            print(f"    - {err}")
    
    browser.close()
    print("\n测试完成！")