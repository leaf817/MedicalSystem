package com.medical.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("medicine_stock_log")
public class MedicineStockLog {
    @TableId(type = IdType.AUTO)
    private Long logId;
    private Long medicineId;
    private Integer changeQty;
    private Integer beforeQty;
    private Integer afterQty;
    private String bizType;
    private Long bizId;
    private Long operatorId;
    private String remark;
    private LocalDateTime createdTime;
}
