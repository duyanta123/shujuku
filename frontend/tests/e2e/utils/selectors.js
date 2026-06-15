/**
 * Element Plus 组件选择器常量
 * 封装常用的 CSS 选择器，便于测试脚本复用和稳定维护
 */
const SELECTORS = {
  // 通用
  DIALOG: '.el-dialog',
  DIALOG_BODY: '.el-dialog__body',
  DIALOG_HEADER: '.el-dialog__header',
  DIALOG_FOOTER: '.el-dialog__footer',
  CONFIRM_BUTTON: '.el-dialog__footer .el-button--primary',
  CANCEL_BUTTON: '.el-dialog__footer .el-button:not(.el-button--primary)',

  // 表单
  FORM: '.el-form',
  FORM_ITEM: '.el-form-item',
  FORM_ERROR: '.el-form-item__error',
  INPUT: '.el-input__inner',
  TEXTAREA: '.el-textarea__inner',
  SELECT: '.el-select',
  SELECT_DROPDOWN: '.el-select-dropdown',
  SELECT_OPTION: '.el-select-dropdown__item',
  SELECT_INPUT: '.el-select .el-input__inner',

  // 表格
  TABLE: '.el-table',
  TABLE_BODY: '.el-table__body',
  TABLE_ROW: '.el-table__body tbody tr',
  TABLE_CELL: '.el-table__body tbody td',
  TABLE_EMPTY: '.el-table__empty-text',

  // 标签
  TAG: '.el-tag',
  TAG_SUCCESS: '.el-tag--success',
  TAG_INFO: '.el-tag--info',
  TAG_DANGER: '.el-tag--danger',

  // 按钮
  BUTTON_PRIMARY: '.el-button--primary',
  BUTTON_DANGER: '.el-button--danger',
  BUTTON_TEXT: '.el-button--text',
  BUTTON_LOADING: '.is-loading',

  // 分页
  PAGINATION: '.el-pagination',

  // Tabs
  TABS: '.el-tabs',
  TAB_HEADER: '.el-tabs__header',
  TAB_ITEM: '.el-tabs__item',
  TAB_CONTENT: '.el-tabs__content',
  TAB_ACTIVE: '.is-active',

  // MessageBox
  MESSAGE_BOX: '.el-message-box',
  MESSAGE_BOX_MSG: '.el-message-box__message',
  MESSAGE_BOX_CONFIRM: '.el-message-box__btns .el-button--primary',

  // Message (toast)
  MESSAGE: '.el-message',
  MESSAGE_CONTENT: '.el-message__content',

  // 侧边栏
  SIDEBAR: '.sidebar-nav, .nav-list, nav',
  NAV_ITEM: '.nav-item',

  // 加载
  LOADING: '.el-loading-mask',

  // 搜索 / 筛选
  SEARCH_INPUT: '.search-input, .el-input',
  FILTER_SELECT: '.filter-select, .el-select',
};

module.exports = { SELECTORS };