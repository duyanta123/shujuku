<template>
  <div class="page-container schedule-page">
    <div class="schedule-hero">
      <div class="hero-actions">
        <div v-if="cacheAge" class="cache-status" :title="'缓存时间：' + cacheAge">
          <span class="cache-dot" :class="{ expired: cacheExpired }"></span>
          <span>{{ cacheExpired ? '缓存已过期' : '已缓存' }}</span>
        </div>

        <div class="view-toggle" role="tablist" aria-label="课表视图">
          <button
            v-for="mode in viewModes"
            :key="mode.value"
            type="button"
            :class="['toggle-btn', { active: viewMode === mode.value }]"
            :aria-selected="viewMode === mode.value"
            @click="viewMode = mode.value"
          >
            {{ mode.label }}
          </button>
        </div>

        <el-button size="small" :loading="loading" @click="refreshSchedule">
          <template #icon>
            <svg width="14" height="14" viewBox="0 0 16 16" fill="none" aria-hidden="true">
              <path d="M2 8a6 6 0 0 1 10.47-4M14 8a6 6 0 0 1-10.47 4M2 2v4h4M14 14v-4h-4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </template>
          刷新
        </el-button>
      </div>
    </div>

    <div v-if="loading && scheduleCourses.length === 0" class="schedule-skeleton" aria-live="polite">
      <div v-for="item in 4" :key="item" class="skeleton-card"></div>
    </div>

    <div v-else-if="scheduleCourses.length === 0" class="empty-state schedule-empty">
      <span class="empty-visual" aria-hidden="true">
        <svg viewBox="0 0 64 64" fill="none">
          <rect x="12" y="14" width="40" height="38" rx="8" stroke="currentColor" stroke-width="3"/>
          <path d="M22 10v10M42 10v10M13 26h38" stroke="currentColor" stroke-width="3" stroke-linecap="round"/>
          <path d="M22 36h8M36 36h6M22 44h20" stroke="currentColor" stroke-width="3" stroke-linecap="round"/>
        </svg>
      </span>
      <p>暂无课程安排</p>
      <span class="empty-hint">完成选课后，这里会自动同步你的周课表。</span>
    </div>

    <template v-else>
      <div class="metric-grid">
        <div class="metric-card">
          <span class="metric-label">已选课程</span>
          <strong>{{ scheduleCourses.length }}</strong>
          <span class="metric-hint">门课程</span>
        </div>
        <div class="metric-card">
          <span class="metric-label">本周课次</span>
          <strong>{{ scheduledEntries.length }}</strong>
          <span class="metric-hint">个时间段</span>
        </div>
        <div class="metric-card">
          <span class="metric-label">今日安排</span>
          <strong>{{ todayEntries.length }}</strong>
          <span class="metric-hint">{{ todayName }}</span>
        </div>
        <div class="metric-card">
          <span class="metric-label">实验室</span>
          <strong>{{ labCount }}</strong>
          <span class="metric-hint">个地点</span>
        </div>
      </div>

      <div class="status-strip">
        <div class="status-item">
          <span class="status-dot done"></span>
          <span>{{ todayStatusCounts.completed }} 已结束</span>
        </div>
        <div class="status-item">
          <span class="status-dot active"></span>
          <span>{{ todayStatusCounts.current }} 进行中</span>
        </div>
        <div class="status-item">
          <span class="status-dot upcoming"></span>
          <span>{{ todayStatusCounts.upcoming }} 待开始</span>
        </div>
      </div>

      <section v-if="viewMode === 'day'" class="day-view" aria-label="日课表">
        <div class="day-selector">
          <button
            v-for="day in visibleDays"
            :key="day.index"
            type="button"
            :class="['day-btn', { active: selectedDay === day.index, today: day.isToday }]"
            @click="selectedDay = day.index"
          >
            <span>{{ day.name }}</span>
            <small>{{ day.isToday ? '今天' : getDayCourseCount(day.index) + ' 节' }}</small>
          </button>
        </div>

        <div class="day-board">
          <div class="board-heading">
            <div>
              <h3>{{ selectedDayName }}安排</h3>
              <p>{{ selectedDayEntries.length ? `共 ${selectedDayEntries.length} 个课程时间段` : '今天留给自习、实验准备或复盘。' }}</p>
            </div>
          </div>

          <div class="timeline">
            <div v-for="period in periodConfig" :key="period.label" class="timeline-row">
              <div class="timeline-time">
                <strong>{{ period.label }}</strong>
                <span>{{ period.time }}</span>
              </div>
              <div class="timeline-content">
                <button
                  v-for="entry in getCellEntries(selectedDay, period.start)"
                  :key="entry.entryId"
                  type="button"
                  class="session-card"
                  :class="entry.status"
                  :style="{ '--course-color': getCourseColor(entry.course.course_name) }"
                  @click="openDetail(entry.course)"
                >
                  <span class="course-bar"></span>
                  <span class="session-main">
                    <span class="session-title">{{ entry.course.course_name }}</span>
                    <span class="session-meta">
                      <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
                        <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" stroke="currentColor" stroke-width="1.7" stroke-linecap="round"/>
                        <circle cx="12" cy="7" r="4" stroke="currentColor" stroke-width="1.7"/>
                      </svg>
                      {{ entry.course.teacher_name || '-' }}
                    </span>
                    <span class="session-meta">
                      <svg viewBox="0 0 24 24" fill="none" aria-hidden="true">
                        <path d="M4 21V7l8-4 8 4v14M9 21v-7h6v7" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round"/>
                      </svg>
                      {{ entry.course.location || entry.course.lab_name || '-' }}
                    </span>
                  </span>
                  <span class="session-badge">{{ getStatusLabel(entry.status) }}</span>
                </button>

                <div v-if="getCellEntries(selectedDay, period.start).length === 0" class="slot-empty">
                  无课程安排
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section v-else class="week-view" aria-label="周课表">
        <div class="week-table">
          <div class="week-header">
            <div class="week-head-cell time-head">时间</div>
            <div
              v-for="day in visibleDays"
              :key="day.index"
              :class="['week-head-cell', { today: day.isToday }]"
            >
              <span>{{ day.name }}</span>
              <small>{{ day.isToday ? '今天' : getDayCourseCount(day.index) + ' 节' }}</small>
            </div>
          </div>

          <div class="week-body">
            <div v-for="period in periodConfig" :key="period.label" class="week-row">
              <div class="week-time">
                <strong>{{ period.label }}</strong>
                <span>{{ period.time }}</span>
              </div>
              <div
                v-for="day in visibleDays"
                :key="`${day.index}-${period.start}`"
                :class="['week-cell', { today: day.isToday, filled: getCellEntries(day.index, period.start).length > 0 }]"
              >
                <button
                  v-for="entry in getCellEntries(day.index, period.start)"
                  :key="entry.entryId"
                  type="button"
                  class="week-course"
                  :class="entry.status"
                  :style="{ '--course-color': getCourseColor(entry.course.course_name) }"
                  @click="openDetail(entry.course)"
                >
                  <span class="week-course-title">{{ entry.course.course_name }}</span>
                  <span class="week-course-line">{{ entry.course.teacher_name || '-' }}</span>
                  <span class="week-course-line">{{ entry.course.location || entry.course.lab_name || '-' }}</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      </section>
    </template>

    <el-dialog v-model="detailVisible" title="课程详情" width="520px" :close-on-click-modal="false">
      <template v-if="detailCourse">
        <div class="detail-header">
          <span class="detail-color" :style="{ backgroundColor: getCourseColor(detailCourse.course_name) }"></span>
          <div>
            <h3 class="detail-name">{{ detailCourse.course_name }}</h3>
            <p>{{ detailCourse.course_time || '暂未设置上课时间' }}</p>
          </div>
        </div>
        <div class="detail-grid">
          <div class="detail-item">
            <span class="detail-label">授课教师</span>
            <span class="detail-value">{{ detailCourse.teacher_name || '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">所属学院</span>
            <span class="detail-value">{{ detailCourse.college || '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">实验室</span>
            <span class="detail-value">{{ detailCourse.lab_name || '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">上课地点</span>
            <span class="detail-value">{{ detailCourse.location || '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">课程容量</span>
            <span class="detail-value">{{ detailCourse.selected_count ?? '-' }} / {{ detailCourse.max_count ?? '-' }}</span>
          </div>
          <div class="detail-item">
            <span class="detail-label">选课时间</span>
            <span class="detail-value">{{ detailCourse.select_time || '-' }}</span>
          </div>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getMyCourses } from '../../api/selection'
import { PERIOD_CONFIG, DAY_NAMES, parseCourseTime } from '../../utils/scheduleParser'
import { getScheduleCache, setScheduleCache, getCacheAge } from '../../utils/scheduleCache'
import { onScheduleUpdate } from '../../utils/scheduleEventBus'

const loading = ref(false)
const scheduleCourses = ref([])
const viewMode = ref('week')
const selectedDay = ref(new Date().getDay() || 7)
const detailVisible = ref(false)
const detailCourse = ref(null)
const cacheAge = ref('')
const cacheExpired = ref(false)
let syncTimer = null
let scheduleUnsub = null

const viewModes = [
  { label: '周视图', value: 'week' },
  { label: '日视图', value: 'day' }
]

const periodConfig = PERIOD_CONFIG

const visibleDays = computed(() => {
  const today = new Date().getDay() || 7
  return DAY_NAMES.slice(1, 8).map((name, i) => ({
    index: i + 1,
    name,
    isToday: i + 1 === today
  }))
})

const todayIndex = computed(() => new Date().getDay() || 7)
const todayName = computed(() => DAY_NAMES[todayIndex.value])
const selectedDayName = computed(() => DAY_NAMES[selectedDay.value] || '当日')

const courseColors = [
  '#c88d2c',
  '#2d5a7a',
  '#2d6a4f',
  '#8a5a2b',
  '#7462a8',
  '#b85c1a',
  '#4f6f8f',
  '#7a6240'
]

const scheduledEntries = computed(() => {
  const entries = []

  scheduleCourses.value.forEach((course) => {
    parseCourseTime(course.course_time).forEach((slot, index) => {
      const entryId = `${course.selection_id || course.id}-${slot.day}-${slot.startPeriod}-${index}`
      entries.push({
        entryId,
        course,
        slot,
        status: getEntryStatus(slot)
      })
    })
  })

  return entries.sort((a, b) => a.slot.day - b.slot.day || a.slot.startPeriod - b.slot.startPeriod)
})

const todayEntries = computed(() => scheduledEntries.value.filter(entry => entry.slot.day === todayIndex.value))
const selectedDayEntries = computed(() => scheduledEntries.value.filter(entry => entry.slot.day === selectedDay.value))

const labCount = computed(() => {
  const labs = new Set(
    scheduleCourses.value
      .map(course => course.lab_name || course.location)
      .filter(Boolean)
  )
  return labs.size
})

const todayStatusCounts = computed(() => {
  return todayEntries.value.reduce((counts, entry) => {
    counts[entry.status] += 1
    return counts
  }, { completed: 0, current: 0, upcoming: 0 })
})

function getCourseColor(courseName = '') {
  let hash = 0
  for (let i = 0; i < courseName.length; i++) {
    hash = courseName.charCodeAt(i) + ((hash << 5) - hash)
  }
  return courseColors[Math.abs(hash) % courseColors.length]
}

function getCellEntries(day, startPeriod) {
  return scheduledEntries.value.filter(entry => entry.slot.day === day && entry.slot.startPeriod === startPeriod)
}

function getDayCourseCount(day) {
  return scheduledEntries.value.filter(entry => entry.slot.day === day).length
}

function getStatusLabel(status) {
  const labels = {
    completed: '已结束',
    current: '进行中',
    upcoming: '待开始'
  }
  return labels[status] || '待开始'
}

function getEntryStatus(slot) {
  if (slot.day !== todayIndex.value) return 'upcoming'

  const period = PERIOD_CONFIG.find(item => item.start === slot.startPeriod)
  if (!period?.time) return 'upcoming'

  const [startText, endText] = period.time.split(' - ')
  const now = new Date()
  const currentMinutes = now.getHours() * 60 + now.getMinutes()
  const startMinutes = toMinutes(startText)
  const endMinutes = toMinutes(endText)

  if (currentMinutes > endMinutes) return 'completed'
  if (currentMinutes >= startMinutes && currentMinutes <= endMinutes) return 'current'
  return 'upcoming'
}

function toMinutes(timeText) {
  const [hours, minutes] = timeText.split(':').map(Number)
  return hours * 60 + minutes
}

function openDetail(course) {
  if (!course) return
  detailCourse.value = course
  detailVisible.value = true
}

async function loadSchedule(forceRefresh = false) {
  if (!forceRefresh) {
    const cached = getScheduleCache()
    if (cached.data && !cached.isExpired) {
      scheduleCourses.value = cached.data
      cacheAge.value = getCacheAge()
      cacheExpired.value = false
      return
    }
    if (cached.data && cached.isExpired) {
      scheduleCourses.value = cached.data
      cacheAge.value = getCacheAge()
      cacheExpired.value = true
    }
  }

  loading.value = true
  try {
    const result = await getMyCourses()
    if (result.success) {
      scheduleCourses.value = result.data
      setScheduleCache(result.data)
      cacheAge.value = getCacheAge()
      cacheExpired.value = false
    } else {
      ElMessage.error(result.message || '加载课表失败')
    }
  } catch {
    const cached = getScheduleCache()
    if (cached.data) {
      scheduleCourses.value = cached.data
      cacheAge.value = getCacheAge()
      cacheExpired.value = true
    } else {
      ElMessage.error('加载课表失败，请检查网络连接')
    }
  } finally {
    loading.value = false
  }
}

function refreshSchedule() {
  loadSchedule(true)
}

function onStorageChange(e) {
  if (e.key === 'schedule_bust') {
    loadSchedule(true)
  }
}

onMounted(() => {
  loadSchedule()
  scheduleUnsub = onScheduleUpdate(() => loadSchedule(true))
  window.addEventListener('storage', onStorageChange)
  syncTimer = setInterval(() => {
    const cached = getScheduleCache()
    if (cached.isExpired) {
      loadSchedule(true)
    }
  }, 5 * 60 * 1000)
})

onUnmounted(() => {
  window.removeEventListener('storage', onStorageChange)
  if (scheduleUnsub) scheduleUnsub()
  if (syncTimer) clearInterval(syncTimer)
})
</script>

<style scoped>
.schedule-page {
  display: flex;
  flex-direction: column;
  gap: var(--space-lg);
}

.schedule-hero {
  display: flex;
  align-items: flex-start;
  justify-content: flex-end;
  gap: var(--space-lg);
  padding-bottom: var(--space-lg);
  border-bottom: 1px solid var(--color-border-soft);
}

.hero-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: var(--space-sm);
  flex-wrap: wrap;
}

.cache-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  min-height: 32px;
  padding: 0 10px;
  border: 1px solid var(--color-border-soft);
  border-radius: var(--radius-sm);
  color: var(--color-text-muted);
  background: #faf9f7;
  font-size: 0.78rem;
}

.cache-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--color-success);
}

.cache-dot.expired {
  background: var(--color-warning);
}

.view-toggle {
  display: inline-flex;
  min-height: 34px;
  padding: 3px;
  border: 1px solid var(--color-border-soft);
  border-radius: var(--radius-sm);
  background: #faf9f7;
}

.toggle-btn {
  min-width: 72px;
  border: none;
  border-radius: 5px;
  background: transparent;
  color: var(--color-text-muted);
  font-size: 0.82rem;
  font-weight: 600;
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out);
}

.toggle-btn.active {
  background: var(--color-primary);
  color: var(--color-text-inverse);
  box-shadow: var(--shadow-sm);
}

.schedule-skeleton {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--space-md);
}

.skeleton-card {
  height: 132px;
  border-radius: var(--radius-md);
  background: linear-gradient(90deg, #f3f0eb 25%, #faf9f7 37%, #f3f0eb 63%);
  background-size: 400% 100%;
  animation: shimmer 1.2s ease-in-out infinite;
}

@keyframes shimmer {
  0% { background-position: 100% 0; }
  100% { background-position: 0 0; }
}

.schedule-empty {
  min-height: 360px;
}

.empty-visual {
  width: 64px;
  height: 64px;
  margin-bottom: var(--space-md);
  color: var(--color-accent);
  opacity: 0.75;
}

.empty-visual svg {
  width: 100%;
  height: 100%;
}

.empty-hint {
  margin-top: 4px;
  color: var(--color-text-muted);
  font-size: 0.85rem;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: var(--space-md);
}

.metric-card {
  padding: var(--space-md);
  border: 1px solid var(--color-border-soft);
  border-radius: var(--radius-md);
  background: #faf9f7;
}

.metric-label,
.metric-hint {
  display: block;
  color: var(--color-text-muted);
  font-size: 0.76rem;
}

.metric-card strong {
  display: block;
  margin: 4px 0;
  color: var(--color-primary);
  font-size: 1.55rem;
  line-height: 1.1;
}

.status-strip {
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  flex-wrap: wrap;
  padding: var(--space-sm) var(--space-md);
  border: 1px solid var(--color-border-soft);
  border-radius: var(--radius-md);
  background: var(--color-surface);
}

.status-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--color-text-soft);
  font-size: 0.82rem;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.status-dot.done {
  background: var(--color-success);
}

.status-dot.active {
  background: var(--color-info);
}

.status-dot.upcoming {
  background: var(--color-text-muted);
}

.day-view,
.week-view {
  animation: viewEnter var(--duration-normal) var(--ease-out);
}

@keyframes viewEnter {
  from {
    opacity: 0;
    transform: translateY(6px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.day-selector {
  display: grid;
  grid-template-columns: repeat(7, minmax(92px, 1fr));
  gap: var(--space-sm);
  margin-bottom: var(--space-md);
  overflow-x: auto;
  padding-bottom: 2px;
}

.day-btn {
  min-height: 58px;
  border: 1px solid var(--color-border-soft);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  color: var(--color-text-soft);
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out);
}

.day-btn span,
.day-btn small {
  display: block;
}

.day-btn span {
  font-weight: 700;
}

.day-btn small {
  margin-top: 2px;
  color: var(--color-text-muted);
  font-size: 0.72rem;
}

.day-btn:hover {
  border-color: var(--color-border);
  box-shadow: var(--shadow-sm);
}

.day-btn.active {
  border-color: var(--color-primary);
  background: var(--color-primary);
  color: var(--color-text-inverse);
  box-shadow: var(--shadow-md);
}

.day-btn.active small {
  color: rgba(240, 236, 230, 0.72);
}

.day-btn.today:not(.active) {
  border-color: rgba(200, 141, 44, 0.45);
}

.day-board,
.week-table {
  border: 1px solid var(--color-border-soft);
  border-radius: var(--radius-lg);
  overflow: hidden;
  background: var(--color-surface);
}

.board-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-md);
  padding: var(--space-lg);
  border-bottom: 1px solid var(--color-border-soft);
  background: #faf9f7;
}

.board-heading h3 {
  margin-bottom: 4px;
  font-size: 1.05rem;
}

.board-heading p {
  color: var(--color-text-muted);
  font-size: 0.84rem;
}

.timeline-row {
  display: grid;
  grid-template-columns: 132px minmax(0, 1fr);
  border-bottom: 1px solid var(--color-border-soft);
}

.timeline-row:last-child {
  border-bottom: none;
}

.timeline-time {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 4px;
  padding: var(--space-md);
  border-right: 1px solid var(--color-border-soft);
  background: #faf9f7;
}

.timeline-time strong,
.week-time strong {
  color: var(--color-text);
  font-size: 0.88rem;
}

.timeline-time span,
.week-time span {
  color: var(--color-text-muted);
  font-size: 0.72rem;
}

.timeline-content {
  display: flex;
  flex-direction: column;
  gap: var(--space-sm);
  min-height: 112px;
  padding: var(--space-md);
}

.session-card {
  display: grid;
  grid-template-columns: 4px minmax(0, 1fr) auto;
  align-items: center;
  gap: var(--space-md);
  width: 100%;
  padding: var(--space-md);
  border: 1px solid var(--color-border-soft);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  text-align: left;
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out);
}

.session-card:hover {
  border-color: var(--color-border);
  box-shadow: var(--shadow-md);
  transform: translateY(-1px);
}

.course-bar {
  width: 4px;
  height: 54px;
  border-radius: 999px;
  background: var(--course-color);
}

.session-main {
  min-width: 0;
}

.session-title,
.week-course-title {
  display: block;
  overflow: hidden;
  color: var(--color-text);
  font-size: 0.98rem;
  font-weight: 700;
  line-height: 1.35;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 5px;
  min-width: 0;
  color: var(--color-text-soft);
  font-size: 0.8rem;
}

.session-meta svg {
  width: 14px;
  height: 14px;
  flex: 0 0 auto;
  color: var(--course-color);
}

.session-badge {
  padding: 3px 9px;
  border-radius: 999px;
  background: #f3f0eb;
  color: var(--color-text-soft);
  font-size: 0.72rem;
  font-weight: 700;
  white-space: nowrap;
}

.session-card.current .session-badge,
.week-course.current {
  background: rgba(45, 90, 122, 0.12);
}

.session-card.completed {
  opacity: 0.72;
}

.slot-empty {
  display: flex;
  align-items: center;
  min-height: 80px;
  padding: 0 var(--space-md);
  border: 1px dashed var(--color-border-soft);
  border-radius: var(--radius-md);
  color: var(--color-text-muted);
  font-size: 0.84rem;
}

.week-view {
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}

.week-table {
  min-width: 900px;
}

.week-header,
.week-row {
  display: grid;
  grid-template-columns: 112px repeat(7, minmax(104px, 1fr));
}

.week-header {
  background: var(--color-primary);
  color: var(--color-text-inverse);
}

.week-head-cell {
  min-height: 64px;
  padding: 12px 10px;
  border-right: 1px solid rgba(240, 236, 230, 0.14);
  text-align: center;
}

.week-head-cell:last-child {
  border-right: none;
}

.week-head-cell span,
.week-head-cell small {
  display: block;
}

.week-head-cell span {
  font-weight: 700;
}

.week-head-cell small {
  margin-top: 2px;
  color: rgba(240, 236, 230, 0.65);
  font-size: 0.7rem;
}

.week-head-cell.today {
  background: var(--color-accent);
}

.time-head {
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(240, 236, 230, 0.78);
  font-size: 0.8rem;
  font-weight: 700;
}

.week-row {
  min-height: 104px;
  border-bottom: 1px solid var(--color-border-soft);
}

.week-row:last-child {
  border-bottom: none;
}

.week-time {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 4px;
  padding: 10px;
  border-right: 1px solid var(--color-border-soft);
  background: #faf9f7;
  text-align: center;
}

.week-cell {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
  padding: 8px;
  border-right: 1px solid var(--color-border-soft);
  transition: background var(--duration-fast) var(--ease-out);
}

.week-cell:last-child {
  border-right: none;
}

.week-cell.today {
  background: rgba(200, 141, 44, 0.04);
}

.week-cell.filled:hover {
  background: rgba(45, 63, 102, 0.04);
}

.week-course {
  width: 100%;
  min-height: 72px;
  padding: 8px 9px;
  border: 1px solid rgba(26, 29, 40, 0.06);
  border-left: 4px solid var(--course-color);
  border-radius: var(--radius-sm);
  background: color-mix(in srgb, var(--course-color) 13%, white);
  text-align: left;
  cursor: pointer;
  transition: all var(--duration-fast) var(--ease-out);
}

.week-course:hover {
  box-shadow: var(--shadow-sm);
  transform: translateY(-1px);
}

.week-course-title {
  font-size: 0.78rem;
}

.week-course-line {
  display: block;
  overflow: hidden;
  margin-top: 3px;
  color: var(--color-text-soft);
  font-size: 0.68rem;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.detail-header {
  display: flex;
  align-items: flex-start;
  gap: var(--space-md);
  margin-bottom: var(--space-lg);
}

.detail-color {
  width: 6px;
  height: 52px;
  border-radius: 999px;
  flex: 0 0 auto;
}

.detail-name {
  margin-bottom: 4px;
  font-size: 1.15rem;
}

.detail-header p {
  color: var(--color-text-muted);
  font-size: 0.84rem;
}

.detail-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: 3px;
  padding: 12px;
  border: 1px solid var(--color-border-soft);
  border-radius: var(--radius-sm);
  background: #faf9f7;
}

.detail-label {
  color: var(--color-text-muted);
  font-size: 0.76rem;
}

.detail-value {
  color: var(--color-text);
  font-size: 0.9rem;
  font-weight: 600;
}

@media (max-width: 1024px) {
  .hero-actions {
    justify-content: flex-start;
    width: 100%;
  }

  .metric-grid,
  .schedule-skeleton {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .schedule-page {
    gap: var(--space-md);
  }

  .metric-grid {
    grid-template-columns: 1fr 1fr;
    gap: var(--space-sm);
  }

  .metric-card {
    padding: 12px;
  }

  .day-selector {
    grid-template-columns: repeat(7, minmax(76px, 1fr));
  }

  .timeline-row {
    grid-template-columns: 84px minmax(0, 1fr);
  }

  .timeline-time,
  .timeline-content,
  .board-heading {
    padding: var(--space-sm);
  }

  .session-card {
    grid-template-columns: 4px minmax(0, 1fr);
    gap: var(--space-sm);
  }

  .session-badge {
    grid-column: 2;
    justify-self: flex-start;
  }

  .detail-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 520px) {
  .hero-actions,
  .status-strip {
    align-items: stretch;
    flex-direction: column;
  }

  .view-toggle,
  .hero-actions .el-button {
    width: 100%;
  }

  .toggle-btn {
    flex: 1;
    min-height: 34px;
  }

  .metric-grid,
  .schedule-skeleton {
    grid-template-columns: 1fr;
  }

  .timeline-row {
    grid-template-columns: 1fr;
  }

  .timeline-time {
    border-right: none;
    border-bottom: 1px solid var(--color-border-soft);
  }
}

@media (prefers-reduced-motion: reduce) {
  .day-view,
  .week-view,
  .skeleton-card,
  .session-card,
  .week-course,
  .toggle-btn {
    animation: none;
    transition: none;
  }
}
</style>
