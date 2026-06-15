"""Quick test: verify search works after backend fix."""
from playwright.sync_api import sync_playwright

BFF_URL = 'http://localhost:3000'
COLLEGE_MAJOR_URL = 'http://localhost:3000/admin/college-major'

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    context = browser.new_context(viewport={'width': 1920, 'height': 1080})
    page = context.new_page()
    
    # Login
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
    print(f"Logged in: {page.url}")
    
    # Navigate
    page.goto(COLLEGE_MAJOR_URL)
    page.wait_for_load_state('networkidle')
    page.wait_for_timeout(2000)
    
    # Check initial table
    rows = page.locator('.el-table__body tbody tr')
    print(f"Initial rows: {rows.count()}")
    
    # Search for "计算机" 
    search_input = page.locator('input[placeholder*="搜索学院"]').first
    search_input.click()
    search_input.fill('')
    search_input.type('信息', delay=50)
    page.wait_for_timeout(500)
    page.locator('button').filter(has_text='搜索').first.click()
    page.wait_for_timeout(3000)
    page.wait_for_load_state('networkidle')
    
    rows = page.locator('.el-table__body tbody tr')
    print(f"Rows after search '信息': {rows.count()}")
    if rows.count() > 0:
        for i in range(min(rows.count(), 3)):
            print(f"  Row {i}: {rows.nth(i).text_content()[:60]}")
    
    found = False
    for i in range(rows.count()):
        text = rows.nth(i).text_content() or ''
        if '信息' in text:
            found = True
            break
    print(f"Search contains '信息': {found}")
    
    browser.close()
    print("\n=== DONE ===")