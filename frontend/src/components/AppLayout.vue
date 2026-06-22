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
          <span class="nav-icon" v-html="item.icon"></span>
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

const onAvatarError = (e) => {
  e.target.src = props.config.placeholder
}

const onAvatarUpdated = (url) => {
  userStore.updateAvatar(url)
}

const handleLogout = async () => {
  const BFF_ENABLED = import.meta.env.VITE_BFF_ENABLED !== 'false'
  if (BFF_ENABLED) {
    try { await fetch('/api/auth/logout', { method: 'POST', credentials: 'include' }) } catch { /* 静默 */ }
  }
  localStorage.removeItem('user')
  userStore.reset()
  router.push('/login')
}

onMounted(() => {
  userStore.initFromLocalStorage()
  userStore.fetchProfile()
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

.nav-icon :deep(svg) {
  width: 20px;
  height: 20px;
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
