package com.medical.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.medical.domain.entity.Prescription;
import com.medical.domain.vo.NurseDashboardVo;
import com.medical.mapper.PrescriptionMapper;
import com.medical.service.MedicineStockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NurseDashboardService {

    private final PrescriptionMapper prescriptionMapper;
    private final MedicineStockService medicineStockService;

    public NurseDashboardVo getStats() {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();

        NurseDashboardVo vo = new NurseDashboardVo();
        vo.setPendingDispense(prescriptionMapper.selectCount(
                new LambdaQueryWrapper<Prescription>()
                        .eq(Prescription::getStatus, 1)));

        vo.setTodayDispensed(prescriptionMapper.selectCount(
                new LambdaQueryWrapper<Prescription>()
                        .eq(Prescription::getStatus, 2)
                        .ge(Prescription::getUpdatedTime, start)
                        .lt(Prescription::getUpdatedTime, end)));
        vo.setStockWarningCount(medicineStockService.countStockWarning());
        return vo;
    }
}
