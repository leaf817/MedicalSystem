package com.medical.service;

import com.medical.common.pagination.PageResult;
import com.medical.domain.vo.MedicineStockLogVo;

public interface MedicineStockService {

    /**
     * 盘点调整：将账面库存校正为实盘数量
     */
    void adjustToActual(Long medicineId, int actualQuantity, Long operatorId, String remark);

    /**
     * 入库增加库存
     */
    void inbound(Long medicineId, int quantity, Long operatorId, String remark);

    /**
     * 发药扣减库存并记流水
     */
    void deductForDispense(Long medicineId, int quantity, Long prescriptionId, Long operatorId);

    PageResult<MedicineStockLogVo> pageLogs(
            Long current, Long size, Long medicineId, String bizType, String keyword);

    /** 当前库存低于或等于最低库存的药品数量 */
    long countStockWarning();
}
