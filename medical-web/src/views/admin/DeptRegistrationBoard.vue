<template>
  <div class="dept-list-page registration-board-page">
    <div class="page-header">
      <div class="header-left">
        <i class="fa-solid fa-chart-column page-icon"></i>
        <div>
          <h2 class="page-title">科室挂号看板</h2>
          <p class="page-desc">
            按科室统计挂号量、配置人数预警，并结合主诉/诊断关键词给出流感、水痘等趋势提示（辅助决策，非正式疫情报告）。
          </p>
        </div>
      </div>
    </div>

    <div v-if="alerts.length" class="alert-banner-wrap">
      <el-alert
        v-for="(item, idx) in alerts"
        :key="'alert-' + idx"
        :title="item.ruleName || '挂号预警'"
        type="error"
        :closable="false"
        show-icon
        class="alert-item alert-item-danger"
      >
        <template #icon>
          <i class="fa-solid fa-triangle-exclamation alert-danger-icon"></i>
        </template>
        <template #default>{{ item.message }}</template>
      </el-alert>
    </div>

    <el-tabs v-model="activeTab" class="board-tabs">
      <el-tab-pane label="看板概览" name="overview">
        <div class="content-card">
          <div class="toolbar">
            <div class="search-wrap">
              <el-date-picker
                v-model="dateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                value-format="YYYY-MM-DD"
                size="large"
                class="date-range-picker"
                @change="loadBoard"
              />
              <el-button class="search-btn" @click="loadBoard">
                <i class="fa-solid fa-rotate"></i>
                刷新
              </el-button>
            </div>
          </div>

          <div v-if="epidemicHints.length" class="hint-section">
            <div class="section-title">
              <i class="fa-solid fa-virus-covid"></i>
              疾病趋势提示
            </div>
            <div class="hint-list">
              <div
                v-for="(hint, idx) in epidemicHints"
                :key="'hint-' + idx"
                :class="['hint-card', 'confidence-' + (hint.confidence || 1)]"
              >
                <div class="hint-head">
                  <span class="hint-dept">{{ hint.deptName }}</span>
                  <el-tag size="small" :type="hintTagType(hint.confidence)">{{ hint.hintTypeLabel }}</el-tag>
                  <span class="hint-confidence">置信度：{{ hint.confidenceLabel }}</span>
                </div>
                <p class="hint-message">{{ hint.message }}</p>
              </div>
            </div>
          </div>

          <div class="table-wrap" v-loading="loading" element-loading-text="加载中...">
            <el-table
              :data="deptStats"
              class="data-table"
              :header-cell-style="headerCellStyle"
              :default-sort="{ prop: 'todayCount', order: 'descending' }"
              highlight-current-row
              @current-change="handleDeptSelect"
            >
              <el-table-column prop="deptName" label="科室" min-width="130">
                <template #default="{ row }">
                  <span class="dept-name">{{ row.deptName }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="todayCount" label="今日挂号" width="110" align="center" sortable>
                <template #default="{ row }">
                  <span :class="['count-num', row.todayCount >= 30 ? 'high' : '']">{{ row.todayCount ?? 0 }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="tomorrowCount" label="明日预约" width="110" align="center" sortable>
                <template #default="{ row }">
                  <span class="count-num future">{{ row.tomorrowCount ?? 0 }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="dayAfterTomorrowCount" label="后日预约" width="110" align="center" sortable>
                <template #default="{ row }">
                  <span class="count-num future">{{ row.dayAfterTomorrowCount ?? 0 }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="periodCount" width="120" align="center">
                <template #header>
                  <el-tooltip :content="periodRangeTip" placement="top">
                    <span class="column-header-tip">所选区间合计</span>
                  </el-tooltip>
                </template>
              </el-table-column>
              <el-table-column prop="avg7Days" label="近7日均值" width="110" align="center">
                <template #default="{ row }">
                  {{ formatDecimal(row.avg7Days) }}
                </template>
              </el-table-column>
              <el-table-column label="较7日变化" width="120" align="center">
                <template #header>
                  <el-tooltip content="今日挂号较近7日（不含今日）日均值的变化；样本不足3天时显示为 —" placement="top">
                    <span class="column-header-tip">较7日变化</span>
                  </el-tooltip>
                </template>
                <template #default="{ row }">
                  <span :class="changeClass(row.changePercent)">
                    {{ formatChange(row.changePercent) }}
                  </span>
                </template>
              </el-table-column>
              <el-table-column min-width="160">
                <template #header>
                  <el-tooltip content="该科室今日挂号占当前表格中最高值的相对比例，非全院占比" placement="top">
                    <span class="column-header-tip">今日热度</span>
                  </el-tooltip>
                </template>
                <template #default="{ row }">
                  <div class="bar-wrap">
                    <div
                      class="bar-fill"
                      :style="{ width: barWidth(row.todayCount) + '%' }"
                    ></div>
                    <span class="bar-label">{{ barWidth(row.todayCount).toFixed(0) }}%</span>
                  </div>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <div v-if="selectedDept" class="trend-section">
            <div class="section-title">
              <i class="fa-solid fa-chart-line"></i>
              {{ selectedDept.deptName }} — 近 {{ trendDays }} 日 + 未来 {{ futureDays }} 日预约趋势
            </div>
            <div class="trend-legend">
              <span class="legend-item"><i class="legend-dot past"></i>历史</span>
              <span class="legend-item"><i class="legend-dot today"></i>今日</span>
              <span class="legend-item"><i class="legend-dot future"></i>未来预约</span>
            </div>
            <div class="trend-chart" v-loading="trendLoading">
              <template v-for="(point, idx) in trendData" :key="point.date">
                <div v-if="point.periodType === 'future' && trendData[idx - 1]?.periodType !== 'future'" class="trend-divider">
                  <span>未来</span>
                </div>
                <div
                  class="trend-bar-item"
                  :class="'period-' + (point.periodType || 'past')"
                >
                  <div class="trend-bar-col">
                    <div
                      class="trend-bar"
                      :class="'bar-' + (point.periodType || 'past')"
                      :style="{ height: trendBarHeight(point.count) + '%' }"
                      :title="`${point.date} (${point.periodTypeLabel || ''}): ${point.count} 例`"
                    ></div>
                  </div>
                  <span class="trend-date">{{ formatTrendDateLabel(point) }}</span>
                  <span class="trend-count">{{ point.count }}</span>
                </div>
              </template>
              <div v-if="!trendLoading && !trendData.length" class="trend-empty">暂无趋势数据</div>
            </div>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="预警规则" name="rules">
        <div class="content-card">
          <div class="toolbar">
            <div class="section-title inline-title">
              <i class="fa-solid fa-bell"></i>
              预警规则配置
            </div>
            <el-button class="add-user-btn" @click="openRuleCreate">
              <i class="fa-solid fa-plus"></i>
              新增规则
            </el-button>
          </div>

          <div class="table-wrap" v-loading="rulesLoading" element-loading-text="加载中...">
            <el-table :data="alertRules" class="data-table" :header-cell-style="headerCellStyle">
              <el-table-column prop="deptName" label="科室" min-width="120" />
              <el-table-column prop="ruleName" label="规则名称" min-width="140">
                <template #default="{ row }">{{ row.ruleName || '—' }}</template>
              </el-table-column>
              <el-table-column prop="thresholdTypeLabel" label="阈值类型" width="150" />
              <el-table-column prop="thresholdValue" label="阈值" width="90" align="center" />
              <el-table-column prop="alertLevelLabel" label="级别" width="90" align="center">
                <template #default="{ row }">
                  <el-tag size="small" :type="alertType(row.alertLevel)">{{ row.alertLevelLabel }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="enabled" label="状态" width="80" align="center">
                <template #default="{ row }">
                  <span :class="['status-dot', row.enabled === 1 ? 'enabled' : 'disabled']"></span>
                  {{ row.enabled === 1 ? '启用' : '停用' }}
                </template>
              </el-table-column>
              <el-table-column label="操作" width="140" align="center" fixed="right">
                <template #default="{ row }">
                  <el-button link type="primary" size="small" @click="openRuleEdit(row)">编辑</el-button>
                  <el-button link type="danger" size="small" @click="confirmDeleteRule(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-dialog
      v-model="ruleDialogVisible"
      :title="ruleIsEdit ? '编辑预警规则' : '新增预警规则'"
      width="520px"
      destroy-on-close
    >
      <el-form ref="ruleFormRef" :model="ruleForm" :rules="ruleRules" label-width="110px">
        <el-form-item label="科室" prop="deptId">
          <el-select v-model="ruleForm.deptId" placeholder="选择科室" filterable style="width: 100%">
            <el-option
              v-for="d in deptOptions"
              :key="d.deptId"
              :label="d.name"
              :value="d.deptId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="规则名称">
          <el-input v-model="ruleForm.ruleName" placeholder="如：儿科日挂号量预警" maxlength="100" />
        </el-form-item>
        <el-form-item label="阈值类型" prop="thresholdType">
          <el-select v-model="ruleForm.thresholdType" style="width: 100%">
            <el-option :value="1" label="今日挂号绝对值" />
            <el-option :value="2" label="较7日均值增幅(%)" />
            <el-option :value="3" label="明日预约绝对值" />
            <el-option :value="4" label="过去一周内预约合计" />
          </el-select>
        </el-form-item>
        <el-form-item label="阈值" prop="thresholdValue">
          <el-input-number v-model="ruleForm.thresholdValue" :min="0.01" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="预警级别" prop="alertLevel">
          <el-select v-model="ruleForm.alertLevel" style="width: 100%">
            <el-option :value="1" label="提示" />
            <el-option :value="2" label="警告" />
            <el-option :value="3" label="严重" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="ruleForm.enabled" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="ruleForm.remark" type="textarea" :rows="2" maxlength="500" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="ruleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="ruleSubmitting" @click="submitRule">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createDeptRegistrationAlertRule,
  deleteDeptRegistrationAlertRule,
  getDeptOptions,
  getDeptRegistrationBoard,
  getDeptRegistrationTrend,
  getDeptRegistrationAlertRules,
  updateDeptRegistrationAlertRule
} from '@/api/admin'

const loading = ref(false)
const rulesLoading = ref(false)
const trendLoading = ref(false)
const ruleSubmitting = ref(false)
const activeTab = ref('overview')
const dateRange = ref([])
const deptStats = ref([])
const alerts = ref([])
const epidemicHints = ref([])
const alertRules = ref([])
const deptOptions = ref([])
const trendData = ref([])
const trendDays = 14
const futureDays = 2
const selectedDept = ref(null)
const maxTodayCount = ref(1)

const ruleDialogVisible = ref(false)
const ruleIsEdit = ref(false)
const ruleFormRef = ref(null)
const editingRuleId = ref(null)
const ruleForm = ref({
  deptId: null,
  ruleName: '',
  thresholdType: 1,
  thresholdValue: 30,
  alertLevel: 2,
  enabled: 1,
  remark: ''
})

const ruleRules = {
  deptId: [{ required: true, message: '请选择科室', trigger: 'change' }],
  thresholdType: [{ required: true, message: '请选择阈值类型', trigger: 'change' }],
  thresholdValue: [{ required: true, message: '请填写阈值', trigger: 'blur' }],
  alertLevel: [{ required: true, message: '请选择预警级别', trigger: 'change' }]
}

const periodRangeTip = computed(() => {
  if (dateRange.value?.length === 2) {
    return `${dateRange.value[0]} 至 ${dateRange.value[1]} 内有效预约（未取消）合计`
  }
  return '当前日期选择器范围内有效预约（未取消）合计'
})

const headerCellStyle = () => ({
  background: 'rgba(250, 245, 240, 0.95)',
  color: '#4a3924',
  fontWeight: '600'
})

const formatDecimal = (val) => {
  const n = Number(val)
  if (Number.isNaN(n)) return '0.0'
  return n.toFixed(1)
}

const formatChange = (val) => {
  if (val == null || val === '') return '—'
  const n = Number(val)
  if (Number.isNaN(n)) return '—'
  const prefix = n > 0 ? '+' : ''
  return `${prefix}${n.toFixed(1)}%`
}

const changeClass = (val) => {
  const n = Number(val)
  if (Number.isNaN(n) || val == null) return 'change-neutral'
  if (n >= 30) return 'change-up-high'
  if (n > 0) return 'change-up'
  if (n < 0) return 'change-down'
  return 'change-neutral'
}

const alertType = (level) => {
  if (level === 3) return 'error'
  if (level === 2) return 'warning'
  return 'info'
}

const hintTagType = (confidence) => {
  if (confidence >= 3) return 'danger'
  if (confidence >= 2) return 'warning'
  return 'info'
}

const barWidth = (count) => {
  const max = maxTodayCount.value || 1
  return Math.min(100, ((Number(count) || 0) / max) * 100)
}

const trendBarHeight = (count) => {
  const max = Math.max(...trendData.value.map((p) => Number(p.count) || 0), 1)
  return Math.max(4, ((Number(count) || 0) / max) * 100)
}

const formatShortDate = (dateStr) => {
  if (!dateStr) return ''
  const parts = dateStr.split('-')
  return parts.length >= 3 ? `${parts[1]}/${parts[2]}` : dateStr
}

const formatTrendDateLabel = (point) => {
  if (!point?.date) return ''
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const target = new Date(point.date + 'T00:00:00')
  const diff = Math.round((target - today) / 86400000)
  if (diff === 0) return '今日'
  if (diff === 1) return '明日'
  if (diff === 2) return '后日'
  return formatShortDate(point.date)
}

const initDateRange = () => {
  const end = new Date()
  const start = new Date()
  start.setDate(end.getDate() - 6)
  const fmt = (d) => d.toISOString().slice(0, 10)
  dateRange.value = [fmt(start), fmt(end)]
}

const loadBoard = async () => {
  loading.value = true
  try {
    const params = {}
    if (dateRange.value?.length === 2) {
      params.from = dateRange.value[0]
      params.to = dateRange.value[1]
    }
    const res = await getDeptRegistrationBoard(params)
    deptStats.value = res?.deptStats || []
    alerts.value = res?.alerts || []
    epidemicHints.value = res?.epidemicHints || []
    maxTodayCount.value = Math.max(...deptStats.value.map((d) => Number(d.todayCount) || 0), 1)
    if (deptStats.value.length && !selectedDept.value) {
      selectedDept.value = deptStats.value[0]
      loadTrend(selectedDept.value.deptId)
    }
  } catch {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}

const loadTrend = async (deptId) => {
  if (!deptId) return
  trendLoading.value = true
  try {
    const res = await getDeptRegistrationTrend({ deptId, days: trendDays, futureDays })
    trendData.value = res || []
  } catch {
    trendData.value = []
  } finally {
    trendLoading.value = false
  }
}

const handleDeptSelect = (row) => {
  if (!row) return
  selectedDept.value = row
  loadTrend(row.deptId)
}

const loadRules = async () => {
  rulesLoading.value = true
  try {
    alertRules.value = await getDeptRegistrationAlertRules() || []
  } finally {
    rulesLoading.value = false
  }
}

const loadDeptOptions = async () => {
  try {
    deptOptions.value = await getDeptOptions() || []
  } catch {
    deptOptions.value = []
  }
}

const resetRuleForm = () => {
  ruleForm.value = {
    deptId: null,
    ruleName: '',
    thresholdType: 1,
    thresholdValue: 30,
    alertLevel: 2,
    enabled: 1,
    remark: ''
  }
}

const openRuleCreate = () => {
  ruleIsEdit.value = false
  editingRuleId.value = null
  resetRuleForm()
  ruleDialogVisible.value = true
}

const openRuleEdit = (row) => {
  ruleIsEdit.value = true
  editingRuleId.value = row.ruleId
  ruleForm.value = {
    deptId: row.deptId,
    ruleName: row.ruleName || '',
    thresholdType: row.thresholdType,
    thresholdValue: Number(row.thresholdValue),
    alertLevel: row.alertLevel,
    enabled: row.enabled ?? 1,
    remark: row.remark || ''
  }
  ruleDialogVisible.value = true
}

const submitRule = async () => {
  try {
    await ruleFormRef.value?.validate()
  } catch {
    return
  }
  ruleSubmitting.value = true
  try {
    const payload = { ...ruleForm.value }
    if (ruleIsEdit.value) {
      await updateDeptRegistrationAlertRule(editingRuleId.value, payload)
      ElMessage.success('规则已更新')
    } else {
      await createDeptRegistrationAlertRule(payload)
      ElMessage.success('规则已创建')
    }
    ruleDialogVisible.value = false
    await loadRules()
    await loadBoard()
  } catch {
    // 已提示
  } finally {
    ruleSubmitting.value = false
  }
}

const confirmDeleteRule = async (row) => {
  try {
    await ElMessageBox.confirm(`确定删除规则「${row.ruleName || row.deptName}」吗？`, '删除规则', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  try {
    await deleteDeptRegistrationAlertRule(row.ruleId)
    ElMessage.success('已删除')
    await loadRules()
    await loadBoard()
  } catch {
    // 已提示
  }
}

onMounted(async () => {
  initDateRange()
  await loadDeptOptions()
  await Promise.all([loadBoard(), loadRules()])
})
</script>

<style scoped src="@/assets/admin-management-page.css"></style>
<style scoped>
.registration-board-page .page-icon {
  background: linear-gradient(135deg, #6b9fd4, #4a7fb8);
  box-shadow: 0 4px 14px rgba(74, 127, 184, 0.35);
}

.alert-banner-wrap {
  margin-bottom: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.alert-item-danger :deep(.el-alert) {
  background: linear-gradient(135deg, rgba(255, 241, 240, 0.98), rgba(254, 226, 226, 0.95));
  border: 1px solid rgba(220, 68, 68, 0.55);
  border-left: 5px solid #dc4444;
  border-radius: 10px;
  box-shadow: 0 4px 14px rgba(184, 68, 68, 0.18);
}

.alert-item-danger :deep(.el-alert__title) {
  color: #a82a2a;
  font-weight: 700;
  font-size: 15px;
}

.alert-item-danger :deep(.el-alert__description),
.alert-item-danger :deep(.el-alert__content) {
  color: #8b2e2e;
  font-size: 13px;
  line-height: 1.55;
}

.alert-item-danger :deep(.el-alert__icon) {
  color: #dc4444;
  font-size: 22px;
  width: 22px;
  margin-right: 4px;
}

.alert-danger-icon {
  color: #dc4444;
  font-size: 22px;
  line-height: 1;
}

.board-tabs :deep(.el-tabs__header) {
  margin-bottom: 0;
}

.board-tabs :deep(.el-tabs__content) {
  padding-top: 16px;
}

.date-range-picker {
  width: 280px;
}

.hint-section,
.trend-section {
  padding: 16px 24px 24px;
}

.trend-legend {
  display: flex;
  gap: 16px;
  margin-bottom: 10px;
  font-size: 12px;
  color: #8f7a63;
}

.legend-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.legend-dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 2px;
}

.legend-dot.past {
  background: linear-gradient(180deg, #6b9fd4, #4a7fb8);
}

.legend-dot.today {
  background: linear-gradient(180deg, #e8a54b, #d48232);
}

.legend-dot.future {
  background: linear-gradient(180deg, #7ec89a, #5c9e76);
  border: 1px dashed #4a7fb8;
}

.trend-divider {
  align-self: stretch;
  display: flex;
  align-items: center;
  padding: 0 4px;
  color: #4a7fb8;
  font-size: 11px;
  writing-mode: vertical-rl;
  border-left: 2px dashed rgba(74, 127, 184, 0.45);
  margin: 0 2px;
}

.trend-bar-item.period-today .trend-count {
  color: #d48232;
}

.trend-bar-item.period-future .trend-count {
  color: #4a7fb8;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 700;
  color: #4a3924;
  margin-bottom: 12px;
}

.inline-title {
  margin-bottom: 0;
}

.hint-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 8px;
}

.hint-card {
  border-radius: 12px;
  padding: 12px 16px;
  border: 1px solid rgba(139, 90, 43, 0.15);
  background: rgba(255, 252, 248, 0.8);
}

.hint-card.confidence-3 {
  border-color: rgba(184, 92, 74, 0.35);
  background: rgba(255, 245, 242, 0.9);
}

.hint-card.confidence-2 {
  border-color: rgba(212, 130, 50, 0.35);
  background: rgba(255, 250, 242, 0.9);
}

.hint-head {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  margin-bottom: 6px;
}

.hint-dept {
  font-weight: 700;
  color: #4a3924;
}

.hint-confidence {
  font-size: 12px;
  color: #8f7a63;
}

.hint-message {
  margin: 0;
  font-size: 13px;
  color: #5c4a32;
  line-height: 1.5;
}

.dept-name {
  font-weight: 600;
  color: #4a3924;
}

.column-header-tip {
  cursor: help;
  border-bottom: 1px dashed rgba(139, 90, 43, 0.35);
}

.count-num.high {
  color: #b85c4a;
  font-weight: 700;
}

.count-num.future {
  color: #4a7fb8;
  font-weight: 600;
}

.change-up-high {
  color: #b85c4a;
  font-weight: 700;
}

.change-up {
  color: #d48232;
}

.change-down {
  color: #5c7c4a;
}

.change-neutral {
  color: #8f7a63;
}

.bar-wrap {
  position: relative;
  height: 18px;
  background: rgba(139, 90, 43, 0.08);
  border-radius: 9px;
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  background: linear-gradient(90deg, #e8a54b, #d48232);
  border-radius: 9px;
  transition: width 0.3s ease;
}

.bar-label {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 11px;
  color: #5c4a32;
}

.trend-chart {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  min-height: 180px;
  padding: 12px 0 8px;
  overflow-x: auto;
}

.trend-bar-item {
  flex: 1;
  min-width: 36px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.trend-bar-col {
  width: 100%;
  height: 120px;
  display: flex;
  align-items: flex-end;
  justify-content: center;
}

.trend-bar {
  width: 70%;
  min-height: 4px;
  border-radius: 4px 4px 0 0;
  transition: height 0.3s ease;
}

.trend-bar.bar-past {
  background: linear-gradient(180deg, #6b9fd4, #4a7fb8);
}

.trend-bar.bar-today {
  background: linear-gradient(180deg, #e8a54b, #d48232);
  box-shadow: 0 0 0 2px rgba(212, 130, 50, 0.25);
}

.trend-bar.bar-future {
  background: linear-gradient(180deg, #7ec89a, #5c9e76);
  border: 1px dashed rgba(74, 127, 184, 0.55);
  box-sizing: border-box;
}

.trend-date {
  font-size: 11px;
  color: #8f7a63;
}

.trend-count {
  font-size: 12px;
  font-weight: 600;
  color: #4a3924;
}

.trend-empty {
  width: 100%;
  text-align: center;
  color: #8f7a63;
  padding: 40px 0;
}
</style>
