package com.medical.web.api.reception;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medical.common.exception.BusinessWarningException;
import com.medical.common.pagination.PageResult;
import com.medical.common.response.ResultVo;
import com.medical.domain.dto.ReceptionAppointmentCreateDto;
import com.medical.domain.entity.Appointment;
import com.medical.domain.entity.Doctor;
import com.medical.domain.entity.Patient;
import com.medical.domain.entity.SysDept;
import com.medical.domain.vo.AppointmentVo;
import com.medical.mapper.AppointmentMapper;
import com.medical.mapper.DoctorMapper;
import com.medical.mapper.PatientMapper;
import com.medical.mapper.SysDeptMapper;
import com.medical.service.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/reception/appointment")
@RequiredArgsConstructor
public class ReceptionAppointmentController {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final AppointmentService appointmentService;
    private final AppointmentMapper appointmentMapper;
    private final DoctorMapper doctorMapper;
    private final PatientMapper patientMapper;
    private final SysDeptMapper sysDeptMapper;

    @GetMapping("/page")
    public ResultVo<PageResult<AppointmentVo>> page(
            @RequestParam(value = "current", defaultValue = "1") Long current,
            @RequestParam(value = "size", defaultValue = "10") Long size,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "paid", required = false) Integer paid,
            @RequestParam(value = "patientId", required = false) Long patientId,
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
        if (patientId != null) {
            wrapper.eq(Appointment::getPatientId, patientId);
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
                return ResultVo.ok(emptyPage(current, size));
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

        List<AppointmentVo> list = toVoList(page.getRecords());
        list = list.stream().sorted(Comparator.comparingInt(this::getAppointmentOrder)).collect(Collectors.toList());

        PageResult<AppointmentVo> result = new PageResult<>();
        result.setCurrentPage(page.getCurrent());
        result.setPageSize(page.getSize());
        result.setTotal(page.getTotal());
        result.setList(list);
        return ResultVo.ok(result);
    }

    @PostMapping("/create")
    public ResultVo<Map<String, Object>> create(@Valid @RequestBody ReceptionAppointmentCreateDto dto) {
        Patient patient = patientMapper.selectById(dto.getPatientId());
        if (patient == null || patient.getStatus() == null || patient.getStatus() != 1) {
            throw new BusinessWarningException("请选择有效患者");
        }
        Appointment appointment = appointmentService.createAppointment(dto.getPatientId(), dto.getScheduleId());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("appointmentId", appointment.getAppointmentId());
        data.put("appointmentNo", appointment.getAppointmentNo());
        data.put("feeAmount", appointment.getFeeAmount());
        return ResultVo.ok(data);
    }

    @PutMapping("/{appointmentId}/cancel")
    public ResultVo<Void> cancel(@PathVariable Long appointmentId) {
        appointmentService.cancelAppointmentByStaff(appointmentId);
        return ResultVo.ok();
    }

    @PutMapping("/{appointmentId}/checkin")
    public ResultVo<Void> checkin(@PathVariable Long appointmentId) {
        appointmentService.checkinByStaff(appointmentId);
        return ResultVo.ok();
    }

    private PageResult<AppointmentVo> emptyPage(Long current, Long size) {
        PageResult<AppointmentVo> empty = new PageResult<>();
        empty.setCurrentPage(current);
        empty.setPageSize(size);
        empty.setTotal(0L);
        empty.setList(List.of());
        return empty;
    }

    private int getAppointmentOrder(AppointmentVo v) {
        if (v.getStatus() == 1 && v.getPaid() != null && v.getPaid() == 1) return 1;
        if (v.getStatus() == 1 && v.getPaid() != null && v.getPaid() == 0) return 2;
        if (v.getStatus() == 2) return 3;
        if (v.getStatus() == 4) return 4;
        if (v.getStatus() == 3) return 5;
        return 6;
    }

    private List<AppointmentVo> toVoList(List<Appointment> records) {
        if (records == null || records.isEmpty()) {
            return List.of();
        }
        Set<Long> doctorIds = records.stream().map(Appointment::getDoctorId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Doctor> doctorMap = new HashMap<>();
        if (!doctorIds.isEmpty()) {
            for (Doctor d : doctorMapper.selectBatchIds(doctorIds)) {
                if (d != null) {
                    doctorMap.put(d.getDoctorId(), d);
                }
            }
        }
        Set<Long> patientIds = records.stream().map(Appointment::getPatientId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Patient> patientMap = new HashMap<>();
        if (!patientIds.isEmpty()) {
            for (Patient p : patientMapper.selectBatchIds(patientIds)) {
                if (p != null) {
                    patientMap.put(p.getPatientId(), p);
                }
            }
        }
        Set<Long> deptIds = doctorMap.values().stream().map(Doctor::getDeptId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> deptNameMap = new HashMap<>();
        if (!deptIds.isEmpty()) {
            for (SysDept d : sysDeptMapper.selectBatchIds(deptIds)) {
                if (d != null) {
                    deptNameMap.put(d.getDeptId(), d.getName());
                }
            }
        }
        return records.stream()
                .map(a -> toVo(a, doctorMap, patientMap, deptNameMap))
                .collect(Collectors.toList());
    }

    private AppointmentVo toVo(Appointment a, Map<Long, Doctor> doctorMap,
                               Map<Long, Patient> patientMap, Map<Long, String> deptNameMap) {
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
        if (a.getStatus() != null && a.getStatus() == 1) {
            vo.setStatusText(a.getPaid() != null && a.getPaid() == 1 ? "待就诊" : "待支付");
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

    private String statusText(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case 1 -> "待就诊";
            case 2 -> "已就诊";
            case 3 -> "已取消";
            case 4 -> "爽约";
            default -> "未知";
        };
    }
}
