import { reactive } from 'vue'
import { getUserProfile } from '../api/user'

const userStore = reactive({
  name: '',
  account: '',
  avatarUrl: '',
  college: '',
  title: '',
  role: '',
  loaded: false,

  initFromLocalStorage() {
    try {
      const raw = JSON.parse(localStorage.getItem('user') || '{}')
      this.name = raw.name || ''
      this.account = raw.account || raw.username || raw.studentNo || raw.teacherNo || ''
      this.role = raw.role || ''
      this.title = raw.title || ''
      this.college = raw.college || ''
    } catch { /* ignore */ }
  },

  async fetchProfile() {
    try {
      const res = await getUserProfile()
      if (res.success || res.data) {
        const data = res.data || res
        this.name = data.name || this.name
        this.account = data.account || this.account
        this.avatarUrl = data.avatarUrl || ''
        this.college = data.college || this.college
        this.title = data.title || this.title
        this.role = data.role || this.role
        this.loaded = true

        // Sync to localStorage
        try {
          const raw = JSON.parse(localStorage.getItem('user') || '{}')
          raw.name = this.name
          raw.avatarUrl = this.avatarUrl
          localStorage.setItem('user', JSON.stringify(raw))
        } catch { /* ignore */ }
      }
    } catch {
      // Silent fail - use cached data
      this.loaded = true
    }
  },

  updateAvatar(url) {
    this.avatarUrl = url
    try {
      const raw = JSON.parse(localStorage.getItem('user') || '{}')
      raw.avatarUrl = url
      localStorage.setItem('user', JSON.stringify(raw))
    } catch { /* ignore */ }
  },

  reset() {
    this.name = ''
    this.account = ''
    this.avatarUrl = ''
    this.college = ''
    this.title = ''
    this.role = ''
    this.loaded = false
  }
})

export default userStore