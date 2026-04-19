<template>
  <div class="schedule-page">
    <div class="page-header">
      <div class="header-left">
        <i class="fa-solid fa-calendar-days page-icon"></i>
        <div>
          <h2 class="page-title">我的排班</h2>
          <p class="page-desc">管理我的门诊排班信息，患者端会实时读取这里的数据。</p>
        </div>
      </div>
    </div>

    <div class="content-card">
      <div class="toolbar">
        <div class="search-wrap">
          <i class="fa-solid fa-magnifying-glass search-icon"></i>
          <el-date-picker
              v-model="queryParams.startDate"
              type="date"
              placeholder="开始日期"
              value-format="YYYY-MM-DD"
              class="search-input"
              :prefix-icon="Calendar"
          />
          <span class="date-separator">至</span>
          <el-date-picker
              v-model="queryParams.endDate"
              type="date"
              placeholder="结束日期"
              value-format="YYYY-MM-DD"
              class="search-input"
              :prefix-icon="Calendar"
          />
          <el-select v-model="queryParams.status" placeholder="全部状态" clearable class="filter-select" style="width: 120px">
            <el-option :value="1" label="可预约" />
            <el-option :value="0" label="停诊" />
          </el-select>
          <el-button class="search-btn" @click="handleSearch">
            <i class="fa-solid fa-search"></i> 查询
          </el-button>
          <el-button class="reset-btn" @click="handleReset">
            <i class="fa-solid fa-rotate-right"></i> 重置
          </el-button>
        </div>
      </div>

      <div class="table-wrap" v-loading="loading" element-loading-text="加载中...">
        <el-table
            :data="tableData"
            class="data-table"
            :header-cell-style="headerCellStyle"
            :row-class-name="tableRowClassName"
            style="width: 100%"
        >
          <el-table-column prop="scheduleId" label="排班ID" align="center">
            <template #default="{ row }">
              <span class="cell-id">{{ row.scheduleId }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="scheduleDate" label="日期" align="center">
            <template #default="{ row }">
              <span class="cell-name">{{ row.scheduleDate }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="timeSlot" label="时段" align="center">
            <template #default="{ row }">
              <span class="cell-time">{{ row.timeSlot }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="totalSlots" label="总号源" align="center">
            <template #default="{ row }">
              <span class="cell-name">{{ row.totalSlots }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="bookedSlots" label="已预约" align="center">
            <template #default="{ row }">
              <span class="cell-name">{{ row.bookedSlots }}</span>
            </template>
          </el-table-column>
          <el-table-column label="剩余号源" align="center">
            <template #default="{ row }">
              <el-tag :type="(row.totalSlots - row.bookedSlots) === 0 ? 'danger' : 'success'" size="small">
                {{ row.totalSlots - row.bookedSlots }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" align="center">
            <template #default="{ row }">
              <span :class="['status-dot', row.status === 1 ? 'enabled' : 'disabled']"></span>
              <span class="status-text">{{ row.status === 1 ? '可预约' : '停诊' }}</span>
            </template>
          </el-table-column>
        </el-table>

        <div class="pagination-wrap">
          <el-pagination
              v-model:current-page="queryParams.pageNum"
              v-model:page-size="queryParams.pageSize"
              :total="total"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next, jumper"
              background
              @size-change="handleSizeChange"
              @current-change="handleCurrentChange"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Calendar } from '@element-plus/icons-vue'

const queryParams = reactive({
  startDate: null,
  endDate: null,
  status: null,
  pageNum: 1,
  pageSize: 10
})

const tableData = ref([])
const total = ref(0)
const loading = ref(false)
const allData = ref([])

const headerCellStyle = {
  background: 'rgba(139, 90, 43, 0.08)',
  color: '#5c4a32',
  fontWeight: '600',
  fontSize: '13px',
  borderBottom: '1px solid rgba(139, 90, 43, 0.15)'
}

const tableRowClassName = ({ rowIndex }) => {
  return rowIndex % 2 === 1 ? 'striped-row' : ''
}

const loadScheduleList = async () => {
  loading.value = true
  try {
    const token = localStorage.getItem('token')
    const response = await fetch('/api/admin/schedule/list?current=1&size=1000', {
      headers: { 'Authorization': 'Bearer ' + token }
    })
    const res = await response.json()

    if (res.code === 200) {
      allData.value = res.data?.list || []
      handleSearch()
    } else {
      ElMessage.error('加载失败')
    }
  } catch (error) {
    console.error('加载失败:', error)
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  let filtered = [...allData.value]
  if (queryParams.startDate) {
    filtered = filtered.filter(item => item.scheduleDate >= queryParams.startDate)
  }
  if (queryParams.endDate) {
    filtered = filtered.filter(item => item.scheduleDate <= queryParams.endDate)
  }
  if (queryParams.status !== null && queryParams.status !== '') {
    filtered = filtered.filter(item => item.status === queryParams.status)
  }

  total.value = filtered.length
  const start = (queryParams.pageNum - 1) * queryParams.pageSize
  const end = start + queryParams.pageSize

  tableData.value = filtered.slice(start, end).map(item => ({
    scheduleId: item.scheduleId,
    scheduleDate: item.scheduleDate,
    timeSlot: item.scheduleTimeSlot,
    totalSlots: item.totalSlots,
    bookedSlots: item.bookedSlots,
    status: item.status
  }))
}

const handleReset = () => {
  queryParams.startDate = null
  queryParams.endDate = null
  queryParams.status = null
  queryParams.pageNum = 1
  handleSearch()
}

const handleSizeChange = () => {
  queryParams.pageNum = 1
  handleSearch()
}

const handleCurrentChange = () => {
  handleSearch()
}

onMounted(() => {
  loadScheduleList()
})
</script>

<style scoped>
.schedule-page {
  padding: 24px 28px 32px;
  min-height: 100%;
}

.page-header {
  margin-bottom: 20px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.page-icon {
  width: 48px;
  height: 48px;
  line-height: 48px;
  text-align: center;
  font-size: 22px;
  color: #fff;
  background: linear-gradient(135deg, #e8a54b, #d48232);
  border-radius: 12px;
  box-shadow: 0 4px 14px rgba(212, 130, 50, 0.35);
}

.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: #2c1810;
  text-shadow: 0 1px 2px rgba(255, 255, 255, 0.8);
}

.page-desc {
  margin: 4px 0 0 0;
  font-size: 13px;
  color: #5c4a32;
}

.content-card {
  border-radius: 16px;
  background: rgba(255, 252, 250, 0.55);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: 0 4px 24px rgba(61, 41, 20, 0.1);
  overflow: hidden;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  padding: 18px 24px;
  border-bottom: 1px solid rgba(139, 90, 43, 0.1);
}

.search-wrap {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  max-width: 100%;
  flex: 1;
  min-width: 220px;
}

.search-icon {
  color: #8b5a2b;
  font-size: 16px;
  opacity: 0.8;
}

.search-input {
  width: 160px;
}

.search-input :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.7);
  border-radius: 10px;
  border: 1px solid rgba(139, 90, 43, 0.2);
  box-shadow: none;
}

.search-input :deep(.el-input__wrapper:hover),
.search-input :deep(.el-input__wrapper.is-focus) {
  border-color: rgba(232, 165, 75, 0.5);
  box-shadow: 0 0 0 1px rgba(232, 165, 75, 0.2);
}

.filter-select :deep(.el-select__wrapper) {
  border-radius: 10px !important;
  background: rgba(255, 255, 255, 0.7) !important;
  border: 1.5px solid rgba(100, 70, 40, 0.5) !important;
  box-shadow: none !important;
}

.filter-select :deep(.el-select__wrapper:hover),
.filter-select :deep(.el-select__wrapper.is-focus) {
  border-color: rgba(232, 165, 75, 0.7) !important;
  box-shadow: 0 0 0 2px rgba(232, 165, 75, 0.15) !important;
}

.date-separator {
  color: #b8a890;
  font-size: 14px;
}

.search-btn {
  background: linear-gradient(135deg, #e8a54b, #d48232);
  border: none;
  color: #fff;
  border-radius: 10px;
  padding: 10px 18px;
  font-weight: 600;
  box-shadow: 0 4px 14px rgba(212, 130, 50, 0.3);
}

.search-btn:hover {
  background: linear-gradient(135deg, #f0b55c, #e08d3a);
  color: #fff;
  box-shadow: 0 5px 18px rgba(212, 130, 50, 0.4);
}

.reset-btn {
  background: rgba(255, 255, 255, 0.7);
  border: 1px solid rgba(139, 90, 43, 0.3);
  color: #5c4a32;
  border-radius: 10px;
  padding: 10px 18px;
  font-weight: 600;
}

.reset-btn:hover {
  border-color: #d48232;
  color: #d48232;
  background: rgba(255, 255, 255, 0.9);
}

.table-wrap {
  padding: 0 24px 24px;
}

.data-table {
  --el-table-border-color: rgba(139, 90, 43, 0.12);
  --el-table-header-bg-color: transparent;
  background: transparent !important;
  width: 100% !important;
}

.data-table :deep(.el-table__inner-wrapper::before) {
  display: none;
}

.data-table :deep(.el-table th.el-table__cell) {
  padding: 14px 0;
}

.data-table :deep(.el-table td.el-table__cell) {
  padding: 12px 0;
  border-bottom: 1px solid rgba(139, 90, 43, 0.06);
}

.data-table :deep(.el-table__row:hover > td) {
  background: rgba(232, 165, 75, 0.08) !important;
}

.data-table :deep(.striped-row td) {
  background: rgba(255, 250, 245, 0.5) !important;
}

.data-table :deep(.striped-row:hover > td) {
  background: rgba(232, 165, 75, 0.08) !important;
}

/* 让所有列平均分配宽度 */
.data-table :deep(.el-table__header-wrapper table),
.data-table :deep(.el-table__body-wrapper table) {
  width: 100% !important;
  table-layout: fixed !important;
}

.data-table :deep(.el-table__header-wrapper col),
.data-table :deep(.el-table__body-wrapper col) {
  width: calc(100% / 7) !important;
}

.cell-id {
  font-weight: 600;
  color: #8b5a2b;
}

.cell-name {
  font-weight: 600;
  color: #2c1810;
}

.cell-time {
  color: #5c4a32;
  font-size: 13px;
}

.status-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  margin-right: 6px;
  vertical-align: middle;
}

.status-dot.enabled {
  background: #52c41a;
  box-shadow: 0 0 0 2px rgba(82, 196, 26, 0.3);
}

.status-dot.disabled {
  background: #ff4d4f;
  box-shadow: 0 0 0 2px rgba(255, 77, 79, 0.3);
}

.status-text {
  font-size: 13px;
  color: #5c4a32;
}

.pagination-wrap {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.pagination-wrap :deep(.el-pagination) {
  --el-pagination-button-bg-color: rgba(255, 255, 255, 0.8);
  --el-pagination-hover-color: #e8a54b;
}

.pagination-wrap :deep(.el-pagination.is-background .el-pager li:not(.is-disabled).is-active) {
  background: linear-gradient(135deg, #e8a54b, #d48232);
}

.pagination-wrap :deep(.el-pagination .el-pagination__total) {
  color: #5c4a32;
  font-weight: 500;
}
</style>