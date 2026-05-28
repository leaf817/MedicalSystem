package com.medical.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medical.common.exception.BusinessWarningException;
import com.medical.common.exception.ServiceException;
import com.medical.common.pagination.PageResult;
import com.medical.domain.entity.Medicine;
import com.medical.domain.entity.MedicineStockLog;
import com.medical.domain.entity.SysUser;
import com.medical.domain.vo.MedicineStockLogVo;
import com.medical.mapper.MedicineMapper;
import com.medical.mapper.MedicineStockLogMapper;
import com.medical.mapper.SysUserMapper;
import com.medical.service.MedicineStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MedicineStockServiceImpl implements MedicineStockService {

    private final MedicineMapper medicineMapper;
    private final MedicineStockLogMapper medicineStockLogMapper;
    private final SysUserMapper sysUserMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adjustToActual(Long medicineId, int actualQuantity, Long operatorId, String remark) {
        Medicine medicine = requireMedicine(medicineId);
        int before = safeQty(medicine.getStockQuantity());
        if (actualQuantity == before) {
            return;
        }
        int change = actualQuantity - before;
        medicine.setStockQuantity(actualQuantity);
        medicine.setUpdatedTime(LocalDateTime.now());
        medicineMapper.updateById(medicine);
        insertLog(medicineId, change, before, actualQuantity, "ADJUST", null, operatorId, remark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void inbound(Long medicineId, int quantity, Long operatorId, String remark) {
        if (quantity <= 0) {
            throw new BusinessWarningException("入库数量必须大于0");
        }
        Medicine medicine = requireMedicine(medicineId);
        int before = safeQty(medicine.getStockQuantity());
        int after = before + quantity;
        medicine.setStockQuantity(after);
        medicine.setUpdatedTime(LocalDateTime.now());
        medicineMapper.updateById(medicine);
        insertLog(medicineId, quantity, before, after, "INBOUND", null, operatorId, remark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deductForDispense(Long medicineId, int quantity, Long prescriptionId, Long operatorId) {
        if (quantity <= 0) {
            throw new BusinessWarningException("扣减数量无效");
        }
        Medicine medicine = requireMedicine(medicineId);
        int before = safeQty(medicine.getStockQuantity());
        if (before < quantity) {
            throw new BusinessWarningException("药品库存不足: " + medicine.getName() + "，当前库存: " + before);
        }
        int after = before - quantity;
        medicine.setStockQuantity(after);
        medicine.setUpdatedTime(LocalDateTime.now());
        medicineMapper.updateById(medicine);
        insertLog(medicineId, -quantity, before, after, "DISPENSE", prescriptionId, operatorId, "发药扣减");
    }

    @Override
    public PageResult<MedicineStockLogVo> pageLogs(
            Long current, Long size, Long medicineId, String bizType, String keyword) {
        LambdaQueryWrapper<MedicineStockLog> wrapper = new LambdaQueryWrapper<>();
        if (medicineId != null) {
            wrapper.eq(MedicineStockLog::getMedicineId, medicineId);
        }
        if (StringUtils.hasText(bizType)) {
            wrapper.eq(MedicineStockLog::getBizType, bizType.trim().toUpperCase());
        }
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            List<Medicine> matched = medicineMapper.selectList(
                    new LambdaQueryWrapper<Medicine>()
                            .and(w -> w.like(Medicine::getName, kw)
                                    .or().like(Medicine::getMedicineCode, kw)));
            if (matched.isEmpty()) {
                PageResult<MedicineStockLogVo> empty = new PageResult<>();
                empty.setCurrentPage(current);
                empty.setPageSize(size);
                empty.setTotal(0L);
                empty.setList(List.of());
                return empty;
            }
            Set<Long> ids = matched.stream().map(Medicine::getMedicineId).collect(Collectors.toSet());
            wrapper.in(MedicineStockLog::getMedicineId, ids);
        }
        wrapper.orderByDesc(MedicineStockLog::getCreatedTime).orderByDesc(MedicineStockLog::getLogId);

        Page<MedicineStockLog> page = medicineStockLogMapper.selectPage(new Page<>(current, size), wrapper);
        return toLogPageResult(page);
    }

    @Override
    public long countStockWarning() {
        return medicineMapper.selectCount(
                new LambdaQueryWrapper<Medicine>()
                        .apply("stock_quantity IS NOT NULL AND min_stock IS NOT NULL AND stock_quantity <= min_stock"));
    }

    private Medicine requireMedicine(Long medicineId) {
        Medicine medicine = medicineMapper.selectById(medicineId);
        if (medicine == null) {
            throw new ServiceException("药品不存在");
        }
        return medicine;
    }

    private int safeQty(Integer qty) {
        return qty != null ? qty : 0;
    }

    private void insertLog(
            Long medicineId,
            int changeQty,
            int beforeQty,
            int afterQty,
            String bizType,
            Long bizId,
            Long operatorId,
            String remark) {
        MedicineStockLog log = new MedicineStockLog();
        log.setMedicineId(medicineId);
        log.setChangeQty(changeQty);
        log.setBeforeQty(beforeQty);
        log.setAfterQty(afterQty);
        log.setBizType(bizType);
        log.setBizId(bizId);
        log.setOperatorId(operatorId);
        log.setRemark(remark);
        log.setCreatedTime(LocalDateTime.now());
        medicineStockLogMapper.insert(log);
    }

    private PageResult<MedicineStockLogVo> toLogPageResult(Page<MedicineStockLog> page) {
        Set<Long> medicineIds = page.getRecords().stream()
                .map(MedicineStockLog::getMedicineId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Medicine> medicineMap = new HashMap<>();
        if (!medicineIds.isEmpty()) {
            for (Medicine m : medicineMapper.selectBatchIds(medicineIds)) {
                medicineMap.put(m.getMedicineId(), m);
            }
        }

        Set<Long> operatorIds = page.getRecords().stream()
                .map(MedicineStockLog::getOperatorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> operatorNameMap = new HashMap<>();
        if (!operatorIds.isEmpty()) {
            for (SysUser u : sysUserMapper.selectBatchIds(operatorIds)) {
                operatorNameMap.put(u.getUserId(), StringUtils.hasText(u.getName()) ? u.getName() : u.getUsername());
            }
        }

        List<MedicineStockLogVo> voList = page.getRecords().stream().map(log -> {
            MedicineStockLogVo vo = new MedicineStockLogVo();
            vo.setLogId(log.getLogId());
            vo.setMedicineId(log.getMedicineId());
            vo.setChangeQty(log.getChangeQty());
            vo.setBeforeQty(log.getBeforeQty());
            vo.setAfterQty(log.getAfterQty());
            vo.setBizType(log.getBizType());
            vo.setBizTypeText(bizTypeText(log.getBizType()));
            vo.setBizId(log.getBizId());
            vo.setOperatorId(log.getOperatorId());
            vo.setOperatorName(operatorNameMap.get(log.getOperatorId()));
            vo.setRemark(log.getRemark());
            vo.setCreatedTime(log.getCreatedTime());
            Medicine m = medicineMap.get(log.getMedicineId());
            if (m != null) {
                vo.setMedicineCode(m.getMedicineCode());
                vo.setMedicineName(m.getName());
            }
            return vo;
        }).collect(Collectors.toList());

        PageResult<MedicineStockLogVo> result = new PageResult<>();
        result.setCurrentPage(page.getCurrent());
        result.setPageSize(page.getSize());
        result.setTotal(page.getTotal());
        result.setList(voList);
        return result;
    }

    private String bizTypeText(String bizType) {
        if (bizType == null) {
            return "";
        }
        return switch (bizType) {
            case "DISPENSE" -> "发药出库";
            case "ADJUST" -> "盘点调整";
            case "INBOUND" -> "入库";
            default -> bizType;
        };
    }
}
