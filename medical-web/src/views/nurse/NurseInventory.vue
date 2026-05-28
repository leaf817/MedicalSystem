<template>
  <div class="page">
    <div class="page-header">
      <div class="header-left">
        <i class="fa-solid fa-boxes-stacked page-icon"></i>
        <div>
          <h2 class="page-title">药品盘点</h2>
          <p class="page-desc">核对账面库存与实盘数量，支持盘点调整、入库及流水查询。</p>
        </div>
      </div>
    </div>

    <div class="content-card">
      <el-tabs v-model="activeTab" @tab-change="onTabChange">
        <el-tab-pane label="盘点录入" name="check">
          <div class="toolbar">
            <el-input
              v-model="keyword"
              class="search-input"
              clearable
              placeholder="药品名称/编码"
              @clear="loadMedicines"
              @keyup.enter="loadMedicines"
            />
            <el-select
              v-model="categoryId"
              placeholder="分类"
              clearable
              filterable
              class="filter-select"
              @change="loadMedicines"
            >
              <el-option
                v-for="c in categories"
                :key="c.categoryId"
                :label="c.name"
                :value="c.categoryId"
              />
            </el-select>
            <el-button type="primary" @click="loadMedicines">查询</el-button>
          </div>

          <el-table :data="medicineList" v-loading="medicineLoading" class="data-table">
            <el-table-column prop="medicineCode" label="编码" width="110" />
            <el-table-column prop="name" label="药品名称" min-width="130" />
            <el-table-column prop="spec" label="规格" min-width="100" />
            <el-table-column prop="unit" label="单位" width="70" />
            <el-table-column prop="stockQuantity" label="账面库存" width="100" align="center">
              <template #default="{ row }">
                <span :class="{ 'text-warning': isLowStock(row) }">{{ row.stockQuantity ?? 0 }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="minStock" label="最低库存" width="100" align="center" />
            <el-table-column label="实盘数量" width="130" align="center">
              <template #default="{ row }">
                <el-input-number
                  v-model="row._actualQty"
                  :min="0"
                  :max="999999"
                  size="small"
                  controls-position="right"
                />
              </template>
            </el-table-column>
            <el-table-column label="差异" width="80" align="center">
              <template #default="{ row }">
                <span :class="diffClass(row)">{{ formatDiff(row) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="200" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="submitAdjust(row)">盘点确认</el-button>
                <el-button link type="success" @click="openInbound(row)">入库</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-wrap">
            <el-pagination
              v-model:current-page="medicinePage"
              v-model:page-size="medicinePageSize"
              :total="medicineTotal"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next"
              background
              @size-change="loadMedicines"
              @current-change="loadMedicines"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane label="库存流水" name="log">
          <div class="toolbar">
            <el-input
              v-model="logKeyword"
              class="search-input"
              clearable
              placeholder="药品名称/编码"
              @clear="loadLogs"
              @keyup.enter="loadLogs"
            />
            <el-select v-model="logBizType" placeholder="业务类型" clearable @change="loadLogs">
              <el-option label="发药出库" value="DISPENSE" />
              <el-option label="盘点调整" value="ADJUST" />
              <el-option label="入库" value="INBOUND" />
            </el-select>
            <el-button type="primary" @click="loadLogs">查询</el-button>
          </div>

          <el-table :data="logList" v-loading="logLoading" class="data-table">
            <el-table-column prop="createdTime" label="时间" min-width="165" />
            <el-table-column prop="medicineName" label="药品" min-width="120" />
            <el-table-column prop="medicineCode" label="编码" width="110" />
            <el-table-column prop="bizTypeText" label="类型" width="100" />
            <el-table-column prop="changeQty" label="变动" width="80" align="center">
              <template #default="{ row }">
                <span :class="row.changeQty > 0 ? 'text-in' : 'text-out'">
                  {{ row.changeQty > 0 ? '+' : '' }}{{ row.changeQty }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="beforeQty" label="变动前" width="80" align="center" />
            <el-table-column prop="afterQty" label="变动后" width="80" align="center" />
            <el-table-column prop="operatorName" label="操作人" width="100" />
            <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
          </el-table>

          <div class="pagination-wrap">
            <el-pagination
              v-model:current-page="logPage"
              v-model:page-size="logPageSize"
              :total="logTotal"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next"
              background
              @size-change="loadLogs"
              @current-change="loadLogs"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <el-dialog v-model="adjustDialogVisible" title="盘点确认" width="480px">
      <el-form label-width="100px">
        <el-form-item label="药品">{{ adjustTarget?.name }}</el-form-item>
        <el-form-item label="账面库存">{{ adjustTarget?.stockQuantity ?? 0 }}</el-form-item>
        <el-form-item label="实盘数量">{{ adjustTarget?._actualQty }}</el-form-item>
        <el-form-item label="差异说明">
          <el-input v-model="adjustRemark" type="textarea" :rows="3" placeholder="可选，说明盘点差异原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="adjustDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="confirmAdjust">确认调整</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="inboundDialogVisible" title="药品入库" width="480px">
      <el-form label-width="100px">
        <el-form-item label="药品">{{ inboundTarget?.name }}</el-form-item>
        <el-form-item label="当前库存">{{ inboundTarget?.stockQuantity ?? 0 }}</el-form-item>
        <el-form-item label="入库数量" required>
          <el-input-number v-model="inboundQty" :min="1" :max="999999" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="inboundRemark" type="textarea" :rows="2" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="inboundDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="confirmInbound">确认入库</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  adjustInventoryStock,
  getInventoryMedicineCategories,
  getInventoryMedicinePage,
  getInventoryStockLogPage,
  inboundInventoryStock
} from '@/api/nurse'

const activeTab = ref('check')

const keyword = ref('')
const categoryId = ref(null)
const categories = ref([])
const medicineList = ref([])
const medicineLoading = ref(false)
const medicinePage = ref(1)
const medicinePageSize = ref(10)
const medicineTotal = ref(0)

const logKeyword = ref('')
const logBizType = ref('')
const logList = ref([])
const logLoading = ref(false)
const logPage = ref(1)
const logPageSize = ref(10)
const logTotal = ref(0)

const adjustDialogVisible = ref(false)
const adjustTarget = ref(null)
const adjustRemark = ref('')
const inboundDialogVisible = ref(false)
const inboundTarget = ref(null)
const inboundQty = ref(1)
const inboundRemark = ref('')
const submitting = ref(false)

const isLowStock = (row) => {
  const stock = row.stockQuantity ?? 0
  const min = row.minStock ?? 0
  return stock <= min
}

const formatDiff = (row) => {
  const book = row.stockQuantity ?? 0
  const actual = row._actualQty ?? book
  const diff = actual - book
  if (diff === 0) return '0'
  return diff > 0 ? `+${diff}` : String(diff)
}

const diffClass = (row) => {
  const book = row.stockQuantity ?? 0
  const actual = row._actualQty ?? book
  if (actual > book) return 'text-in'
  if (actual < book) return 'text-out'
  return ''
}

const loadCategories = async () => {
  try {
    const res = await getInventoryMedicineCategories()
    categories.value = Array.isArray(res) ? res : []
  } catch {
    categories.value = []
  }
}

const loadMedicines = async () => {
  medicineLoading.value = true
  try {
    const res = await getInventoryMedicinePage({
      current: medicinePage.value,
      size: medicinePageSize.value,
      keyword: keyword.value || undefined,
      categoryId: categoryId.value || undefined,
      status: 1
    })
    const list = res?.list || res?.records || []
    medicineList.value = list.map((m) => ({
      ...m,
      _actualQty: m.stockQuantity ?? 0
    }))
    medicineTotal.value = res?.total ?? 0
  } finally {
    medicineLoading.value = false
  }
}

const loadLogs = async () => {
  logLoading.value = true
  try {
    const res = await getInventoryStockLogPage({
      current: logPage.value,
      size: logPageSize.value,
      keyword: logKeyword.value || undefined,
      bizType: logBizType.value || undefined
    })
    logList.value = res?.list || res?.records || []
    logTotal.value = res?.total ?? 0
  } finally {
    logLoading.value = false
  }
}

const onTabChange = (name) => {
  if (name === 'log') {
    loadLogs()
  }
}

const submitAdjust = (row) => {
  const actual = row._actualQty ?? row.stockQuantity ?? 0
  if (actual === (row.stockQuantity ?? 0)) {
    ElMessage.info('实盘与账面一致，无需调整')
    return
  }
  adjustTarget.value = row
  adjustRemark.value = ''
  adjustDialogVisible.value = true
}

const confirmAdjust = async () => {
  if (!adjustTarget.value) return
  submitting.value = true
  try {
    await adjustInventoryStock(adjustTarget.value.medicineId, {
      actualQuantity: adjustTarget.value._actualQty,
      remark: adjustRemark.value || undefined
    })
    ElMessage.success('盘点调整成功')
    adjustDialogVisible.value = false
    loadMedicines()
  } finally {
    submitting.value = false
  }
}

const openInbound = (row) => {
  inboundTarget.value = row
  inboundQty.value = 1
  inboundRemark.value = ''
  inboundDialogVisible.value = true
}

const confirmInbound = async () => {
  if (!inboundTarget.value || !inboundQty.value || inboundQty.value < 1) {
    ElMessage.warning('请输入有效入库数量')
    return
  }
  submitting.value = true
  try {
    await inboundInventoryStock(inboundTarget.value.medicineId, {
      quantity: inboundQty.value,
      remark: inboundRemark.value || undefined
    })
    ElMessage.success('入库成功')
    inboundDialogVisible.value = false
    loadMedicines()
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadCategories()
  loadMedicines()
})
</script>

<style scoped>
.page { padding: 24px 28px 32px; min-height: 100%; }
.page-header { margin-bottom: 20px; }
.header-left { display: flex; align-items: center; gap: 14px; }
.page-icon {
  width: 48px; height: 48px; line-height: 48px; text-align: center; font-size: 22px; color: #fff;
  background: linear-gradient(135deg, #ec4899, #db2777); border-radius: 12px;
}
.page-title { margin: 0; font-size: 20px; font-weight: 700; color: #2c1810; }
.page-desc { margin: 4px 0 0; font-size: 13px; color: #5c4a32; }
.content-card {
  background: rgba(255,252,248,.95); border-radius: 16px; padding: 20px 22px 24px;
  box-shadow: 0 8px 40px rgba(61,41,20,.08), 0 0 0 1px rgba(139,90,43,.08);
}
.toolbar { display: flex; gap: 10px; margin-bottom: 16px; flex-wrap: wrap; align-items: center; }
.search-input { width: 220px; }
.filter-select { width: 160px; }
.pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }
.text-warning { color: #e6a23c; font-weight: 600; }
.text-in { color: #22c55e; font-weight: 600; }
.text-out { color: #ef4444; font-weight: 600; }
</style>
