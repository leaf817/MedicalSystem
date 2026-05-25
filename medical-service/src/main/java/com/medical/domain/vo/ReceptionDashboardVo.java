package com.medical.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ReceptionDashboardVo {
    private Long todayAppointments;
    private Long pendingPayment;
    private BigDecimal todayPaidAmount;
    private Long todayRefunds;
}
