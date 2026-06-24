import { reactive } from 'vue'
import { getUserProfile } from '../api/user'
import { readJsonStorage, writeJsonStorage } from '../utils/safeStorage'

const userStore = reactive({
  name: '',
  account: '',
  avatarUrl: '',
  college: '',
  title: '',
  role: '',
  loaded: false,
  profilePromise: null,

  initFromLocalStorage() {
    const raw = readJsonStorage('user', {})
    this.name = raw.name || ''
    this.account = raw.account || ''
    this.avatarUrl = raw.avatarUrl || ''
    this.college = raw.college || ''
    this.title = raw.title || ''
  },

  applyProfile(data = {}) {
    this.name = data.name || ''
    this.account = data.account || ''
    this.avatarUrl = data.avatarUrl || ''
    this.college = data.college || data.collegeName || ''
    this.title = data.title || ''
    this.role = data.role || ''
    this.loaded = true

    writeJsonStorage('user', {
      name: this.name,
      account: this.account,
      avatarUrl: this.avatarUrl,
      college: this.college,
      title: this.title,
    })
  },

  async fetchProfile(options = {}) {
    const res = await getUserProfile(options)
    const data = res.data || res
    if (!res.success && !res.data) {
      throw new Error(res.message || 'Failed to load profile')
    }
    this.applyProfile(data)
    return data
  },

  async ensureProfile(options = {}) {
    if (this.loaded && this.role) return this
    if (!this.profilePromise) {
      this.profilePromise = this.fetchProfile(options)
        .finally(() => {
          this.profilePromise = null
        })
    }
    await this.profilePromise
    return this
  },

  updateAvatar(url) {
    this.avatarUrl = url
    const raw = readJsonStorage('user', {})
    writeJsonStorage('user', { ...raw, avatarUrl: url })
  },

  reset() {
    this.name = ''
    this.account = ''
    this.avatarUrl = ''
    this.college = ''
    this.title = ''
    this.role = ''
    this.loaded = false
    this.profilePromise = null
  }
})

export default userStore
