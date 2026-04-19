package com.medical.web.api.doctor;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.medical.common.exception.BusinessWarningException;
import com.medical.common.exception.ServiceException;
import com.medical.common.response.ResultVo;
import com.medical.domain.entity.Appointment;
import com.medical.domain.entity.Doctor;
import com.medical.domain.entity.Patient;
import com.medical.domain.entity.SysUser;
import com.medical.domain.vo.QueueVo;
import com.medical.mapper.AppointmentMapper;
import com.medical.mapper.DoctorMapper;
import com.medical.mapper.PatientMapper;
import com.medical.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/doctor/queue")
@RequiredArgsConstructor
public class DoctorQueueController {

    private final AppointmentMapper appointmentMapper;
    private final DoctorMapper doctorMapper;
    private final PatientMapper patientMapper;
    private final SysUserMapper sysUserMapper;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // 时间段排序映射（按开始时间排序）
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
     * 获取当前登录用户ID
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails) {
            String username = ((UserDetails) principal).getUsername();
            SysUser user = sysUserMapper.selectOne(
                    new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
            if (user != null) {
                return user.getUserId();
            }
        }
        return null;
    }

    /**
     * 获取当前登录医生信息
     */
    private Doctor getCurrentDoctor() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            throw new ServiceException("未登录");
        }
        Doctor doctor = doctorMapper.selectOne(
                new LambdaQueryWrapper<Doctor>()
                        .eq(Doctor::getUserId, userId));
        if (doctor == null) {
            throw new ServiceException("未找到医生信息");
        }
        return doctor;
    }

    /**
     * 获取时间段排序权重
     */
    private int getTimeSlotWeight(String timeSlot) {
        return TIME_SLOT_ORDER.getOrDefault(timeSlot, 999);
    }

    /**
     * 获取当天最大排队号
     */
    private Integer getMaxQueueNumber(Long doctorId, LocalDate date) {
        LambdaQueryWrapper<Appointment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Appointment::getDoctorId, doctorId)
                .eq(Appointment::getAppointmentDate, date)
                .orderByDesc(Appointment::getQueueNo)
                .last("LIMIT 1");
        Appointment maxAppointment = appointmentMapper.selectOne(wrapper);
        return maxAppointment != null && maxAppointment.getQueueNo() != null ? maxAppointment.getQueueNo() : 0;
    }

    /**
     * 生成排队号（按时间段分组，同一时间段内按创建时间顺序）
     */
    private Integer generateQueueNumber(Long doctorId, LocalDate date, String timeSlot, LocalDateTime createdTime) {
        // 查询同一时间段内所有预约，按创建时间排序
        LambdaQueryWrapper<Appointment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Appointment::getDoctorId, doctorId)
                .eq(Appointment::getAppointmentDate, date)
                .eq(Appointment::getTimeSlot, timeSlot)
                .orderByAsc(Appointment::getCreatedTime);

        List<Appointment> sameSlotAppointments = appointmentMapper.selectList(wrapper);

        if (sameSlotAppointments.isEmpty()) {
            // 如果当前时间段没有预约，需要计算该时间段应该从哪个数字开始
            // 获取所有时间段的最大排队号，然后加上该时间段内的序号
            LambdaQueryWrapper<Appointment> allWrapper = new LambdaQueryWrapper<>();
            allWrapper.eq(Appointment::getDoctorId, doctorId)
                    .eq(Appointment::getAppointmentDate, date)
                    .orderByDesc(Appointment::getQueueNo)
                    .last("LIMIT 1");
            Appointment maxAppointment = appointmentMapper.selectOne(allWrapper);
            int maxQueueNo = maxAppointment != null && maxAppointment.getQueueNo() != null ? maxAppointment.getQueueNo() : 0;
            return maxQueueNo + 1;
        } else {
            // 获取当前时间段最后一个预约的排队号，然后 +1
            Appointment lastInSlot = sameSlotAppointments.get(sameSlotAppointments.size() - 1);
            return lastInSlot.getQueueNo() + 1;
        }
    }

    /**
     * 重新分配所有排队号（按时间段排序，同一时间段内按创建时间排序）
     */
    @Transactional
    public void reassignQueueNumbers(Long doctorId, LocalDate date) {
        LambdaQueryWrapper<Appointment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Appointment::getDoctorId, doctorId)
                .eq(Appointment::getAppointmentDate, date)
                .eq(Appointment::getStatus, 1)  // 只处理待就诊的
                .orderByAsc(Appointment::getTimeSlot)  // 按时段排序
                .orderByAsc(Appointment::getCreatedTime);  // 同一时段按创建时间排序

        List<Appointment> appointments = appointmentMapper.selectList(wrapper);

        // 按时段权重重新排序
        appointments.sort((a1, a2) -> {
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
     * 获取待诊队列列表（按时间段排序）
     */
    @GetMapping("/list")
    public ResultVo<List<QueueVo>> getQueueList(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer queueNo,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate queryDate) {

        Doctor doctor = getCurrentDoctor();

        final LocalDate finalQueryDate = queryDate != null ? queryDate : LocalDate.now();

        LambdaQueryWrapper<Appointment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Appointment::getDoctorId, doctor.getDoctorId())
                .eq(Appointment::getAppointmentDate, finalQueryDate);

        if (status != null) {
            wrapper.eq(Appointment::getStatus, status);
        }

        List<Appointment> appointments = appointmentMapper.selectList(wrapper);

        // 按时段和创建时间排序
        appointments.sort((a1, a2) -> {
            // 已支付的优先
            int paidCompare = Integer.compare(
                    a2.getPaid() != null ? a2.getPaid() : 0,
                    a1.getPaid() != null ? a1.getPaid() : 0);
            if (paidCompare != 0) {
                return paidCompare;
            }
            // 按时段排序
            int weight1 = getTimeSlotWeight(a1.getTimeSlot());
            int weight2 = getTimeSlotWeight(a2.getTimeSlot());
            if (weight1 != weight2) {
                return Integer.compare(weight1, weight2);
            }
            // 同一时段按创建时间排序
            return a1.getCreatedTime().compareTo(a2.getCreatedTime());
        });

        // 关键词搜索
        if (keyword != null && !keyword.trim().isEmpty()) {
            String kw = keyword.trim();
            Set<Long> matchedPatientIds = patientMapper.selectList(
                            new LambdaQueryWrapper<Patient>()
                                    .like(Patient::getName, kw)
                                    .or()
                                    .like(Patient::getPatientNo, kw))
                    .stream()
                    .map(Patient::getPatientId)
                    .collect(Collectors.toSet());
            appointments = appointments.stream()
                    .filter(a -> matchedPatientIds.contains(a.getPatientId()) ||
                            (a.getAppointmentNo() != null && a.getAppointmentNo().contains(kw)))
                    .collect(Collectors.toList());
        }

        if (queueNo != null) {
            appointments = appointments.stream()
                    .filter(a -> Objects.equals(a.getQueueNo(), queueNo))
                    .collect(Collectors.toList());
        }

        // 获取患者信息
        Set<Long> patientIds = appointments.stream()
                .map(Appointment::getPatientId)
                .collect(Collectors.toSet());
        Map<Long, Patient> patientMap = new HashMap<>();
        if (!patientIds.isEmpty()) {
            for (Patient p : patientMapper.selectBatchIds(patientIds)) {
                if (p != null) {
                    patientMap.put(p.getPatientId(), p);
                }
            }
        }

        final LocalDate dateForLambda = finalQueryDate;
        List<QueueVo> result = appointments.stream()
                .map(a -> toQueueVo(a, patientMap.get(a.getPatientId()), dateForLambda))
                .collect(Collectors.toList());

        return ResultVo.ok(result);
    }

    /**
     * 获取可用日期（今天和明天）
     */
    @GetMapping("/available-dates")
    public ResultVo<List<Map<String, Object>>> getAvailableDates() {
        Doctor doctor = getCurrentDoctor();
        List<Map<String, Object>> dateList = new ArrayList<>();

        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        long todayCount = appointmentMapper.selectCount(
                new LambdaQueryWrapper<Appointment>()
                        .eq(Appointment::getDoctorId, doctor.getDoctorId())
                        .eq(Appointment::getAppointmentDate, today));

        long tomorrowCount = appointmentMapper.selectCount(
                new LambdaQueryWrapper<Appointment>()
                        .eq(Appointment::getDoctorId, doctor.getDoctorId())
                        .eq(Appointment::getAppointmentDate, tomorrow));

        Map<String, Object> todayMap = new HashMap<>();
        todayMap.put("date", today.toString());
        todayMap.put("display", "今天");
        todayMap.put("weekday", getWeekday(today));
        todayMap.put("count", todayCount);
        todayMap.put("hasSchedule", todayCount > 0);
        dateList.add(todayMap);

        Map<String, Object> tomorrowMap = new HashMap<>();
        tomorrowMap.put("date", tomorrow.toString());
        tomorrowMap.put("display", "明天");
        tomorrowMap.put("weekday", getWeekday(tomorrow));
        tomorrowMap.put("count", tomorrowCount);
        tomorrowMap.put("hasSchedule", tomorrowCount > 0);
        dateList.add(tomorrowMap);

        return ResultVo.ok(dateList);
    }

    private String getWeekday(LocalDate date) {
        String[] weekdays = {"周日", "周一", "周二", "周三", "周四", "周五", "周六"};
        return weekdays[date.getDayOfWeek().getValue() % 7];
    }

    /**
     * 开始接诊
     */
    @PutMapping("/{appointmentId}/start")
    @Transactional
    public ResultVo<Void> startConsultation(@PathVariable Long appointmentId) {
        Appointment appointment = validateAndGetAppointment(appointmentId);

        if (appointment.getStatus() != 1) {
            throw new BusinessWarningException("只有待就诊状态的预约可以开始接诊");
        }
        if (appointment.getPaid() == null || appointment.getPaid() != 1) {
            throw new BusinessWarningException("患者未支付挂号费，请先完成支付");
        }

        appointment.setStatus(2);
        appointment.setUpdatedTime(LocalDateTime.now());
        appointmentMapper.updateById(appointment);

        return ResultVo.ok();
    }

    /**
     * 叫号 - 更新排队顺序，返回当前叫号信息
     */
    @PutMapping("/{appointmentId}/call")
    @Transactional
    public ResultVo<Map<String, Object>> callNext(@PathVariable Long appointmentId) {
        Appointment appointment = validateAndGetAppointment(appointmentId);
        Doctor doctor = getCurrentDoctor();
        LocalDate today = LocalDate.now();

        if (appointment.getStatus() != 1) {
            throw new BusinessWarningException("当前患者状态无法叫号");
        }

        if (appointment.getPaid() == null || appointment.getPaid() != 1) {
            throw new BusinessWarningException("患者未支付挂号费，无法叫号");
        }

        // 更新叫号时间（只更新时间，不改变状态，因为还没就诊）
        appointment.setUpdatedTime(LocalDateTime.now());
        appointmentMapper.updateById(appointment);

        Patient patient = patientMapper.selectById(appointment.getPatientId());

        // 获取所有待就诊且已支付的患者（按时段和创建时间排序）
        List<Appointment> waitingAppointments = getSortedWaitingAppointments(doctor.getDoctorId(), today);

        // 等候人数 = 待就诊且已支付的患者总数（包括当前被叫号的）
        // 注意：叫号后，当前患者还在等候队列中，直到开始接诊才移除
        long waitingCount = waitingAppointments.size();

        // 获取下一个待叫号的患者（排队号最小的，且不是当前叫号的）
        Appointment nextAppointment = null;
        for (Appointment a : waitingAppointments) {
            if (!a.getAppointmentId().equals(appointmentId)) {
                nextAppointment = a;
                break;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("currentCalled", toQueueVo(appointment, patient, today));

        if (nextAppointment != null) {
            Patient nextPatient = patientMapper.selectById(nextAppointment.getPatientId());
            result.put("nextWaiting", toQueueVo(nextAppointment, nextPatient, today));
        } else {
            result.put("nextWaiting", null);
        }

        result.put("waitingCount", waitingCount);

        return ResultVo.ok(result);
    }

    /**
     * 获取排序后的待就诊预约列表（按时段和创建时间）
     */
    private List<Appointment> getSortedWaitingAppointments(Long doctorId, LocalDate date) {
        LambdaQueryWrapper<Appointment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Appointment::getDoctorId, doctorId)
                .eq(Appointment::getAppointmentDate, date)
                .eq(Appointment::getStatus, 1)
                .eq(Appointment::getPaid, 1);

        List<Appointment> appointments = appointmentMapper.selectList(wrapper);

        // 按时段权重排序，同一时段按创建时间排序
        appointments.sort((a1, a2) -> {
            int weight1 = getTimeSlotWeight(a1.getTimeSlot());
            int weight2 = getTimeSlotWeight(a2.getTimeSlot());
            if (weight1 != weight2) {
                return Integer.compare(weight1, weight2);
            }
            return a1.getCreatedTime().compareTo(a2.getCreatedTime());
        });

        return appointments;
    }

    /**
     * 获取当前叫号信息
     */
    @GetMapping("/current-calling")
    public ResultVo<Map<String, Object>> getCurrentCalling(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate queryDate) {
        Doctor doctor = getCurrentDoctor();

        final LocalDate finalQueryDate = queryDate != null ? queryDate : LocalDate.now();

        // 获取所有待就诊且已支付的患者（按时段排序）
        List<Appointment> waitingAppointments = getSortedWaitingAppointments(doctor.getDoctorId(), finalQueryDate);

        Map<String, Object> result = new HashMap<>();

        if (!waitingAppointments.isEmpty()) {
            Appointment currentAppointment = waitingAppointments.get(0);
            Patient patient = patientMapper.selectById(currentAppointment.getPatientId());
            result.put("currentCalling", toQueueVo(currentAppointment, patient, finalQueryDate));

            if (waitingAppointments.size() > 1) {
                Appointment nextAppointment = waitingAppointments.get(1);
                Patient nextPatient = patientMapper.selectById(nextAppointment.getPatientId());
                result.put("nextWaiting", toQueueVo(nextAppointment, nextPatient, finalQueryDate));
            } else {
                result.put("nextWaiting", null);
            }
        } else {
            result.put("currentCalling", null);
            result.put("nextWaiting", null);
        }

        // 等候人数 = 待就诊且已支付的患者总数
        result.put("waitingCount", (long) waitingAppointments.size());

        return ResultVo.ok(result);
    }

    /**
     * 获取医生统计（支持日期）
     */
    @GetMapping("/stats")
    public ResultVo<Map<String, Object>> getTodayStats(
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate queryDate) {
        Doctor doctor = getCurrentDoctor();

        final LocalDate finalQueryDate = queryDate != null ? queryDate : LocalDate.now();

        LambdaQueryWrapper<Appointment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Appointment::getDoctorId, doctor.getDoctorId())
                .eq(Appointment::getAppointmentDate, finalQueryDate);

        List<Appointment> todayAppointments = appointmentMapper.selectList(wrapper);

        long total = todayAppointments.size();
        long waiting = todayAppointments.stream()
                .filter(a -> a.getStatus() == 1 && a.getPaid() != null && a.getPaid() == 1)
                .count();
        long unpaid = todayAppointments.stream()
                .filter(a -> a.getStatus() == 1 && (a.getPaid() == null || a.getPaid() == 0))
                .count();
        long completed = todayAppointments.stream()
                .filter(a -> a.getStatus() == 2)
                .count();
        long cancelled = todayAppointments.stream()
                .filter(a -> a.getStatus() == 3)
                .count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("waiting", waiting);
        stats.put("unpaid", unpaid);
        stats.put("completed", completed);
        stats.put("cancelled", cancelled);
        stats.put("queryDate", finalQueryDate.toString());

        return ResultVo.ok(stats);
    }

    /**
     * 医生取消预约
     */
    @PutMapping("/cancel/{appointmentId}")
    @Transactional
    public ResultVo<Void> doctorCancelAppointment(@PathVariable Long appointmentId) {
        Appointment appointment = validateAndGetAppointment(appointmentId);

        if (appointment.getStatus() != 1) {
            throw new BusinessWarningException("只有待就诊状态的预约可以取消");
        }

        appointment.setStatus(3);
        appointment.setUpdatedTime(LocalDateTime.now());
        appointmentMapper.updateById(appointment);

        // 取消后重新分配排队号
        Doctor doctor = getCurrentDoctor();
        reassignQueueNumbers(doctor.getDoctorId(), appointment.getAppointmentDate());

        return ResultVo.ok();
    }

    /**
     * 重新排序队列
     */
    @PutMapping("/resort")
    @Transactional
    public ResultVo<Void> resortQueue() {
        Doctor doctor = getCurrentDoctor();
        LocalDate today = LocalDate.now();
        reassignQueueNumbers(doctor.getDoctorId(), today);
        return ResultVo.ok();
    }

    private Appointment validateAndGetAppointment(Long appointmentId) {
        Appointment appointment = appointmentMapper.selectById(appointmentId);
        if (appointment == null) {
            throw new ServiceException("预约记录不存在");
        }

        Doctor doctor = getCurrentDoctor();
        if (!Objects.equals(appointment.getDoctorId(), doctor.getDoctorId())) {
            throw new ServiceException("无权操作此预约记录");
        }

        return appointment;
    }

    private QueueVo toQueueVo(Appointment appointment, Patient patient, LocalDate queryDate) {
        QueueVo vo = new QueueVo();
        vo.setAppointmentId(appointment.getAppointmentId());
        vo.setAppointmentNo(appointment.getAppointmentNo());
        vo.setPatientId(appointment.getPatientId());
        vo.setQueueNo(appointment.getQueueNo());
        vo.setTimeSlot(appointment.getTimeSlot());
        vo.setStatus(appointment.getStatus());
        vo.setPaid(appointment.getPaid());
        vo.setFeeAmount(appointment.getFeeAmount());
        vo.setCreatedTime(appointment.getCreatedTime());
        vo.setAppointmentDate(appointment.getAppointmentDate() != null
                ? appointment.getAppointmentDate().format(DATE_FORMATTER) : null);

        if (patient != null) {
            vo.setPatientName(patient.getName());
            vo.setPatientNo(patient.getPatientNo());
            vo.setGender(patient.getGender());
            vo.setAge(calculateAge(patient.getBirthDate()));
            vo.setPhone(patient.getPhone());
        }

        if (appointment.getStatus() == 1) {
            if (appointment.getPaid() != null && appointment.getPaid() == 1) {
                vo.setStatusText("待就诊");
            } else {
                vo.setStatusText("待支付");
            }
        } else if (appointment.getStatus() == 2) {
            vo.setStatusText("已就诊");
        } else if (appointment.getStatus() == 3) {
            vo.setStatusText("已取消");
        } else if (appointment.getStatus() == 4) {
            vo.setStatusText("爽约");
        }

        return vo;
    }

    private Integer calculateAge(LocalDate birthDate) {
        if (birthDate == null) return null;
        return LocalDate.now().getYear() - birthDate.getYear();
    }
}