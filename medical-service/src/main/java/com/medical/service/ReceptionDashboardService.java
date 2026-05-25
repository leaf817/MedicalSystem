package com.medical.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.medical.domain.entity.Appointment;
import com.medical.domain.vo.ReceptionDashboardVo;
import com.medical.mapper.AppointmentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ReceptionDashboardService {

    private final AppointmentMapper appointmentMapper;
    private final PaymentService paymentService;

    public ReceptionDashboardVo getSummary() {
        LocalDate today = LocalDate.now();
        ReceptionDashboardVo vo = new ReceptionDashboardVo();

        vo.setTodayAppointments(appointmentMapper.selectCount(
                new LambdaQueryWrapper<Appointment>()
                        .eq(Appointment::getAppointmentDate, today)
                        .ne(Appointment::getStatus, 3)));

        vo.setPendingPayment(appointmentMapper.selectCount(
                new LambdaQueryWrapper<Appointment>()
                        .eq(Appointment::getAppointmentDate, today)
                        .eq(Appointment::getStatus, 1)
                        .eq(Appointment::getPaid, 0)));

        vo.setTodayPaidAmount(paymentService.sumTodayPaidAmount());
        vo.setTodayRefunds(paymentService.countTodayRefunds());
        return vo;
    }
}
