<template>
  <div class="payment-mgmt-page">
    <div class="page-header">
      <div class="header-left">
        <i class="fa-solid fa-receipt page-icon"></i>
        <div>
          <h2 class="page-title">收费查询</h2>
          <p class="page-desc">统一查询挂号费、处方费等支付与退费流水，支持多条件筛选与 CSV 导出</p>
        </div>
      </div>
    </div>

    <div class="content-card">
      <div class="toolbar">
        <el-input
          v-model="keyword"
          placeholder="流水号 / 患者姓名 / 手机号"
          clearable
          style="width: 240px"
          @keyup.enter="handleSearch"
        />
        <el-select v-model="bizType" clearable placeholder="业务类型" style="width: 130px">
          <el-option label="挂号费" value="APPOINTMENT" />
          <el-option label="处方费" value="PRESCRIPTION" />
        </el-select>
        <el-select v-model="status" clearable placeholder="状态" style="width: 120px">
          <el-option label="已支付" :value="1" />
          <el-option label="已退款" :value="2" />
        </el-select>
        <el-select v-model="payMethod" clearable placeholder="支付方式" style="width: 120px">
          <el-option label="现金" value="CASH" />
          <el-option label="微信" value="WECHAT" />
          <el-option label="支付宝" value="ALIPAY" />
          <el-option label="银行卡" value="CARD" />
          <el-option label="在线支付" value="ONLINE" />
        </el-select>
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
        />
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
        <el-button type="success" plain :loading="exporting" @click="handleExport">
          <i class="fa-solid fa-file-export"></i> 导出 CSV
        </el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" class="data-table" :header-cell-style="headerCellStyle">
        <el-table-column prop="paymentNo" label="流水号" min-width="165" />
        <el-table-column prop="patientName" label="患者" width="100" />
        <el-table-column prop="bizTypeText" label="类型" width="90" />
        <el-table-column prop="bizNo" label="业务单号" min-width="150" />
        <el-table-column label="金额" width="100">
          <template #default="{ row }">¥{{ row.amount }}</template>
        </el-table-column>
        <el-table-column prop="payMethodText" label="支付方式" width="100" />
        <el-table-column prop="operatorName" label="操作员" width="100">
          <template #default="{ row }">{{ row.operatorName || '—' }}</template>
        </el-table-column>
        <el-table-column prop="payTime" label="支付时间" width="165" />
        <el-table-column prop="statusText" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 2 ? 'info' : 'success'" size="small">{{ row.statusText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          background
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </div>

    <el-dialog v-model="detailVisible" title="收费详情" width="520px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="流水号" :span="2">{{ detail.paymentNo }}</el-descriptions-item>
        <el-descriptions-item label="患者">{{ detail.patientName }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ detail.bizTypeText }}</el-descriptions-item>
        <el-descriptions-item label="业务单号" :span="2">{{ detail.bizNo }}</el-descriptions-item>
        <el-descriptions-item label="金额">¥{{ detail.amount }}</el-descriptions-item>
        <el-descriptions-item label="支付方式">{{ detail.payMethodText }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail.statusText }}</el-descriptions-item>
        <el-descriptions-item label="操作员">{{ detail.operatorName || '—' }}</el-descriptions-item>
        <el-descriptions-item label="支付时间" :span="2">{{ detail.payTime }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detail.remark || '—' }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getAdminPaymentPage, getAdminPaymentDetail, exportAdminPaymentCsv } from '@/api/payment'

const loading = ref(false)
const exporting = ref(false)
const tableData = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const keyword = ref('')
const bizType = ref('')
const status = ref(null)
const payMethod = ref('')
const dateRange = ref([])
const detailVisible = ref(false)
const detail = reactive({})

const headerCellStyle = {
  background: 'rgba(139, 90, 43, 0.08)',
  color: '#5c4a32',
  fontWeight: '600'
}

const buildQueryParams = () => {
  const params = {
    keyword: keyword.value || undefined,
    bizType: bizType.value || undefined,
    status: status.value ?? undefined,
    payMethod: payMethod.value || undefined
  }
  if (dateRange.value?.length === 2) {
    params.dateFrom = dateRange.value[0]
    params.dateTo = dateRange.value[1]
  }
  return params
}

const loadData = async () => {
  loading.value = true
  try {
    const params = {
      current: currentPage.value,
      size: pageSize.value,
      ...buildQueryParams()
    }
    const res = await getAdminPaymentPage(params)
    tableData.value = res?.list || []
    total.value = res?.total || 0
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  loadData()
}

const handleReset = () => {
  keyword.value = ''
  bizType.value = ''
  status.value = null
  payMethod.value = ''
  dateRange.value = []
  currentPage.value = 1
  loadData()
}

const handleExport = async () => {
  exporting.value = true
  try {
    const res = await exportAdminPaymentCsv(buildQueryParams())
    const blob = new Blob([res.data], { type: 'text/csv;charset=utf-8;' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    const date = new Date().toISOString().slice(0, 10).replace(/-/g, '')
    link.href = url
    link.download = `payment_${date}.csv`
    link.click()
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch {
    ElMessage.error('导出失败')
  } finally {
    exporting.value = false
  }
}

const openDetail = async (row) => {
  const d = await getAdminPaymentDetail(row.paymentId)
  Object.assign(detail, d || {})
  detailVisible.value = true
}

onMounted(loadData)
</script>

<style scoped>
.payment-mgmt-page {
  padding: 24px 28px 32px;
}
.page-header { margin-bottom: 20px; }
.header-left { display: flex; gap: 14px; align-items: flex-start; }
.page-icon { font-size: 28px; color: #8b5a2b; margin-top: 4px; }
.page-title { font-size: 22px; font-weight: 600; margin: 0; color: #1e293b; }
.page-desc { font-size: 14px; color: #64748b; margin: 6px 0 0; }
.content-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}
.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
  align-items: center;
}
.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
