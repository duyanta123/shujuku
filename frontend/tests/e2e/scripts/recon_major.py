"""Debug major search."""
from playwright.sync_api import sync_playwright

BFF_URL = 'http://localhost:3000'
COLLEGE_MAJOR_URL = 'http://localhost:3000/admin/college-major'

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    context = browser.new_context(viewport={'width': 1920, 'height': 1080})
    page = context.new_page()
    
    page.goto(f'{BFF_URL}/login')
    page.wait_for_load_state('networkidle')
    page.wait_for_timeout(1000)
    
    role_tabs = page.locator('.role-tab')
    for i in range(role_tabs.count()):
        tab = role_tabs.nth(i)
        if '管理员' in (tab.text_content() or ''):
            tab.click()
            break
    page.wait_for_timeout(500)
    
    inputs = page.locator('.el-input__inner')
    inputs.first.fill('admin')
    inputs.nth(1).fill('123456')
    page.locator('.login-btn').click()
    page.wait_for_timeout(3000)
    
    # Navigate
    page.goto(COLLEGE_MAJOR_URL)
    page.wait_for_load_state('networkidle')
    page.wait_for_timeout(2000)
    
    # Switch to major tab
    major_tab = page.locator('.el-tabs__item').filter(has_text='专业管理')
    major_tab.click()
    page.wait_for_timeout(1000)
    page.wait_for_load_state('networkidle')
    page.wait_for_timeout(1000)
    page.screenshot(path='recon_major_01_tab.png')
    
    # Check search input
    search_input = page.locator('input[placeholder*="搜索专业"]')
    print(f"Search input count: {search_input.count()}")
    
    if search_input.count() > 0:
        search_input.first.click()
        search_input.first.fill('')
        search_input.first.type('计算机', delay=50)
        page.wait_for_timeout(500)
        page.screenshot(path='recon_major_02_input.png')
        
        # Try Enter
        page.keyboard.press('Enter')
        page.wait_for_timeout(3000)
        page.wait_for_load_state('networkidle')
        page.screenshot(path='recon_major_03_after_enter.png')
        
        rows = page.locator('.el-table__body tbody tr')
        print(f"Rows after Enter: {rows.count()}")
        if rows.count() > 0:
            for i in range(min(rows.count(), 3)):
                print(f"  Row {i}: {rows.nth(i).text_content()[:80]}")
    
    browser.close()