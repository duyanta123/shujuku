"""
Reconnaissance script: Investigate the major tab DOM structure to understand
why .el-table__body tbody selectors are ambiguous between college and major tabs.
"""
from playwright.sync_api import sync_playwright
import time

FRONTEND_URL = 'http://localhost:3000'

with sync_playwright() as p:
    browser = p.chromium.launch(headless=False, slow_mo=300)
    context = browser.new_context(viewport={'width': 1440, 'height': 900})
    page = context.new_page()
    
    # Step 1: Login as admin
    print("=== Step 1: Login ===")
    page.goto(f'{FRONTEND_URL}/login', wait_until='networkidle')
    page.wait_for_timeout(1000)
    
    # Fill login form (using Element Plus selectors matching auth.js)
    page.wait_for_selector('.el-input__inner', timeout=10000)
    # Select admin role
    role_tab = page.locator('.role-tab').filter(has_text='管理员')
    if role_tab.count() > 0:
        role_tab.first.click()
        page.wait_for_timeout(300)
    inputs = page.locator('.el-input__inner')
    inputs.first.fill('admin')
    inputs.nth(1).fill('123456')
    
    # Click login button
    page.locator('.login-btn').first.click()
    page.wait_for_load_state('networkidle')
    page.wait_for_timeout(2000)
    print(f"  URL after login: {page.url}")
    
    # Step 2: Navigate to college-major page
    print("\n=== Step 2: Navigate to College-Major Page ===")
    page.goto(f'{FRONTEND_URL}/admin/college-major', wait_until='networkidle')
    page.wait_for_timeout(1500)
    page.screenshot(path='d:/789/frontend/tests/e2e/screenshots/recon_01_college_tab.png', full_page=True)
    
    # Step 3: Check the tab structure
    print("\n=== Step 3: Tab Structure ===")
    tabs = page.locator('.el-tabs__item')
    tab_count = tabs.count()
    print(f"  Number of tabs: {tab_count}")
    for i in range(tab_count):
        tab = tabs.nth(i)
        text = tab.text_content()
        classes = tab.get_attribute('class')
        print(f"  Tab {i}: '{text}' | class: {classes}")
    
    # Step 4: Check table bodies on the college tab (default)
    print("\n=== Step 4: College Tab - Table Bodies ===")
    tbodies = page.locator('.el-table__body tbody')
    tbody_count = tbodies.count()
    print(f"  Number of .el-table__body tbody: {tbody_count}")
    for i in range(tbody_count):
        tbody = tbodies.nth(i)
        visible = tbody.is_visible()
        text = tbody.text_content()[:100] if visible else "HIDDEN"
        print(f"  tbody[{i}]: visible={visible}, text={text}")
    
    # Step 5: Check tab pane display states
    print("\n=== Step 5: Tab Pane Display States ===")
    tab_panes = page.locator('.el-tab-pane')
    pane_count = tab_panes.count()
    print(f"  Number of tab panes: {pane_count}")
    for i in range(pane_count):
        pane = tab_panes.nth(i)
        style = pane.get_attribute('style') or ''
        label = pane.get_attribute('aria-label') or pane.get_attribute('label') or ''
        # Check the label via text content of the associated tab
        # Try to get the id
        pane_id = pane.get_attribute('id') or ''
        print(f"  pane[{i}]: id={pane_id}, style={style}, label_attr={label}")
    
    # Step 6: Click the major tab
    print("\n=== Step 6: Switch to Major Tab ===")
    major_tab = page.locator('.el-tabs__item').filter(has_text='专业管理')
    major_tab.click()
    page.wait_for_load_state('networkidle')
    page.wait_for_timeout(2000)
    page.screenshot(path='d:/789/frontend/tests/e2e/screenshots/recon_02_major_tab.png', full_page=True)
    
    # Step 7: Check table bodies after switching to major tab
    print("\n=== Step 7: Major Tab - Table Bodies ===")
    tbodies = page.locator('.el-table__body tbody')
    tbody_count = tbodies.count()
    print(f"  Number of .el-table__body tbody: {tbody_count}")
    for i in range(tbody_count):
        tbody = tbodies.nth(i)
        visible = tbody.is_visible()
        text = tbody.text_content()[:100] if visible else "HIDDEN"
        print(f"  tbody[{i}]: visible={visible}, text={text}")
    
    # Step 8: Check tab pane display states after switching
    print("\n=== Step 8: Tab Pane Display States After Switch ===")
    tab_panes = page.locator('.el-tab-pane')
    for i in range(tab_panes.count()):
        pane = tab_panes.nth(i)
        style = pane.get_attribute('style') or ''
        pane_id = pane.get_attribute('id') or ''
        # Check aria-hidden
        aria_hidden = pane.get_attribute('aria-hidden') or ''
        print(f"  pane[{i}]: id={pane_id}, style={style}, aria-hidden={aria_hidden}")
    
    # Step 9: Test search functionality on major tab
    print("\n=== Step 9: Search on Major Tab ===")
    search_input = page.locator('input[placeholder*="搜索专业"]').first
    print(f"  Search input visible: {search_input.is_visible()}")
    search_input.fill('计算机')
    page.wait_for_timeout(300)
    search_input.press('Enter')
    page.wait_for_load_state('networkidle')
    page.wait_for_timeout(2000)
    page.screenshot(path='d:/789/frontend/tests/e2e/screenshots/recon_03_major_search.png', full_page=True)
    
    # Check what data is in each tbody after search
    print("\n=== Step 10: Table Bodies After Search ===")
    tbodies = page.locator('.el-table__body tbody')
    for i in range(tbodies.count()):
        tbody = tbodies.nth(i)
        visible = tbody.is_visible()
        text = tbody.text_content()[:200] if visible else "HIDDEN"
        print(f"  tbody[{i}]: visible={visible}")
        # Print row count for each
        rows = tbody.locator('tr')
        print(f"    rows: {rows.count()}, text: {text[:150]}")
    
    # Step 11: Test with scoped selector
    print("\n=== Step 11: Scoped Selector Test ===")
    # Try to find the active tab pane (not display:none)
    active_pane = page.locator('.el-tab-pane:not([style*="display: none"])')
    active_count = active_pane.count()
    print(f"  Active panes: {active_count}")
    for i in range(active_count):
        pane = active_pane.nth(i)
        print(f"  Active pane[{i}]: visible={pane.is_visible()}, text={pane.text_content()[:100]}")
    
    # Test the scoped tbody selector
    scoped_tbody = page.locator('.el-tab-pane:not([style*="display: none"]) .el-table__body tbody')
    print(f"  Scoped tbody count: {scoped_tbody.count()}")
    for i in range(scoped_tbody.count()):
        tbody = scoped_tbody.nth(i)
        print(f"  Scoped tbody[{i}]: visible={tbody.is_visible()}, text={tbody.text_content()[:150]}")
    
    # Step 12: Check column headers to identify tables
    print("\n=== Step 12: Column Headers ===")
    all_headers = page.locator('th')
    for i in range(all_headers.count()):
        th = all_headers.nth(i)
        visible = th.is_visible()
        text = th.text_content()
        if visible:
            print(f"  th[{i}]: visible={visible}, text='{text}'")
    
    print("\n=== Done ===")
    browser.close()