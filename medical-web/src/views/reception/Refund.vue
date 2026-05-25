<template>
  <div class="refund-page">
    <div class="page-header">
      <div class="header-left">
        <i class="fa-solid fa-rotate-left page-icon"></i>
        <div>
          <h2 class="page-title">退费</h2>
          <p class="page-desc">对已支付且未就诊的挂号费等办理退费，并回滚关联业务状态</p>
        </div>
      </div>
    </div>

    <div class="content-card">
      <div class="toolbar">
        <el-input v-model="keyword" placeholder="流水号" clearable style="width: 220px" @keyup.enter="loadData" />
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          value-format="YYYY-MM-DD"
          range-separator="至"
          start-placeholder="开始"
          end-placeholder="结束"
          @change="loadData"
        />
        <el-button type="primary" @click="loadData">查询</el-button>
      </div>

      <el-table :data="tableData" v-loading="loading">
        <el-table-column prop="paymentNo" label="流水号" min-width="165" />
        <el-table-column prop="patientName" label="患者" width="90" />
        <el-table-column prop="bizTypeText" label="类型" width="90" />
        <el-table-column prop="bizNo" label="业务单号" min-width="150" />
        <el-table-column label="金额" width="90">
          <template #default="{ row }">¥{{ row.amount }}</template>
        </el-table-column>
        <el-table-column prop="payMethodText" label="支付方式" width="90" />
        <el-table-column prop="payTime" label="支付时间" width="165" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="danger" @click="openRefund(row)">退费</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          background
          @current-change="loadData"
        />
      </div>
    </div>

    <el-dialog v-model="refundVisible" title="办理退费" width="440px">
      <p class="refund-info">流水号：{{ currentRow?.paymentNo }}</p>
      <p class="refund-info">金额：¥{{ currentRow?.amount }}</p>
      <el-input v-model="refundReason" type="textarea" :rows="3" placeholder="退费原因（可选）" />
      <template #footer>
        <el-button @click="refundVisible = false">取消</el-button>
        <el-button type="danger" :loading="refunding" @click="confirmRefund">确认退费</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getPaymentPage, refundPayment } from '@/api/reception'

const loading = ref(false)
const refunding = ref(false)
const tableData = ref([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const keyword = ref('')
const dateRange = ref([])
const refundVisible = ref(false)
const currentRow = ref(null)
const refundReason = ref('')

const todayStr = () => {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

const loadData = async () => {
  loading.value = true
  try {
    const params = {
      current: currentPage.value,
      size: pageSize.value,
      status: 1,
      keyword: keyword.value || undefined
    }
    if (dateRange.value?.length === 2) {
      params.dateFrom = dateRange.value[0]
      params.dateTo = dateRange.value[1]
    } else {
      params.dateFrom = todayStr()
      params.dateTo = todayStr()
    }
    const res = await getPaymentPage(params)
    tableData.value = res?.list || []
    total.value = res?.total || 0
  } finally {
    loading.value = false
  }
}

const openRefund = (row) => {
  currentRow.value = row
  refundReason.value = ''
  refundVisible.value = true
}

const confirmRefund = async () => {
  if (!currentRow.value) return
  try {
    await ElMessageBox.confirm('退费后关联预约将变为未支付，确定继续？', '退费确认', { type: 'warning' })
    refunding.value = true
    await refundPayment({
      paymentId: currentRow.value.paymentId,
      reason: refundReason.value
    })
    ElMessage.success('退费成功')
    refundVisible.value = false
    loadData()
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  } finally {
    refunding.value = false
  }
}

onMounted(loadData)
</script>

<style scoped>
.refund-page {
  padding: 24px 28px 32px;
}
.page-header { margin-bottom: 20px; }
.header-left { display: flex; gap: 14px; align-items: flex-start; }
.page-icon { font-size: 28px; color: #ef4444; margin-top: 4px; }
.page-title { font-size: 22px; font-weight: 600; margin: 0; }
.page-desc { font-size: 14px; color: #64748b; margin: 6px 0 0; }
.content-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}
.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}
.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
.refund-info {
  margin: 0 0 8px;
  color: #475569;
}
</style>
