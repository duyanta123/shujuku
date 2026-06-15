"""Browser reconnaissance for college-major page - simplified."""
from playwright.sync_api import sync_playwright

COLLEGE_MAJOR_URL = 'http://localhost:3000/admin/college-major'
BFF_URL = 'http://localhost:3000'

with sync_playwright() as p:
    browser = p.chromium.launch(headless=True)
    context = browser.new_context(viewport={'width': 1920, 'height': 1080})
    page = context.new_page()
    
    # 1. Go to login
    page.goto(f'{BFF_URL}/login')
    page.wait_for_load_state('networkidle')
    page.wait_for_timeout(1000)
    page.screenshot(path='recon_01_login.png')
    
    # Select admin role tab
    role_tabs = page.locator('.role-tab')
    count = role_tabs.count()
    print(f"Role tabs: {count}")
    for i in range(count):
        tab = role_tabs.nth(i)
        text = tab.text_content() or ''
        print(f"  Tab {i}: '{text.strip()}'")
        if '管理员' in text:
            tab.click()
            print(f"  -> Clicked admin tab")
            break
    page.wait_for_timeout(500)
    
    # Fill credentials
    inputs = page.locator('.el-input__inner')
    inputs.first.fill('admin')
    inputs.nth(1).fill('123456')
    page.screenshot(path='recon_02_login_filled.png')
    
    # Click login
    page.locator('.login-btn').click()
    page.wait_for_timeout(3000)
    page.screenshot(path='recon_03_after_login.png')
    print(f"After login URL: {page.url}")
    
    # Check if still on login page
    if '/login' in page.url:
        print("Still on login page! Checking for error messages...")
        error = page.locator('.el-message--error')
        if error.count() > 0:
            print(f"Error message: {error.first.text_content()}")
        # Try to get page content
        content = page.content()
        # Print a snippet
        print("Page title:", page.title())
    
    # 2. Try navigating to college-major directly
    page.goto(COLLEGE_MAJOR_URL)
    page.wait_for_load_state('networkidle')
    page.wait_for_timeout(2000)
    print(f"After goto college-major URL: {page.url}")
    page.screenshot(path='recon_04_college_page.png', full_page=True)
    
    # 3. Check college tab
    print("\n=== COLLEGE TAB ===")
    rows = page.locator('.el-table__body tbody tr')
    row_count = rows.count()
    print(f"Rows in college table: {row_count}")
    if row_count > 0:
        print(f"First row text: {rows.first.text_content()}")
    
    # Check search input
    search_input = page.locator('input[placeholder*="搜索学院"]')
    print(f"Search input count: {search_input.count()}")
    
    # Check pagination
    pagination = page.locator('.el-pagination')
    if pagination.count() > 0:
        print(f"Pagination text: {pagination.first.text_content()}")
    
    # 4. Test search
    print("\n=== SEARCH TEST ===")
    if search_input.count() > 0:
        search_input.first.fill('计算机')
        search_input.first.press('Enter')
        page.wait_for_timeout(2000)
        page.wait_for_load_state('networkidle')
        rows = page.locator('.el-table__body tbody tr')
        print(f"Rows after search '计算机': {rows.count()}")
        page.screenshot(path='recon_05_search_result.png', full_page=True)
    
    # 5. Switch to major tab
    print("\n=== MAJOR TAB ===")
    major_tab = page.locator('.el-tabs__item').filter(has_text='专业管理')
    print(f"Major tab count: {major_tab.count()}")
    if major_tab.count() > 0:
        major_tab.click()
        page.wait_for_load_state('networkidle')
        page.wait_for_timeout(2000)
        page.screenshot(path='recon_06_major_page.png', full_page=True)
        
        rows = page.locator('.el-table__body tbody tr')
        print(f"Rows in major table: {rows.count()}")
        if rows.count() > 0:
            print(f"First row: {rows.first.text_content()}")
    
    # 6. Check filter select
    print("\n=== MAJOR FILTER ===")
    filter_select = page.locator('.el-select').first
    print(f"Filter select visible: {filter_select.is_visible()}")
    filter_select.click()
    page.wait_for_timeout(1000)
    page.screenshot(path='recon_07_filter_dropdown.png')
    
    dropdowns = page.locator('.el-select-dropdown')
    for i in range(dropdowns.count()):
        dd = dropdowns.nth(i)
        try:
            v = dd.is_visible()
        except:
            v = 'err'
        o = dd.locator('.el-select-dropdown__item').count()
        print(f"  Dropdown {i}: visible={v}, options={o}")
    
    browser.close()
    print("\n=== DONE ===")