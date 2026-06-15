"""
Debug: Simulate the college delete test after running 6 prior tests.
Reproduce the issue where the delete doesn't work when run as test #7.
"""
from playwright.sync_api import sync_playwright

FRONTEND_URL = 'http://localhost:3000'

with sync_playwright() as p:
    browser = p.chromium.launch(headless=False, slow_mo=100)
    context = browser.new_context(viewport={'width': 1440, 'height': 900})
    page = context.new_page()
    
    # === Simulate running 6 tests first (same browser context) ===
    for i in range(6):
        print(f"\n=== Simulating test {i+1} ===")
        page.goto(f'{FRONTEND_URL}/login', wait_until='networkidle')
        page.wait_for_selector('.el-input__inner', timeout=10000)
        role_tab = page.locator('.role-tab').filter(has_text='管理员')
        if role_tab.count() > 0:
            role_tab.first.click()
            page.wait_for_timeout(300)
        inputs = page.locator('.el-input__inner')
        inputs.first.fill('admin')
        inputs.nth(1).fill('123456')
        page.locator('.login-btn').first.click()
        page.wait_for_load_state('networkidle')
        page.wait_for_timeout(1000)
        page.goto(f'{FRONTEND_URL}/admin/college-major', wait_until='networkidle')
        page.wait_for_timeout(500)
        print(f"  Login OK, on page: {page.url}")
    
    # === Now run the delete test ===
    print("\n=== Running delete test ===")
    uniqueName = f"可删除学院_{int(__import__('time').time() * 1000)}"
    
    # Create college
    page.locator('button').filter(has_text='添加学院').first.click()
    page.wait_for_selector('.el-dialog', timeout=5000)
    dialog = page.locator('.el-dialog').last
    dialog.locator('.el-input__inner').first.fill(uniqueName)
    dialog.locator('.el-dialog__footer .el-button--primary').last.click()
    
    # Wait for success
    success = page.wait_for_selector('.el-message--success', timeout=5000)
    if success:
        print(f"  Created: {uniqueName}")
    page.wait_for_timeout(500)
    page.screenshot(path='d:/789/frontend/tests/e2e/screenshots/debug_college_01_created.png')
    
    # Search
    search_input = page.locator('input[placeholder*="搜索学院"]').first
    search_input.fill(uniqueName)
    page.locator('button').filter(has_text='搜索').first.click()
    page.wait_for_timeout(2000)
    page.wait_for_load_state('networkidle')
    page.screenshot(path='d:/789/frontend/tests/e2e/screenshots/debug_college_02_search.png')
    
    # Check what's in the table
    tbody = page.locator('.el-table__body tbody').first
    tbody_text = tbody.text_content()
    print(f"  Search result: {tbody_text[:200]}")
    
    # Delete
    row = page.locator('.el-table__body tbody tr').filter(has_text=uniqueName)
    delete_btn = row.locator('button').filter(has_text='删除')
    print(f"  Delete button count: {delete_btn.count()}")
    if delete_btn.count() > 0:
        delete_btn.first.click()
        page.wait_for_timeout(500)
        page.screenshot(path='d:/789/frontend/tests/e2e/screenshots/debug_college_03_delete_clicked.png')
        
        # Wait for message box
        msgbox = page.wait_for_selector('.el-message-box', timeout=5000)
        if msgbox:
            print("  ElMessageBox found!")
            page.wait_for_timeout(300)
            # Check buttons
            btns = page.locator('.el-message-box__btns button')
            for j in range(btns.count()):
                btn = btns.nth(j)
                print(f"    Button {j}: '{btn.text_content()}' class={btn.get_attribute('class')}")
            
            page.screenshot(path='d:/789/frontend/tests/e2e/screenshots/debug_college_04_msgbox.png')
            page.locator('.el-message-box__btns .el-button--primary').first.click()
            page.wait_for_timeout(500)
        else:
            print("  ElMessageBox NOT found!")
        
        # Wait for result
        result = page.wait_for_selector('.el-message--success, .el-message--error', timeout=5000)
        if result:
            result_text = result.text_content()
            print(f"  Result message: {result_text}")
            page.screenshot(path='d:/789/frontend/tests/e2e/screenshots/debug_college_05_result.png')
        else:
            print("  No result message!")
    else:
        print("  No delete button found!")
    
    # Search again
    page.wait_for_timeout(1000)
    search_input2 = page.locator('input[placeholder*="搜索学院"]').first
    search_input2.clear()
    search_input2.fill(uniqueName)
    page.locator('button').filter(has_text='搜索').first.click()
    page.wait_for_timeout(2000)
    page.wait_for_load_state('networkidle')
    page.screenshot(path='d:/789/frontend/tests/e2e/screenshots/debug_college_06_after_delete.png')
    
    tbody = page.locator('.el-table__body tbody').first
    tbody_text = tbody.text_content()
    print(f"  After delete search: {tbody_text[:200]}")
    
    print("\n=== Done ===")
    browser.close()