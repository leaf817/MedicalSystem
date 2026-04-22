<template>
  <div class="page">
    <div class="page-header">
      <div class="header-left">
        <i class="fa-solid fa-circle-check page-icon"></i>
        <div>
          <h2 class="page-title">发药确认</h2>
          <p class="page-desc">查看已发药处方记录，支持按处方号检索与详情查看。</p>
        </div>
      </div>
    </div>

    <div class="content-card">
      <div class="toolbar">
        <el-input
          v-model="keyword"
          class="search-input"
          clearable
          placeholder="处方号搜索"
          @clear="loadData"
          @keyup.enter="loadData"
        />
        <el-button type="primary" @click="loadData">查询</el-button>
      </div>

      <el-table :data="tableData" v-loading="loading" class="data-table">
        <el-table-column prop="prescriptionNo" label="处方号" min-width="170" />
        <el-table-column prop="patientName" label="患者" width="110" />
        <el-table-column prop="doctorName" label="开方医生" min-width="130" />
        <el-table-column prop="totalAmount" label="金额" width="100">
          <template #default="{ row }">¥{{ row.totalAmount ?? '-' }}</template>
        </el-table-column>
        <el-table-column prop="updatedTime" label="发药时间" min-width="165" />
        <el-table-column prop="statusText" label="状态" width="90" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="detailVisible" title="处方详情" width="760px">
      <el-descriptions :column="3" border>
        <el-descriptions-item label="处方号">{{ detail.prescriptionNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="患者">{{ detail.patientName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="开方医生">{{ detail.doctorName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="总金额">¥{{ detail.totalAmount ?? '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail.statusText || '-' }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ detail.updatedTime || '-' }}</el-descriptions-item>
      </el-descriptions>
      <el-table :data="detail.details || []" style="margin-top: 12px">
        <el-table-column prop="medicineName" label="药品" min-width="140" />
        <el-table-column prop="spec" label="规格" min-width="130" />
        <el-table-column prop="dosage" label="用法用量" min-width="140" />
        <el-table-column prop="quantity" label="数量" width="70" />
        <el-table-column prop="unitPrice" label="单价" width="90">
          <template #default="{ row }">¥{{ row.unitPrice ?? '-' }}</template>
        </el-table-column>
        <el-table-column prop="amount" label="小计" width="90">
          <template #default="{ row }">¥{{ row.amount ?? '-' }}</template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { getDispensedPrescriptions, getNursePrescriptionDetail } from '@/api/admin'

const loading = ref(false)
const keyword = ref('')
const tableData = ref([])
const detailVisible = ref(false)
const detail = ref({})

const loadData = async () => {
  loading.value = true
  try {
    const res = await getDispensedPrescriptions(keyword.value || undefined)
    tableData.value = Array.isArray(res) ? res : []
  } finally {
    loading.value = false
  }
}

const openDetail = async (row) => {
  const res = await getNursePrescriptionDetail(row.prescriptionId)
  detail.value = res || {}
  detailVisible.value = true
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.page { padding: 24px 28px 32px; min-height: 100%; }
.page-header { margin-bottom: 20px; }
.header-left { display: flex; align-items: center; gap: 14px; }
.page-icon {
  width: 48px; height: 48px; line-height: 48px; text-align: center; font-size: 22px; color: #fff;
  background: linear-gradient(135deg, #2f8f5b, #227148); border-radius: 12px;
}
.page-title { margin: 0; font-size: 20px; font-weight: 700; color: #2c1810; }
.page-desc { margin: 4px 0 0; font-size: 13px; color: #5c4a32; }
.content-card {
  background: rgba(255,252,248,.95); border-radius: 16px; padding: 20px 22px 24px;
  box-shadow: 0 8px 40px rgba(61,41,20,.08), 0 0 0 1px rgba(139,90,43,.08);
}
.toolbar { display: flex; gap: 10px; margin-bottom: 16px; align-items: center; }
.search-input { width: 220px; }
</style>
