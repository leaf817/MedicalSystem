package com.medical.web.api.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medical.common.exception.BusinessWarningException;
import com.medical.common.exception.ServiceException;
import com.medical.common.pagination.PageResult;
import com.medical.common.response.ResultVo;
import com.medical.domain.entity.Appointment;
import com.medical.domain.entity.Doctor;
import com.medical.domain.entity.Patient;
import com.medical.domain.entity.SysDept;
import com.medical.domain.vo.AppointmentVo;
import com.medical.mapper.AppointmentMapper;
import com.medical.mapper.DoctorMapper;
import com.medical.mapper.PatientMapper;
import com.medical.mapper.SysDeptMapper;
import com.medical.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/appointment")
@RequiredArgsConstructor
public class AdminAppointmentController {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final AppointmentMapper appointmentMapper;
    private final DoctorMapper doctorMapper;
    private final PatientMapper patientMapper;
    private final SysDeptMapper sysDeptMapper;
    private final ScheduleService scheduleService;

    @GetMapping("/page")
    public ResultVo<PageResult<AppointmentVo>> page(
            @RequestParam(value = "current", defaultValue = "1") Long current,
            @RequestParam(value = "size", defaultValue = "10") Long size,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "paid", required = false) Integer paid,
            @RequestParam(value = "deptId", required = false) Long deptId,
            @RequestParam(value = "doctorId", required = false) Long doctorId,
            @RequestParam(value = "date", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        LambdaQueryWrapper<Appointment> wrapper = new LambdaQueryWrapper<>();
        if (status != null) {
            wrapper.eq(Appointment::getStatus, status);
        }
        if (paid != null) {
            wrapper.eq(Appointment::getPaid, paid);
        }
        if (doctorId != null) {
            wrapper.eq(Appointment::getDoctorId, doctorId);
        }
        if (deptId != null) {
            List<Long> doctorIdsInDept = doctorMapper.selectList(
                            new LambdaQueryWrapper<Doctor>()
                                    .select(Doctor::getDoctorId)
                                    .eq(Doctor::getDeptId, deptId))
                    .stream().map(Doctor::getDoctorId).filter(Objects::nonNull).collect(Collectors.toList());
            if (doctorIdsInDept.isEmpty()) {
                PageResult<AppointmentVo> empty = new PageResult<>();
                empty.setCurrentPage(current);
                empty.setPageSize(size);
                empty.setTotal(0L);
                empty.setList(List.of());
                return ResultVo.ok(empty);
            }
            wrapper.in(Appointment::getDoctorId, doctorIdsInDept);
        }
        if (date != null) {
            wrapper.eq(Appointment::getAppointmentDate, date);
        }
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(Appointment::getAppointmentNo, kw).or().like(Appointment::getTimeSlot, kw));
        }

        wrapper.orderByDesc(Appointment::getCreatedTime).orderByDesc(Appointment::getAppointmentId);
        Page<Appointment> page = appointmentMapper.selectPage(new Page<>(current, size), wrapper);

        Set<Long> doctorIds = page.getRecords().stream()
                .map(Appointment::getDoctorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Doctor> doctorMap = new HashMap<>();
        if (!doctorIds.isEmpty()) {
            for (Doctor d : doctorMapper.selectBatchIds(doctorIds)) {
                if (d != null) {
                    doctorMap.put(d.getDoctorId(), d);
                }
            }
        }
        Set<Long> patientIds = page.getRecords().stream()
                .map(Appointment::getPatientId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Patient> patientMap = new HashMap<>();
        if (!patientIds.isEmpty()) {
            for (Patient p : patientMapper.selectBatchIds(patientIds)) {
                if (p != null) {
                    patientMap.put(p.getPatientId(), p);
                }
            }
        }
        Set<Long> deptIds = doctorMap.values().stream()
                .map(Doctor::getDeptId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> deptNameMap = new HashMap<>();
        if (!deptIds.isEmpty()) {
            for (SysDept d : sysDeptMapper.selectBatchIds(deptIds)) {
                if (d != null) {
                    deptNameMap.put(d.getDeptId(), d.getName());
                }
            }
        }

        List<AppointmentVo> list = page.getRecords().stream()
                .map(a -> toVo(a, doctorMap, patientMap, deptNameMap))
                .collect(Collectors.toList());

        // 排序：待支付 > 待就诊 > 已就诊 > 爽约 > 已取消
        list = list.stream().sorted((o1, o2) -> {
            int order1 = getAppointmentOrder(o1);
            int order2 = getAppointmentOrder(o2);
            return Integer.compare(order1, order2);
        }).collect(Collectors.toList());

        PageResult<AppointmentVo> result = new PageResult<>();
        result.setCurrentPage(page.getCurrent());
        result.setPageSize(page.getSize());
        result.setTotal(page.getTotal());
        result.setList(list);
        return ResultVo.ok(result);
    }

    private int getAppointmentOrder(AppointmentVo v) {
        if (v.getStatus() == 1 && v.getPaid() != null && v.getPaid() == 1) return 1;  // 待就诊
        if (v.getStatus() == 1 && v.getPaid() != null && v.getPaid() == 0) return 2;  // 待支付
        if (v.getStatus() == 2) return 3;   // 已就诊
        if (v.getStatus() == 4) return 4;   // 爽约
        if (v.getStatus() == 3) return 5;   // 已取消
        return 6;
    }

    @GetMapping("/{appointmentId}")
    public ResultVo<AppointmentVo> detail(@PathVariable Long appointmentId) {
        Appointment a = appointmentMapper.selectById(appointmentId);
        if (a == null) {
            throw new ServiceException("预约记录不存在");
        }
        Doctor doctor = doctorMapper.selectById(a.getDoctorId());
        Patient patient = patientMapper.selectById(a.getPatientId());
        String deptName = null;
        if (doctor != null && doctor.getDeptId() != null) {
            SysDept dept = sysDeptMapper.selectById(doctor.getDeptId());
            if (dept != null) {
                deptName = dept.getName();
            }
        }
        Map<Long, Doctor> dMap = new HashMap<>();
        Map<Long, Patient> pMap = new HashMap<>();
        Map<Long, String> deptMap = new HashMap<>();
        if (doctor != null) {
            dMap.put(doctor.getDoctorId(), doctor);
            if (doctor.getDeptId() != null && deptName != null) {
                deptMap.put(doctor.getDeptId(), deptName);
            }
        }
        if (patient != null) {
            pMap.put(patient.getPatientId(), patient);
        }
        return ResultVo.ok(toVo(a, dMap, pMap, deptMap));
    }

    @PutMapping("/{appointmentId}/cancel")
    public ResultVo<Void> cancel(@PathVariable Long appointmentId) {
        Appointment a = appointmentMapper.selectById(appointmentId);
        if (a == null) {
            throw new ServiceException("预约记录不存在");
        }
        // 只要状态是待就诊(1)就可以取消，不限制支付状态
        if (a.getStatus() == null || a.getStatus() != 1) {
            throw new BusinessWarningException("只有待就诊状态的预约可以取消");
        }
        a.setStatus(3);
        a.setUpdatedTime(LocalDateTime.now());
        appointmentMapper.updateById(a);
        scheduleService.releaseSlot(a.getScheduleId());
        return ResultVo.ok();
    }

    private AppointmentVo toVo(Appointment a,
                               Map<Long, Doctor> doctorMap,
                               Map<Long, Patient> patientMap,
                               Map<Long, String> deptNameMap) {
        AppointmentVo vo = new AppointmentVo();
        vo.setAppointmentId(a.getAppointmentId());
        vo.setAppointmentNo(a.getAppointmentNo());
        vo.setPatientId(a.getPatientId());
        vo.setAppointmentDate(a.getAppointmentDate() != null ? a.getAppointmentDate().format(DATE_FORMATTER) : null);
        vo.setTimeSlot(a.getTimeSlot());
        vo.setQueueNo(a.getQueueNo());
        vo.setStatus(a.getStatus());
        vo.setFeeAmount(a.getFeeAmount());
        vo.setPaid(a.getPaid());
        vo.setCreatedTime(a.getCreatedTime());

        // 根据 status 和 paid 组合设置状态文本
        if (a.getStatus() != null && a.getStatus() == 1) {
            if (a.getPaid() != null && a.getPaid() == 1) {
                vo.setStatusText("待就诊");
            } else {
                vo.setStatusText("待支付");
            }
        } else {
            vo.setStatusText(statusText(a.getStatus()));
        }

        Doctor d = doctorMap.get(a.getDoctorId());
        if (d != null) {
            vo.setDoctorName(d.getName());
            vo.setDoctorTitle(d.getTitle());
            if (d.getDeptId() != null) {
                vo.setDeptName(deptNameMap.get(d.getDeptId()));
            }
        }
        Patient p = patientMap.get(a.getPatientId());
        if (p != null) {
            vo.setPatientName(p.getName());
        }
        return vo;
    }

    /**
     * 签到
     */
    /**
     * 签到 - 分配排队号（按天重置）
     */
    @PutMapping("/{appointmentId}/checkin")
    public ResultVo<Void> checkin(@PathVariable Long appointmentId) {
        Appointment a = appointmentMapper.selectById(appointmentId);
        if (a == null) {
            throw new ServiceException("预约记录不存在");
        }
        if (a.getStatus() != 1) {
            throw new BusinessWarningException("只有待就诊状态的预约可以签到");
        }
        if (a.getPaid() == null || a.getPaid() != 1) {
            throw new BusinessWarningException("请先支付挂号费");
        }

        // 已分配过排队号则不再重复分配
        if (a.getQueueNo() != null && a.getQueueNo() > 0) {
            throw new BusinessWarningException("该预约已完成签到，排队号为：" + a.getQueueNo());
        }

        // 查询当前医生、当前日期、当前时段最大的排队号
        Integer maxQueueNo = (Integer) appointmentMapper.selectObjs(
                new LambdaQueryWrapper<Appointment>()
                        .eq(Appointment::getDoctorId, a.getDoctorId())
                        .eq(Appointment::getAppointmentDate, a.getAppointmentDate())  // 关键：加上日期条件
                        .eq(Appointment::getTimeSlot, a.getTimeSlot())
                        .eq(Appointment::getStatus, 1)  // 只统计待就诊状态
                        .isNotNull(Appointment::getQueueNo)
                        .select(Appointment::getQueueNo)
                        .orderByDesc(Appointment::getQueueNo)
                        .last("limit 1")
        ).stream().findFirst().orElse(null);

        int newQueueNo = (maxQueueNo == null ? 1 : maxQueueNo + 1);

        a.setQueueNo(newQueueNo);
        a.setCheckInTime(LocalDateTime.now());
        a.setUpdatedTime(LocalDateTime.now());

        appointmentMapper.updateById(a);

        return ResultVo.ok();
    }



    private String statusText(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 1:
                return "待就诊";
            case 2:
                return "已就诊";
            case 3:
                return "已取消";
            case 4:
                return "爽约";
            default:
                return "未知";
        }
    }
}