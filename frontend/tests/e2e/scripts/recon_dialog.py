"""
侦察 dialog 实际 DOM 结构 — 找到正确的 select 选择器
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
    
    # ============================================
    print("=" * 60)
    print("1. 学院添加弹窗 DOM 结构")
    print("=" * 60)
    
    page.locator("button").filter(has_text="添加学院").first.click()
    page.wait_for_timeout(2000)
    page.wait_for_selector(".el-dialog", timeout=3000)
    
    dialog = page.locator(".el-dialog").last
    print(f"弹窗标题: {dialog.locator('.el-dialog__title').inner_text()}")
    
    # 打印 dialog 内所有 el-select 的完整 HTML
    selects = dialog.locator(".el-select").all()
    print(f"\nel-select 数量: {len(selects)}")
    for i, sel in enumerate(selects):
        if sel.is_visible():
            html = sel.inner_html()
            print(f"\n  [{i}] HTML: {html[:300]}")
            
            # 检查内部结构
            inner = sel.locator("*").all()
            for child in inner:
                try:
                    tag = child.evaluate("el => el.tagName")
                    cls = child.get_attribute("class") or ""
                    if cls:
                        print(f"       <{tag}> class='{cls[:80]}'")
                except:
                    pass
    
    # 打印 dialog 内所有 input 元素
    all_inputs = dialog.locator("input").all()
    print(f"\ninput 元素数量: {len(all_inputs)}")
    for i, inp in enumerate(all_inputs):
        if inp.is_visible():
            cls = inp.get_attribute("class") or ""
            placeholder = inp.get_attribute("placeholder") or ""
            readonly = inp.get_attribute("readonly") or ""
            print(f"  [{i}] class='{cls[:80]}' placeholder='{placeholder}' readonly={readonly}")
    
    page.screenshot(path=os.path.join(SCREENSHOT_DIR, "recon-college-dialog.png"), full_page=True)
    
    # 关闭弹窗
    dialog.locator("button").filter(has_text="取消").first.click()
    page.wait_for_timeout(1000)
    
    # ============================================
    print("\n" + "=" * 60)
    print("2. 专业添加弹窗 DOM 结构")
    print("=" * 60)
    
    # 切换到专业管理Tab
    page.locator(".el-tabs__item").filter(has_text="专业管理").first.click()
    page.wait_for_timeout(2000)
    page.wait_for_load_state("networkidle")
    
    page.locator("button").filter(has_text="添加专业").first.click()
    page.wait_for_timeout(2000)
    page.wait_for_selector(".el-dialog", timeout=3000)
    
    dialog = page.locator(".el-dialog").last
    print(f"弹窗标题: {dialog.locator('.el-dialog__title').inner_text()}")
    
    selects = dialog.locator(".el-select").all()
    print(f"\nel-select 数量: {len(selects)}")
    for i, sel in enumerate(selects):
        if sel.is_visible():
            html = sel.inner_html()
            print(f"\n  [{i}] HTML: {html[:300]}")
            inner = sel.locator("*").all()
            for child in inner:
                try:
                    tag = child.evaluate("el => el.tagName")
                    cls = child.get_attribute("class") or ""
                    if cls:
                        print(f"       <{tag}> class='{cls[:80]}'")
                except:
                    pass
    
    all_inputs = dialog.locator("input").all()
    print(f"\ninput 元素数量: {len(all_inputs)}")
    for i, inp in enumerate(all_inputs):
        if inp.is_visible():
            cls = inp.get_attribute("class") or ""
            placeholder = inp.get_attribute("placeholder") or ""
            readonly = inp.get_attribute("readonly") or ""
            print(f"  [{i}] class='{cls[:80]}' placeholder='{placeholder}' readonly={readonly}")
    
    # 测试用不同方式点击 select
    print("\n测试 select 点击方式:")
    
    # 方式1: 直接点击 .el-select
    try:
        sel = dialog.locator(".el-select").first
        sel.click()
        page.wait_for_timeout(500)
        dropdown = page.locator(".el-select-dropdown").first
        if dropdown.is_visible():
            print("  方式1 (.el-select 直接点击): OK")
            page.keyboard.press("Escape")
            page.wait_for_timeout(300)
        else:
            print("  方式1 (.el-select 直接点击): 失败 - dropdown 未出现")
    except Exception as e:
        print(f"  方式1: 异常 - {e}")
    
    # 方式2: 点击内部的 .el-select__wrapper
    try:
        wrapper = dialog.locator(".el-select__wrapper").first
        if wrapper.is_visible():
            wrapper.click()
            page.wait_for_timeout(500)
            dropdown = page.locator(".el-select-dropdown").first
            if dropdown.is_visible():
                print("  方式2 (.el-select__wrapper 点击): OK")
                page.keyboard.press("Escape")
                page.wait_for_timeout(300)
            else:
                print("  方式2 (.el-select__wrapper 点击): 失败")
        else:
            print("  方式2: .el-select__wrapper 不存在")
    except Exception as e:
        print(f"  方式2: 异常 - {e}")
    
    # 方式3: 点击内部的 input (readonly)
    try:
        readonly_input = dialog.locator("input[readonly]").first
        if readonly_input.is_visible():
            readonly_input.click()
            page.wait_for_timeout(500)
            dropdown = page.locator(".el-select-dropdown").first
            if dropdown.is_visible():
                print("  方式3 (input[readonly] 点击): OK")
                page.keyboard.press("Escape")
                page.wait_for_timeout(300)
            else:
                print("  方式3 (input[readonly] 点击): 失败")
        else:
            print("  方式3: input[readonly] 不存在")
    except Exception as e:
        print(f"  方式3: 异常 - {e}")
    
    page.screenshot(path=os.path.join(SCREENSHOT_DIR, "recon-major-dialog.png"), full_page=True)
    
    # 关闭弹窗
    dialog.locator("button").filter(has_text="取消").first.click()
    page.wait_for_timeout(1000)
    
    # ============================================
    print("\n" + "=" * 60)
    print("3. 搜索栏 DOM 结构")
    print("=" * 60)
    
    # 搜索按钮
    search_btn = page.locator("button").filter(has_text="搜索").first
    print(f"搜索按钮: visible={search_btn.is_visible()}")
    cls = search_btn.get_attribute("class") or ""
    print(f"  按钮 class: {cls}")
    
    # 学院管理Tab搜索
    page.locator(".el-tabs__item").first.click()
    page.wait_for_timeout(1000)
    search_btn2 = page.locator("button").filter(has_text="搜索").first
    print(f"学院Tab搜索按钮: visible={search_btn2.is_visible()}")
    
    # 查找所有 .el-select 的 wrapper
    print("\n页面中所有 el-select:")
    all_selects = page.locator(".el-select").all()
    for i, sel in enumerate(all_selects):
        if sel.is_visible():
            html = sel.inner_html()[:200]
            print(f"  [{i}] {html[:150]}")
    
    browser.close()
    print("\n侦察完成")