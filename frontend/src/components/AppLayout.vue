<template>
  <div class="layout-root">
    <div v-if="sidebarOpen" class="sidebar-overlay" @click="sidebarOpen = false"></div>

    <aside :class="['sidebar', { open: sidebarOpen }]">
      <div class="sidebar-brand">
        <svg width="28" height="28" viewBox="0 0 36 36" fill="none">
          <rect width="36" height="36" rx="8" fill="#c88d2c"/>
          <path d="M10 14h16M10 20h12M10 26h8" stroke="#fff" stroke-width="2" stroke-linecap="round"/>
        </svg>
        <div>
          <span class="brand-title">实验选课</span>
          <span class="brand-sub">{{ config.brandSub }}</span>
        </div>
      </div>

      <div class="user-info">
        <div class="avatar-wrapper" @click="showAvatarDialog = true" title="点击查看个人信息">
          <img
            :src="userStore.avatarUrl || placeholder"
            :alt="userStore.name"
            class="avatar-img"
            @error="onAvatarError"
          />
          <div class="avatar-overlay"></div>
        </div>
        <div class="user-detail">
          <p class="user-name">{{ userStore.name || config.userNameFallback }}</p>
        </div>
      </div>

      <nav class="sidebar-nav">
        <router-link
          v-for="item in config.navItems"
          :key="item.to"
          :to="item.to"
          class="nav-item"
          active-class="active"
          @click="sidebarOpen = false"
        >
          <span class="nav-icon">
            <svg class="nav-svg" viewBox="0 0 24 24" fill="none" aria-hidden="true">
              <component
                :is="node.tag"
                v-for="(node, index) in iconNodes(item.icon)"
                :key="index"
                v-bind="node.attrs"
              />
            </svg>
          </span>
          <span>{{ item.label }}</span>
        </router-link>
      </nav>

      <div class="sidebar-footer">
        <button class="logout-btn" @click="handleLogout">
          <span>退出登录</span>
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
            <path d="M6 2H3v12h3M11 11l3-3-3-3M14 8H6" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </button>
      </div>

      <AvatarDialog
        v-model="showAvatarDialog"
        :role="config.role"
        :user-name="userStore.name"
        :user-account="userStore.account"
        :avatar-url="userStore.avatarUrl"
        :college="userStore.college"
        :title="userStore.title"
        @avatar-updated="onAvatarUpdated"
      />
    </aside>

    <main class="main-area">
      <div class="mobile-topbar">
        <button class="hamburger" @click="sidebarOpen = !sidebarOpen" aria-label="菜单">
          <svg width="22" height="22" viewBox="0 0 22 22" fill="none">
            <path d="M3 6h16M3 11h16M3 16h16" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
          </svg>
        </button>
        <span class="topbar-title">{{ config.brandSub }}</span>
      </div>

      <slot />
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import AvatarDialog from '@/components/AvatarDialog.vue'
import userStore from '@/stores/userStore'

const props = defineProps({
  config: {
    type: Object,
    required: true,
    validator(value) {
      if (!value) return false
      const required = ['role', 'brandSub', 'userNameFallback', 'userRolePrefix',
        'accountFallback', 'placeholder', 'navItems']
      const missing = required.filter(f => !(f in value))
      if (missing.length) {
        console.warn(`[AppLayout] config 缺少必填字段: ${missing.join(', ')}`)
      }
      if (value.navItems && !Array.isArray(value.navItems)) {
        console.warn('[AppLayout] config.navItems 必须为数组')
      }
      if (Array.isArray(value.navItems)) {
        value.navItems.forEach((item, i) => {
          if (!item || !item.to || !item.icon || !item.label) {
            console.warn(`[AppLayout] config.navItems[${i}] 缺少必填字段（to/icon/label）`)
          }
        })
      }
      return true
    },
  },
})

const router = useRouter()
const sidebarOpen = ref(false)
const showAvatarDialog = ref(false)

const displayAccount = computed(() => {
  return userStore.account || props.config.accountFallback
})

const placeholder = computed(() => props.config.placeholder)

const ICONS = {
  course: [
    { tag: 'path', attrs: { d: 'M4 19.5A2.5 2.5 0 0 1 6.5 17H20' } },
    { tag: 'path', attrs: { d: 'M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z' } },
  ],
  student: [
    { tag: 'path', attrs: { d: 'M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2' } },
    { tag: 'circle', attrs: { cx: '9', cy: '7', r: '4' } },
    { tag: 'path', attrs: { d: 'M23 21v-2a4 4 0 0 0-3-3.87' } },
    { tag: 'path', attrs: { d: 'M16 3.13a4 4 0 0 1 0 7.75' } },
  ],
  teacher: [
    { tag: 'path', attrs: { d: 'M12 20h9' } },
    { tag: 'path', attrs: { d: 'M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z' } },
  ],
  lab: [
    { tag: 'path', attrs: { d: 'M10 2v7.31a2 2 0 0 1-.367 1.15L3 21h18l-6.633-10.54A2 2 0 0 1 14 9.31V2' } },
    { tag: 'line', attrs: { x1: '8', y1: '2', x2: '16', y2: '2' } },
    { tag: 'line', attrs: { x1: '12', y1: '8', x2: '12', y2: '12' } },
  ],
  college: [
    { tag: 'path', attrs: { d: 'M22 10v6M2 10l10-5 10 5-10 5z' } },
    { tag: 'path', attrs: { d: 'M6 12v5c3 3 9 3 12 0v-5' } },
  ],
  score: [
    { tag: 'path', attrs: { d: 'M16 4h2a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h2' } },
    { tag: 'rect', attrs: { x: '8', y: '2', width: '8', height: '4', rx: '1', ry: '1' } },
  ],
  attendance: [
    { tag: 'path', attrs: { d: 'M9 11l3 3L22 4' } },
    { tag: 'path', attrs: { d: 'M21 12v7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11' } },
  ],
  myCourse: [
    { tag: 'path', attrs: { d: 'M4 19.5A2.5 2.5 0 0 1 6.5 17H20' } },
    { tag: 'path', attrs: { d: 'M6.5 2H20v20H6.5A2.5 2.5 0 0 1 4 19.5v-15A2.5 2.5 0 0 1 6.5 2z' } },
    { tag: 'path', attrs: { d: 'M9 9h6M9 13h6M9 17h4' } },
  ],
  schedule: [
    { tag: 'rect', attrs: { x: '3', y: '4', width: '18', height: '18', rx: '2', ry: '2' } },
    { tag: 'line', attrs: { x1: '16', y1: '2', x2: '16', y2: '6' } },
    { tag: 'line', attrs: { x1: '8', y1: '2', x2: '8', y2: '6' } },
    { tag: 'line', attrs: { x1: '3', y1: '10', x2: '21', y2: '10' } },
    { tag: 'path', attrs: { d: 'M8 14h.01M12 14h.01M16 14h.01M8 18h.01M12 18h.01' } },
  ],
  history: [
    { tag: 'line', attrs: { x1: '18', y1: '20', x2: '18', y2: '10' } },
    { tag: 'line', attrs: { x1: '12', y1: '20', x2: '12', y2: '4' } },
    { tag: 'line', attrs: { x1: '6', y1: '20', x2: '6', y2: '14' } },
  ],
}

function iconNodes(name) {
  return ICONS[name] || ICONS.course
}

const onAvatarError = (e) => {
  e.target.src = props.config.placeholder
}

const onAvatarUpdated = (url) => {
  userStore.updateAvatar(url)
}

const handleLogout = async () => {
  try { await fetch('/api/auth/logout', { method: 'POST', credentials: 'include' }) } catch { /* 静默 */ }
  localStorage.removeItem('user')
  userStore.reset()
  router.push('/login')
}

onMounted(() => {
  userStore.initFromLocalStorage()
  userStore.ensureProfile().catch(() => {})
  document.addEventListener('keydown', onKeydown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', onKeydown)
})

function onKeydown(e) {
  if (e.key === 'Escape' && sidebarOpen.value) {
    sidebarOpen.value = false
  }
}

watch(sidebarOpen, (open) => {
  document.body.style.overflow = open ? 'hidden' : ''
})
</script>

<style scoped>
.layout-root {
  display: flex;
  min-height: 100vh;
  background: var(--color-bg);
}

.sidebar {
  width: 240px;
  background: var(--color-primary);
  display: flex;
  flex-direction: column;
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  z-index: 200;
  overflow-y: auto;
  transition: transform var(--duration-normal) var(--ease-out);
}

.sidebar-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 20px var(--space-lg);
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.brand-title {
  display: block;
  font-size: 0.95rem;
  font-weight: 700;
  color: #fff;
  line-height: 1.2;
}

.brand-sub {
  display: block;
  font-size: 0.7rem;
  color: rgba(255, 255, 255, 0.45);
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: var(--space-lg);
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.avatar-wrapper {
  position: relative;
  width: 42px;
  height: 42px;
  border-radius: 50%;
  overflow: hidden;
  border: 2px solid #444f66;
  cursor: pointer;
  flex-shrink: 0;
  transition: all var(--duration-fast) var(--ease-out);
}

.avatar-wrapper:hover {
  transform: scale(1.1);
  border-color: var(--color-accent);
  box-shadow: 0 0 8px rgba(200, 141, 44, 0.3);
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.avatar-overlay {
  position: absolute;
  inset: 0;
  background: transparent;
  transition: background var(--duration-fast) var(--ease-out);
}

.avatar-wrapper:hover .avatar-overlay {
  background: rgba(255, 255, 255, 0.1);
}

.user-detail { min-width: 0; }

.user-name {
  font-size: 0.9rem;
  font-weight: 600;
  color: #fff;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-role {
  font-size: 0.72rem;
  color: #b0b9c8;
  margin-top: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.sidebar-nav {
  flex: 1;
  padding: var(--space-sm) 0;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 11px 20px;
  margin: 2px 8px;
  border-radius: var(--radius-sm);
  color: rgba(255, 255, 255, 0.65);
  text-decoration: none;
  font-size: 0.88rem;
  font-weight: 500;
  transition: all var(--duration-fast) var(--ease-out);
}

.nav-item:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.06);
}

.nav-item.active {
  color: #fff;
  background: rgba(255, 255, 255, 0.1);
  font-weight: 600;
}

.nav-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  flex-shrink: 0;
}

.nav-svg {
  width: 20px;
  height: 20px;
  stroke: currentColor;
  stroke-width: 1.5;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.sidebar-footer {
  padding: var(--space-md);
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.logout-btn {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 10px 16px;
  border: none;
  border-radius: var(--radius-sm);
  background: rgba(255, 255, 255, 0.06);
  color: rgba(255, 255, 255, 0.6);
  font-size: 0.84rem;
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out);
}

.logout-btn:hover {
  background: rgba(166, 27, 46, 0.25);
  color: #f87171;
}

.main-area {
  flex: 1;
  margin-left: 240px;
  padding: var(--space-lg);
  min-height: 100vh;
}

.mobile-topbar {
  display: none;
  align-items: center;
  gap: var(--space-sm);
  padding: 10px 0;
  margin-bottom: var(--space-md);
}

.hamburger {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: var(--radius-sm);
  background: var(--color-surface);
  color: var(--color-text);
  cursor: pointer;
  box-shadow: var(--shadow-sm);
}

.topbar-title {
  font-size: 0.95rem;
  font-weight: 700;
  color: var(--color-text);
}

.sidebar-overlay {
  display: none;
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.35);
  z-index: 150;
  animation: fadeIn var(--duration-fast) var(--ease-out);
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@media (max-width: 768px) {
  .sidebar {
    transform: translateX(-100%);
  }

  .sidebar.open {
    transform: translateX(0);
  }

  .sidebar-overlay {
    display: block;
  }

  .main-area {
    margin-left: 0;
    padding: var(--space-md);
  }

  .mobile-topbar {
    display: flex;
  }
}
</style>
