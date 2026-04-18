package com.medical.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.medical.common.exception.BusinessWarningException;
import com.medical.domain.entity.Appointment;
import com.medical.domain.entity.Doctor;
import com.medical.domain.entity.Patient;
import com.medical.domain.entity.Schedule;
import com.medical.domain.entity.SysDept;
import com.medical.domain.vo.AppointmentVo;
import com.medical.mapper.AppointmentMapper;
import com.medical.mapper.DoctorMapper;
import com.medical.mapper.PatientMapper;
import com.medical.mapper.SysDeptMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final AppointmentMapper appointmentMapper;
    private final DoctorMapper doctorMapper;
    private final PatientMapper patientMapper;
    private final SysDeptMapper sysDeptMapper;
    private final ScheduleService scheduleService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // 时间段排序映射
    private static final Map<String, Integer> TIME_SLOT_ORDER = new LinkedHashMap<>();

    static {
        TIME_SLOT_ORDER.put("08:00-09:00", 1);
        TIME_SLOT_ORDER.put("09:00-10:00", 2);
        TIME_SLOT_ORDER.put("10:00-11:00", 3);
        TIME_SLOT_ORDER.put("11:00-12:00", 4);
        TIME_SLOT_ORDER.put("14:00-15:00", 5);
        TIME_SLOT_ORDER.put("15:00-16:00", 6);
        TIME_SLOT_ORDER.put("16:00-17:00", 7);
        TIME_SLOT_ORDER.put("17:00-18:00", 8);
    }

    /**
     * 获取时间段排序权重
     */
    private int getTimeSlotWeight(String timeSlot) {
        return TIME_SLOT_ORDER.getOrDefault(timeSlot, 999);
    }

    /**
     * 生成排队号（按时段和创建时间）
     */
    private Integer generateQueueNumber(Long doctorId, LocalDate date, String timeSlot, LocalDateTime createdTime) {
        // 获取所有待就诊预约，按时段和创建时间排序
        LambdaQueryWrapper<Appointment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Appointment::getDoctorId, doctorId)
                .eq(Appointment::getAppointmentDate, date)
                .eq(Appointment::getStatus, 1);

        List<Appointment> existingAppointments = appointmentMapper.selectList(wrapper);

        // 创建新预约的临时对象用于排序
        Appointment newAppointment = new Appointment();
        newAppointment.setTimeSlot(timeSlot);
        newAppointment.setCreatedTime(createdTime);
        newAppointment.setPaid(0);

        existingAppointments.add(newAppointment);

        // 排序：先按支付状态（已支付优先），再按时段，再按创建时间
        existingAppointments.sort((a1, a2) -> {
            int paidCompare = Integer.compare(
                    a2.getPaid() != null ? a2.getPaid() : 0,
                    a1.getPaid() != null ? a1.getPaid() : 0);
            if (paidCompare != 0) {
                return paidCompare;
            }
            int weight1 = getTimeSlotWeight(a1.getTimeSlot());
            int weight2 = getTimeSlotWeight(a2.getTimeSlot());
            if (weight1 != weight2) {
                return Integer.compare(weight1, weight2);
            }
            return a1.getCreatedTime().compareTo(a2.getCreatedTime());
        });

        // 找到新预约的位置，返回排队号
        for (int i = 0; i < existingAppointments.size(); i++) {
            if (existingAppointments.get(i) == newAppointment) {
                return i + 1;
            }
        }

        return existingAppointments.size();
    }

    /**
     * 创建预约
     */
    @Transactional(rollbackFor = Exception.class)
    public Appointment createAppointment(Long patientId, Long scheduleId) {
        Schedule schedule = scheduleService.getScheduleById(scheduleId);
        if (schedule == null) {
            throw new BusinessWarningException("排班不存在");
        }
        if (schedule.getStatus() != 1) {
            throw new BusinessWarningException("该时段已停诊");
        }
        if (schedule.getBookedSlots() >= schedule.getTotalSlots()) {
            throw new BusinessWarningException("该时段号源已满");
        }

        LambdaQueryWrapper<Appointment> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(Appointment::getPatientId, patientId)
                .eq(Appointment::getScheduleId, scheduleId)
                .in(Appointment::getStatus, List.of(1, 2));
        if (appointmentMapper.selectCount(checkWrapper) > 0) {
            throw new BusinessWarningException("您已预约过该时段，请勿重复预约");
        }

        Doctor doctor = doctorMapper.selectById(schedule.getDoctorId());
        if (doctor == null) {
            throw new BusinessWarningException("医生信息不存在");
        }

        boolean locked = scheduleService.lockSlot(scheduleId);
        if (!locked) {
            throw new BusinessWarningException("号源已被抢完，请重新选择");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDate appointmentDate = schedule.getScheduleDate();
        Long doctorId = schedule.getDoctorId();
        String timeSlot = schedule.getTimeSlot();

        // 按时间段和创建时间生成排队号
        Integer queueNumber = generateQueueNumber(doctorId, appointmentDate, timeSlot, now);

        Appointment appointment = new Appointment();
        appointment.setAppointmentNo(generateAppointmentNo());
        appointment.setPatientId(patientId);
        appointment.setDoctorId(schedule.getDoctorId());
        appointment.setScheduleId(scheduleId);
        appointment.setAppointmentDate(appointmentDate);
        appointment.setTimeSlot(timeSlot);
        appointment.setQueueNo(queueNumber);
        appointment.setStatus(1);
        appointment.setFeeAmount(doctor.getConsultationFee());
        appointment.setPaid(0);
        appointment.setCreatedTime(now);
        appointment.setUpdatedTime(now);
        appointmentMapper.insert(appointment);

        // 重新分配所有排队号（确保连续性）
        reassignAllQueueNumbers(doctorId, appointmentDate);

        return appointment;
    }

    /**
     * 重新分配所有排队号（按时段和创建时间排序）
     */
    private void reassignAllQueueNumbers(Long doctorId, LocalDate date) {
        LambdaQueryWrapper<Appointment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Appointment::getDoctorId, doctorId)
                .eq(Appointment::getAppointmentDate, date)
                .eq(Appointment::getStatus, 1);

        List<Appointment> appointments = appointmentMapper.selectList(wrapper);

        // 排序：先按支付状态（已支付优先），再按时段，再按创建时间
        appointments.sort((a1, a2) -> {
            int paidCompare = Integer.compare(
                    a2.getPaid() != null ? a2.getPaid() : 0,
                    a1.getPaid() != null ? a1.getPaid() : 0);
            if (paidCompare != 0) {
                return paidCompare;
            }
            int weight1 = getTimeSlotWeight(a1.getTimeSlot());
            int weight2 = getTimeSlotWeight(a2.getTimeSlot());
            if (weight1 != weight2) {
                return Integer.compare(weight1, weight2);
            }
            return a1.getCreatedTime().compareTo(a2.getCreatedTime());
        });

        int queueNo = 1;
        for (Appointment appointment : appointments) {
            if (!Objects.equals(appointment.getQueueNo(), queueNo)) {
                appointment.setQueueNo(queueNo);
                appointment.setUpdatedTime(LocalDateTime.now());
                appointmentMapper.updateById(appointment);
            }
            queueNo++;
        }
    }

    /**
     * 生成预约单号
     */
    private String generateAppointmentNo() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String timestamp = LocalDateTime.now().format(formatter);
        int random = (int) (Math.random() * 9000) + 1000;
        return "APT" + timestamp + random;
    }

    /**
     * 取消预约
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancelAppointment(Long appointmentId, Long patientId) {
        Appointment appointment = appointmentMapper.selectById(appointmentId);
        if (appointment == null) {
            throw new BusinessWarningException("预约记录不存在");
        }
        if (!appointment.getPatientId().equals(patientId)) {
            throw new BusinessWarningException("无权操作此预约");
        }
        if (appointment.getStatus() != 1) {
            throw new BusinessWarningException("当前状态无法取消");
        }

        appointment.setStatus(3);
        appointment.setUpdatedTime(LocalDateTime.now());
        appointmentMapper.updateById(appointment);

        scheduleService.releaseSlot(appointment.getScheduleId());

        // 取消后重新分配排队号
        reassignAllQueueNumbers(appointment.getDoctorId(), appointment.getAppointmentDate());
    }

    /**
     * 支付预约
     */
    @Transactional(rollbackFor = Exception.class)
    public void payAppointment(Long appointmentId, Long patientId) {
        Appointment appointment = appointmentMapper.selectById(appointmentId);
        if (appointment == null) {
            throw new BusinessWarningException("预约记录不存在");
        }
        if (!appointment.getPatientId().equals(patientId)) {
            throw new BusinessWarningException("无权操作此预约");
        }
        if (appointment.getStatus() != 1) {
            throw new BusinessWarningException("只有待就诊状态的预约可以支付");
        }
        if (appointment.getPaid() == 1) {
            throw new BusinessWarningException("该预约已支付");
        }

        appointment.setPaid(1);
        appointment.setPaidTime(LocalDateTime.now());
        appointment.setUpdatedTime(LocalDateTime.now());
        appointmentMapper.updateById(appointment);

        // 支付后重新分配排队号（已支付的优先）
        reassignAllQueueNumbers(appointment.getDoctorId(), appointment.getAppointmentDate());
    }

    /**
     * 获取患者的预约列表
     */
    public List<AppointmentVo> getPatientAppointments(Long patientId, Integer status, Integer paid, String keyword) {
        LambdaQueryWrapper<Appointment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Appointment::getPatientId, patientId);

        if (status != null) {
            wrapper.eq(Appointment::getStatus, status);
        }
        if (paid != null) {
            wrapper.eq(Appointment::getPaid, paid);
        }

        wrapper.orderByDesc(Appointment::getAppointmentDate).orderByAsc(Appointment::getTimeSlot);
        List<Appointment> appointments = appointmentMapper.selectList(wrapper);

        List<AppointmentVo> voList = appointments.stream().map(a -> {
            AppointmentVo vo = new AppointmentVo();
            vo.setAppointmentId(a.getAppointmentId());
            vo.setAppointmentNo(a.getAppointmentNo());
            vo.setPatientId(a.getPatientId());
            vo.setAppointmentDate(a.getAppointmentDate().format(DATE_FORMATTER));
            vo.setTimeSlot(a.getTimeSlot());
            vo.setQueueNo(a.getQueueNo());
            vo.setStatus(a.getStatus());
            vo.setStatusText(getStatusText(a.getStatus()));
            vo.setFeeAmount(a.getFeeAmount());
            vo.setPaid(a.getPaid());
            vo.setCreatedTime(a.getCreatedTime());

            Doctor doctor = doctorMapper.selectById(a.getDoctorId());
            if (doctor != null) {
                vo.setDoctorName(doctor.getName());
                vo.setDoctorTitle(doctor.getTitle());
                SysDept dept = sysDeptMapper.selectById(doctor.getDeptId());
                if (dept != null) {
                    vo.setDeptName(dept.getName());
                }
            }

            Patient patient = patientMapper.selectById(a.getPatientId());
            if (patient != null) {
                vo.setPatientName(patient.getName());
            }

            return vo;
        }).collect(Collectors.toList());

        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim().toLowerCase();
            voList = voList.stream().filter(v ->
                    (v.getAppointmentNo() != null && v.getAppointmentNo().toLowerCase().contains(kw)) ||
                            (v.getDoctorName() != null && v.getDoctorName().toLowerCase().contains(kw)) ||
                            (v.getDeptName() != null && v.getDeptName().toLowerCase().contains(kw))
            ).collect(Collectors.toList());
        }

        return voList;
    }

    /**
     * 获取预约详情
     */
    public AppointmentVo getAppointmentDetail(Long appointmentId, Long patientId) {
        Appointment appointment = appointmentMapper.selectById(appointmentId);
        if (appointment == null) {
            throw new BusinessWarningException("预约记录不存在");
        }
        if (!appointment.getPatientId().equals(patientId)) {
            throw new BusinessWarningException("无权查看此预约");
        }

        AppointmentVo vo = new AppointmentVo();
        vo.setAppointmentId(appointment.getAppointmentId());
        vo.setAppointmentNo(appointment.getAppointmentNo());
        vo.setPatientId(appointment.getPatientId());
        vo.setAppointmentDate(appointment.getAppointmentDate().format(DATE_FORMATTER));
        vo.setTimeSlot(appointment.getTimeSlot());
        vo.setQueueNo(appointment.getQueueNo());
        vo.setStatus(appointment.getStatus());
        vo.setStatusText(getStatusText(appointment.getStatus()));
        vo.setFeeAmount(appointment.getFeeAmount());
        vo.setPaid(appointment.getPaid());
        vo.setCreatedTime(appointment.getCreatedTime());

        Doctor doctor = doctorMapper.selectById(appointment.getDoctorId());
        if (doctor != null) {
            vo.setDoctorName(doctor.getName());
            vo.setDoctorTitle(doctor.getTitle());
            SysDept dept = sysDeptMapper.selectById(doctor.getDeptId());
            if (dept != null) {
                vo.setDeptName(dept.getName());
            }
        }

        Patient patient = patientMapper.selectById(appointment.getPatientId());
        if (patient != null) {
            vo.setPatientName(patient.getName());
        }

        return vo;
    }

    private String getStatusText(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 1: return "待就诊";
            case 2: return "已就诊";
            case 3: return "已取消";
            case 4: return "爽约";
            default: return "未知";
        }
    }
}