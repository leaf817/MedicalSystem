<template>
  <div class="prescription-manage">
    <div class="page-header">
      <div class="header-left">
        <i class="fa-solid fa-file-prescription page-icon"></i>
        <div>
          <h2 class="page-title">处方管理</h2>
          <p class="page-desc">开立处方 / 查看历史处方记录</p>
        </div>
      </div>
    </div>

    <div class="content-card">
      <el-tabs v-model="activeTab" class="prescription-tabs">
        <el-tab-pane name="create">
          <template #label>
            <span><i class="fa-solid fa-pen-to-square"></i> 处方开立</span>
          </template>

          <div class="prescription-create">
            <!-- 无recordId时的提示 -->
            <div v-if="!recordId" class="empty-placeholder">
              <i class="fa-solid fa-file-prescription"></i>
              <p>请从病历页面选择病历后再开处方</p>
              <el-button type="primary" plain @click="goToMedicalRecord">前往病历管理</el-button>
            </div>

            <!-- 有recordId时显示 -->
            <template v-else>
              <!-- 患者信息展示 -->
              <el-descriptions title="患者信息" :column="3" border>
                <el-descriptions-item label="患者姓名">{{ patientInfo.name }}</el-descriptions-item>
                <el-descriptions-item label="患者编号">{{ patientInfo.patientId || ('P' + patientInfo.userId) }}</el-descriptions-item>
                <el-descriptions-item label="联系电话">{{ patientInfo.mobilePhone || '-' }}</el-descriptions-item>
                <el-descriptions-item label="就诊日期" v-if="recordInfo.visitDate">{{ recordInfo.visitDate }}</el-descriptions-item>
                <el-descriptions-item label="诊断" :span="2" v-if="recordInfo.diagnosis">{{ recordInfo.diagnosis }}</el-descriptions-item>
              </el-descriptions>

              <!-- 药品清单 -->
              <div class="section-title">
                <span>药品清单</span>
                <el-button type="primary" link @click="showMedicineDialog">
                  <el-icon><Plus /></el-icon>
                  添加药品
                </el-button>
              </div>

              <el-table :data="drugList" border stripe style="width: 100%">
                <el-table-column prop="name" label="药品名称" min-width="180" />
                <el-table-column prop="spec" label="规格" width="120" />
                <el-table-column prop="unit" label="单位" width="80" />
                <el-table-column prop="unitPrice" label="单价(元)" width="100">
                  <template #default="{ row }">¥{{ row.unitPrice?.toFixed(2) }}</template>
                </el-table-column>
                <el-table-column label="数量" width="130">
                  <template #default="{ row }">
                    <el-input-number
                        v-model="row.quantity"
                        :min="1"
                        :max="row.stockQuantity"
                        size="small"
                        controls-position="right"
                    />
                  </template>
                </el-table-column>
                <el-table-column label="用法用量" min-width="280">
                  <template #default="{ row }">
                    <div style="display: flex; gap: 8px; align-items: center; flex-wrap: wrap;">
                      <div style="display: flex; align-items: center; gap: 4px;">
                        <span style="font-size: 12px; color: #606266;">每日</span>
                        <el-input-number
                            v-model="row.frequency"
                            :min="1"
                            :max="10"
                            size="small"
                            style="width: 70px"
                            controls-position="right"
                        />
                        <span style="font-size: 12px; color: #606266;">次</span>
                      </div>
                      <div style="display: flex; align-items: center; gap: 4px;">
                        <span style="font-size: 12px; color: #606266;">每次</span>
                        <el-input-number
                            v-model="row.dosageAmount"
                            :min="0.5"
                            :step="0.5"
                            :precision="1"
                            size="small"
                            style="width: 80px"
                            controls-position="right"
                        />
                        <el-input
                            v-model="row.dosageUnit"
                            placeholder="单位"
                            size="small"
                            style="width: 60px"
                        />
                      </div>
                      <el-select v-model="row.timing" placeholder="服用时间" size="small" style="width: 80px" clearable>
                        <el-option label="饭前" value="饭前" />
                        <el-option label="饭后" value="饭后" />
                        <el-option label="空腹" value="空腹" />
                        <el-option label="睡前" value="睡前" />
                        <el-option label="随餐" value="随餐" />
                      </el-select>
                    </div>
                    <div style="font-size: 12px; color: #409eff; margin-top: 4px;" v-if="row.frequency && row.dosageAmount && row.dosageUnit">
                      预览：每日{{ row.frequency }}次，每次{{ row.dosageAmount }}{{ row.dosageUnit }}<span v-if="row.timing">，{{ row.timing }}</span>
                    </div>
                  </template>
                </el-table-column>
                <el-table-column label="小计(元)" width="110">
                  <template #default="{ row }">¥{{ (row.unitPrice * row.quantity)?.toFixed(2) }}</template>
                </el-table-column>
                <el-table-column label="操作" width="70" fixed="right">
                  <template #default="{ $index }">
                    <el-button type="danger" link @click="removeDrug($index)">
                      <el-icon><Delete /></el-icon>
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>

              <div class="total-amount">
                合计金额：<span class="amount">¥{{ totalAmount.toFixed(2) }}</span>
              </div>

              <el-form-item label="备注" style="margin-top: 20px">
                <el-input
                    v-model="remark"
                    type="textarea"
                    :rows="3"
                    placeholder="请输入备注信息"
                />
              </el-form-item>

              <div class="submit-btn-wrap">
                <el-button type="primary" @click="handleSubmit" :loading="submitting" size="large">
                  提交处方
                </el-button>
              </div>
            </template>
          </div>
        </el-tab-pane>

        <el-tab-pane name="list">
          <template #label>
            <span><i class="fa-solid fa-list"></i> 处方列表</span>
          </template>

          <div class="prescription-list">
            <div class="toolbar">
              <el-select v-model="listStatusFilter" clearable placeholder="处方状态" style="width: 140px" @change="loadPrescriptionList">
                <el-option label="待发药" :value="1" />
                <el-option label="已发药" :value="2" />
                <el-option label="已取消" :value="3" />
              </el-select>
              <el-input
                  v-model="listKeyword"
                  clearable
                  placeholder="搜索处方号/患者姓名"
                  style="width: 260px"
                  @clear="loadPrescriptionList"
                  @keyup.enter="loadPrescriptionList"
              >
                <template #prefix><i class="fa-solid fa-search"></i></template>
              </el-input>
            </div>

            <div v-if="listLoading" class="loading-wrap">
              <el-icon class="is-loading"><Loading /></el-icon> 加载中...
            </div>

            <div v-else-if="prescriptionList.length === 0" class="empty-custom">
              <el-empty description="暂无处方记录" />
            </div>

            <div v-else>
              <div
                  v-for="item in pagedPrescriptionList"
                  :key="item.prescriptionId"
                  class="prescription-card"
              >
                <div class="card-header">
                  <div class="hospital-info">
                    <span class="prescription-no">{{ item.prescriptionNo }}</span>
                    <el-tag :type="getStatusType(item.status)" size="small">{{ item.statusText }}</el-tag>
                  </div>
                  <div class="prescription-date">{{ formatTime(item.createdTime) }}</div>
                </div>

                <div class="card-body">
                  <div class="patient-info">
                    <div class="patient-avatar">
                      <i class="fa-solid fa-user"></i>
                    </div>
                    <div class="patient-detail">
                      <div class="patient-name">{{ item.patientName }}</div>
                      <div class="patient-diagnosis" v-if="item.diagnosis">{{ item.diagnosis }}</div>
                    </div>
                  </div>

                  <div class="prescription-info">
                    <div class="info-row">
                      <span class="info-label">医生</span>
                      <span class="info-value">{{ item.doctorName }}</span>
                    </div>
                    <div class="info-row">
                      <span class="info-label">药品数量</span>
                      <span class="info-value">{{ item.details?.length || 0 }} 种</span>
                    </div>
                    <div class="info-row">
                      <span class="info-label">总金额</span>
                      <span class="info-value amount">¥{{ item.totalAmount?.toFixed(2) }}</span>
                    </div>
                  </div>
                </div>

                <div class="card-footer">
                  <el-button size="small" type="primary" link @click="viewDetail(item)">
                    <i class="fa-solid fa-eye"></i> 查看详情
                  </el-button>
                  <el-button
                      v-if="item.status === 1"
                      size="small"
                      type="danger"
                      link
                      @click="handleDeletePrescription(item)"
                  >
                    <i class="fa-solid fa-trash"></i> 删除
                  </el-button>
                </div>
              </div>

              <div class="pagination-wrap">
                <el-pagination
                    v-model:current-page="listCurrentPage"
                    v-model:page-size="listPageSize"
                    :total="prescriptionList.length"
                    :page-sizes="[5, 10, 20]"
                    layout="total, sizes, prev, pager, next"
                    background
                    @size-change="onListPageSizeChange"
                    @current-change="onListPageSizeChange"
                />
              </div>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 药品选择弹窗 -->
    <el-dialog v-model="medicineDialogVisible" title="选择药品" width="900px">
      <div class="medicine-search-bar">
        <el-input
            v-model="medicineKeyword"
            placeholder="请输入药品名称/通用名/编码"
            clearable
            style="width: 260px"
            @clear="searchMedicines"
            @keyup.enter="searchMedicines"
        >
          <template #append>
            <el-button @click="searchMedicines">搜索</el-button>
          </template>
        </el-input>

        <el-select
            v-model="medicineCategoryId"
            placeholder="药品分类"
            clearable
            filterable
            style="width: 180px; margin-left: 12px"
            @change="searchMedicines"
        >
          <el-option
              v-for="c in categoryOptions"
              :key="c.categoryId"
              :label="c.name"
              :value="c.categoryId"
          />
        </el-select>
      </div>

      <el-table :data="medicineList" stripe style="margin-top: 20px" height="350" v-loading="medicineLoading">
        <el-table-column prop="medicineCode" label="药品编码" width="120" />
        <el-table-column prop="name" label="药品名称" min-width="150" />
        <el-table-column prop="commonName" label="通用名" min-width="150" />
        <el-table-column prop="spec" label="规格" width="120" />
        <el-table-column prop="unit" label="单位" width="60" />
        <el-table-column prop="unitPrice" label="单价(元)" width="90">
          <template #default="{ row }">¥{{ row.unitPrice?.toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="stockQuantity" label="库存" width="70" />
        <el-table-column label="操作" width="70" fixed="right">
          <template #default="{ row }">
            <el-button
                type="primary"
                link
                @click="addDrug(row)"
                :disabled="row.stockQuantity === 0"
            >
              选择
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap" style="margin-top: 20px; display: flex; justify-content: flex-end">
        <el-pagination
            v-model:current-page="medicineCurrentPage"
            v-model:page-size="medicinePageSize"
            :total="medicineTotal"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next"
            background
            @size-change="searchMedicines"
            @current-change="searchMedicines"
        />
      </div>
    </el-dialog>

    <!-- 处方详情弹窗 -->
    <el-dialog v-model="detailVisible" title="处方详情" width="850px" style="padding-top: 20px" class="medicine-dialog edit-dialog" :append-to-body="true" :modal-append-to-body="true">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="处方编号" :span="2">{{ detailData.prescriptionNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="患者姓名">{{ detailData.patientName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="医生姓名">{{ detailData.doctorName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="处方状态" :span="2">
          <el-tag :type="getStatusType(detailData.status)">{{ detailData.statusText }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="药品清单" :span="2">
          <el-table :data="detailData.details" border size="small" style="width: 100%">
            <el-table-column prop="medicineName" label="药品名称" min-width="150" />
            <el-table-column prop="spec" label="规格" width="100" />
            <el-table-column prop="quantity" label="数量" width="60" align="center" />
            <el-table-column prop="dosage" label="用法用量" min-width="150" />
            <el-table-column prop="unitPrice" label="单价" width="80" align="right">
              <template #default="{ row }">¥{{ row.unitPrice?.toFixed(2) }}</template>
            </el-table-column>
            <el-table-column prop="amount" label="小计" width="80" align="right">
              <template #default="{ row }">¥{{ row.amount?.toFixed(2) }}</template>
            </el-table-column>
          </el-table>
        </el-descriptions-item>
        <el-descriptions-item label="总金额">{{ detailData.totalAmount ? '¥' + detailData.totalAmount.toFixed(2) : '-' }}</el-descriptions-item>
        <el-descriptions-item label="开立时间">{{ formatTime(detailData.createdTime) }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detailData.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete, Loading } from '@element-plus/icons-vue'
import {
  getMedicinePage,
  createPrescription,
  getMedicineCategories,
  getPrescriptionDetail,
  deletePrescription,
  getUserPage,
  getMedicalRecordById,
  getMyPrescriptions
} from '@/api/admin'

const route = useRoute()
const router = useRouter()
const activeTab = ref('create')

// ==================== 处方开立相关 ====================
const recordId = ref(route.query.recordId || null)
const patientNo = ref(route.query.patientNo || null)
const userId = ref(patientNo.value ? parseInt(patientNo.value.replace('P', '')) : null)
const patientInfo = ref({})
const recordInfo = ref({})
const drugList = ref([])
const remark = ref('')
const submitting = ref(false)

// ==================== 处方列表相关 ====================
const listLoading = ref(false)
const prescriptionList = ref([])
const listCurrentPage = ref(1)
const listPageSize = ref(10)
const listStatusFilter = ref(null)
const listKeyword = ref('')
const detailVisible = ref(false)
const detailData = ref({})

// ==================== 药品搜索弹窗相关 ====================
const medicineDialogVisible = ref(false)
const medicineKeyword = ref('')
const medicineCategoryId = ref(null)
const medicineList = ref([])
const medicineLoading = ref(false)
const medicineCurrentPage = ref(1)
const medicinePageSize = ref(20)
const medicineTotal = ref(0)
const categoryOptions = ref([])

// 监听 patientNo 变化，计算 userId
watch(patientNo, (newVal) => {
  if (newVal) {
    userId.value = parseInt(newVal.replace('P', ''))
    console.log('userId:', userId.value)
  } else {
    userId.value = null
  }
}, { immediate: true })

// 合计金额
const totalAmount = computed(() => {
  return drugList.value.reduce((sum, drug) => sum + (drug.unitPrice * (drug.quantity || 0)), 0)
})

// 处方列表分页
const pagedPrescriptionList = computed(() => {
  const start = (listCurrentPage.value - 1) * listPageSize.value
  return prescriptionList.value.slice(start, start + listPageSize.value)
})

// 监听药品清单变化，自动生成完整用法用量
watch(drugList, (newList) => {
  newList.forEach(drug => {
    if (drug.frequency && drug.dosageAmount && drug.dosageUnit) {
      let dosage = `每日${drug.frequency}次，每次${drug.dosageAmount}${drug.dosageUnit}`
      if (drug.timing) {
        dosage += `，${drug.timing}`
      }
      drug.dosage = dosage
    }
  })
}, { deep: true, immediate: true })

const getStatusType = (status) => {
  const map = { 1: 'warning', 2: 'success', 3: 'info' }
  return map[status] || 'info'
}

const formatTime = (time) => {
  if (!time) return ''
  return new Date(time).toLocaleString()
}

// ==================== 加载处方列表 ====================
const loadPrescriptionList = async () => {
  listLoading.value = true
  try {
    const params = {}
    if (listStatusFilter.value) {
      params.status = listStatusFilter.value
    }
    const res = await getMyPrescriptions(params)
    let list = []
    if (Array.isArray(res)) {
      list = res
    } else if (res && Array.isArray(res.data)) {
      list = res.data
    } else if (res && res.list) {
      list = res.list
    }
    if (listKeyword.value) {
      const kw = listKeyword.value.toLowerCase()
      list = list.filter(item =>
          item.prescriptionNo?.toLowerCase().includes(kw) ||
          item.patientName?.toLowerCase().includes(kw)
      )
    }
    prescriptionList.value = list
    listCurrentPage.value = 1
  } catch (error) {
    console.error('加载处方列表失败', error)
    ElMessage.error('加载处方列表失败')
  } finally {
    listLoading.value = false
  }
}

const onListPageSizeChange = () => {
  listCurrentPage.value = 1
}

const viewDetail = async (row) => {
  try {
    const res = await getPrescriptionDetail(row.prescriptionId)
    detailData.value = res.data || res || {}
    detailVisible.value = true
  } catch (error) {
    ElMessage.error('获取详情失败')
  }
}

const handleDeletePrescription = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除此处方吗？删除后不可恢复', '提示', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await deletePrescription(row.prescriptionId)
    ElMessage.success('删除成功')
    await loadPrescriptionList()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

// ==================== 药品搜索 ====================
const loadCategoryOptions = async () => {
  try {
    const res = await getMedicineCategories()
    categoryOptions.value = res || []
  } catch (error) {
    categoryOptions.value = []
  }
}

const searchMedicines = async () => {
  medicineLoading.value = true
  try {
    const res = await getMedicinePage({
      current: medicineCurrentPage.value,
      size: medicinePageSize.value,
      keyword: medicineKeyword.value || undefined,
      categoryId: medicineCategoryId.value || undefined,
      status: 1
    })
    if (res && res.list) {
      medicineList.value = res.list
      medicineTotal.value = res.total || 0
    } else if (res && res.records) {
      medicineList.value = res.records
      medicineTotal.value = res.total || 0
    } else {
      medicineList.value = []
      medicineTotal.value = 0
    }
  } catch (error) {
    console.error('加载药品列表失败', error)
    ElMessage.error('加载药品列表失败')
  } finally {
    medicineLoading.value = false
  }
}

// ==================== 获取病历信息 ====================
const getRecordById = async () => {
  if (!recordId.value) return

  try {
    const res = await getMedicalRecordById(recordId.value)
    let recordData = null
    if (res.code === 200 && res.data) {
      recordData = res.data
    } else if (res.recordId) {
      recordData = res
    }

    if (recordData) {
      recordInfo.value = recordData
      // 获取患者信息
      if (recordData.patientId && userId.value) {
        const userRes = await getUserPage({
          current: 1,
          size: 1,
          userId: userId.value
        })
        let userList = []
        if (userRes && userRes.list) {
          userList = userRes.list
        } else if (userRes && userRes.data && userRes.data.list) {
          userList = userRes.data.list
        }
        if (userList.length > 0) {
          patientInfo.value = userList[0]
          patientInfo.value.patientId = recordData.patientId
        }
      }
      ElMessage.success('已获取病历信息')
    } else {
      ElMessage.error('病历不存在')
      recordId.value = null
    }
  } catch (error) {
    console.error('获取病历信息失败', error)
    ElMessage.error('获取病历信息失败')
    recordId.value = null
  }
}

// ==================== 处方开立操作 ====================
const addDrug = (medicine) => {
  const exist = drugList.value.find(d => d.medicineId === medicine.medicineId)
  if (exist) {
    ElMessage.warning('药品已存在，请修改数量')
    return
  }

  drugList.value.push({
    medicineId: medicine.medicineId,
    name: medicine.name,
    spec: medicine.spec,
    unit: medicine.unit,
    unitPrice: medicine.unitPrice,
    stockQuantity: medicine.stockQuantity,
    quantity: 1,
    frequency: 3,
    dosageAmount: 1,
    dosageUnit: '片',
    timing: '',
    dosage: ''
  })
  medicineDialogVisible.value = false
  medicineKeyword.value = ''
  medicineCategoryId.value = null
  medicineList.value = []
}

const removeDrug = (index) => {
  drugList.value.splice(index, 1)
}

const showMedicineDialog = async () => {
  if (!recordInfo.value.recordId) {
    ElMessage.warning('请先选择患者并确认有病历')
    return
  }
  medicineDialogVisible.value = true
  medicineKeyword.value = ''
  medicineCategoryId.value = null
  medicineCurrentPage.value = 1

  await loadCategoryOptions()
  await searchMedicines()
}

const goToMedicalRecord = () => {
  router.push('/doctor/medical-record')
}

const handleSubmit = async () => {
  if (!recordInfo.value.recordId) {
    ElMessage.warning('请先选择患者并确认有病历')
    return
  }

  if (drugList.value.length === 0) {
    ElMessage.warning('请至少添加一个药品')
    return
  }

  const invalidDrug = drugList.value.find(d => !d.dosage || !d.dosage.trim())
  if (invalidDrug) {
    ElMessage.warning('请完整填写药品用法用量（每日次数、每次用量、单位）')
    return
  }

  try {
    await ElMessageBox.confirm('确认提交处方吗？提交后不可修改', '提示', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning'
    })

    submitting.value = true
    const data = {
      recordId: recordInfo.value.recordId,
      details: drugList.value.map(d => ({
        medicineId: d.medicineId,
        quantity: d.quantity,
        dosage: d.dosage
      })),
      remark: remark.value
    }

    const res = await createPrescription(data)

    if (res && res.prescriptionId) {
      ElMessage.success('处方开立成功')

      drugList.value = []
      remark.value = ''
      patientInfo.value = {}
      recordInfo.value = {}

      await loadPrescriptionList()

      activeTab.value = 'list'
    } else {
      ElMessage.error('提交失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '提交失败')
    }
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  loadPrescriptionList()
  if (recordId.value) {
    getRecordById()
  } else {
    ElMessage.warning('请从病历页面选择病历后再开处方')
  }
})
</script>

<style scoped>
/* 样式保持不变，删除了患者选择相关样式 */
.prescription-manage {
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
  margin: 4px 0 0;
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
  padding: 24px 28px;
}

.prescription-tabs :deep(.el-tabs__header) {
  margin-bottom: 24px;
  border-bottom: 1px solid rgba(139, 90, 43, 0.15);
}

.prescription-tabs :deep(.el-tabs__item) {
  font-size: 15px;
  font-weight: 500;
  color: #8b7a68;
}

.prescription-tabs :deep(.el-tabs__item.is-active) {
  color: #d48232;
}

.prescription-tabs :deep(.el-tabs__active-bar) {
  background: linear-gradient(135deg, #e8a54b, #d48232);
}

.prescription-create {
  min-height: 500px;
}

.empty-placeholder {
  text-align: center;
  padding: 80px 20px;
  color: #b0a088;
}

.empty-placeholder i {
  font-size: 64px;
  margin-bottom: 16px;
  color: #d4c5b0;
}

.empty-placeholder p {
  font-size: 14px;
  margin: 0 0 16px;
}

.section-title {
  font-size: 16px;
  font-weight: 500;
  margin: 20px 0 15px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.total-amount {
  text-align: right;
  margin-top: 20px;
  font-size: 16px;
}

.total-amount .amount {
  color: #f56c6c;
  font-size: 20px;
  font-weight: bold;
  margin-left: 10px;
}

.submit-btn-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 24px;
}

.medicine-search-bar {
  display: flex;
  align-items: center;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
}

/* 处方列表样式 */
.prescription-list {
  min-height: 400px;
}

.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
  flex-wrap: wrap;
}

.loading-wrap {
  text-align: center;
  padding: 60px;
  color: #8b7a68;
}

.empty-custom {
  padding: 60px 0;
}

.prescription-card {
  background: #fefaf5;
  border: 1px solid #f0e4d4;
  border-radius: 14px;
  margin-bottom: 16px;
  padding: 20px 24px;
  transition: all 0.2s ease;
}

.prescription-card:hover {
  border-color: #e8a54b;
  box-shadow: 0 4px 12px rgba(232, 165, 75, 0.15);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 15px;
  border-bottom: 1px solid #f0e4d4;
  margin-bottom: 15px;
}

.hospital-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.prescription-no {
  font-size: 14px;
  font-weight: 600;
  color: #d48232;
  font-family: monospace;
}

.prescription-date {
  font-size: 12px;
  color: #b0a088;
}

.card-body {
  display: flex;
  gap: 30px;
  flex-wrap: wrap;
  margin-bottom: 20px;
}

.patient-info {
  display: flex;
  gap: 15px;
  align-items: center;
  min-width: 180px;
}

.patient-avatar {
  width: 56px;
  height: 56px;
  line-height: 56px;
  text-align: center;
  font-size: 28px;
  background: linear-gradient(135deg, #e8f4f0, #d4e8e0);
  border-radius: 50%;
  color: #2c7a5e;
}

.patient-name {
  font-size: 16px;
  font-weight: 600;
  color: #2c1810;
}

.patient-diagnosis {
  font-size: 12px;
  color: #e6a23c;
  background: rgba(230, 162, 60, 0.1);
  display: inline-block;
  padding: 2px 10px;
  border-radius: 12px;
  margin-top: 4px;
}

.prescription-info {
  flex: 1;
}

.info-row {
  display: flex;
  margin-bottom: 10px;
  font-size: 14px;
  color: #2c1810;
}

.info-label {
  width: 80px;
  font-weight: 500;
  color: #8b7a68;
}

.info-value {
  flex: 1;
}

.info-value.amount {
  color: #f56c6c;
  font-weight: 600;
}

.card-footer {
  display: flex;
  justify-content: flex-end;
  gap: 16px;
  padding-top: 15px;
  border-top: 1px solid #f0e4d4;
}

.medicine-dialog.edit-dialog :deep(.el-dialog) {
  border-radius: 16px;
  overflow: hidden;
  background: rgba(255, 252, 250, 0.98);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow: 0 8px 40px rgba(61, 41, 20, 0.15);
}

.medicine-dialog.edit-dialog :deep(.el-dialog__header) {
  padding: 20px 24px;
  border-bottom: 1px solid rgba(139, 90, 43, 0.12);
  background: rgba(255, 250, 245, 0.5);
}

.medicine-dialog.edit-dialog :deep(.el-dialog__footer) {
  padding: 16px 24px 24px;
  border-top: 1px solid rgba(139, 90, 43, 0.08);
}
</style>