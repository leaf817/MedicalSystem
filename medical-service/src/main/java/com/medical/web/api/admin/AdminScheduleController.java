package com.medical.web.api.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medical.common.exception.BusinessWarningException;
import com.medical.common.exception.ServiceException;
import com.medical.common.pagination.PageResult;
import com.medical.common.response.ResultVo;
import com.medical.domain.dto.ScheduleBatchCreateDto;
import com.medical.domain.dto.ScheduleSaveDto;
import com.medical.domain.entity.Appointment;
import com.medical.domain.entity.Doctor;
import com.medical.domain.entity.Schedule;
import com.medical.domain.entity.SysDept;
import com.medical.domain.entity.SysUser;
import com.medical.domain.vo.ScheduleListVo;
import com.medical.mapper.AppointmentMapper;
import com.medical.mapper.DoctorMapper;
import com.medical.mapper.ScheduleMapper;
import com.medical.mapper.SysDeptMapper;
import com.medical.mapper.SysRoleMapper;
import com.medical.mapper.SysUserMapper;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/admin/schedule")
@RequiredArgsConstructor
public class AdminScheduleController {

    private final ScheduleMapper scheduleMapper;
    private final DoctorMapper doctorMapper;
    private final SysDeptMapper sysDeptMapper;
    private final AppointmentMapper appointmentMapper;
    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;

    /**
     * 判断当前用户是否是管理员
     */
    private boolean isAdmin(SysUser user) {
        if (user == null || user.getUserId() == null) {
            return false;
        }
        List<String> roleCodes = sysRoleMapper.selectRoleCodesByUserId(user.getUserId());
        return roleCodes != null && roleCodes.stream()
                .anyMatch(code -> "ADMIN".equals(code) || "SUPER_ADMIN".equals(code));
    }

    /**
     * 获取当前登录用户
     */
    private SysUser getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new BusinessWarningException("请先登录");
        }
        String username = auth.getName();

        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username)
        );
        if (user == null) {
            throw new BusinessWarningException("用户不存在");
        }
        return user;
    }

    /**
     * 获取当前登录医生的医生ID（仅医生角色使用）
     */
    private Long getCurrentDoctorId(SysUser user) {
        Doctor doctor = doctorMapper.selectOne(
                new LambdaQueryWrapper<Doctor>().eq(Doctor::getUserId, user.getUserId())
        );
        if (doctor == null) {
            throw new BusinessWarningException("医生档案不存在");
        }
        return doctor.getDoctorId();
    }

    @GetMapping("/list")
    public ResultVo<PageResult<ScheduleListVo>> list(
            @RequestParam(value = "current", defaultValue = "1") Long current,
            @RequestParam(value = "size", defaultValue = "10") Long size,
            @RequestParam(value = "deptId", required = false) Long deptId,
            @RequestParam(value = "doctorId", required = false) Long doctorId,
            @RequestParam(value = "date", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date,
            @RequestParam(value = "status", required = false) Integer status) {

        // 获取当前登录用户
        SysUser currentUser = getCurrentUser();
        boolean isAdmin = isAdmin(currentUser);

        log.info("当前用户: {}, 是否管理员: {}", currentUser.getUsername(), isAdmin);

        LambdaQueryWrapper<Schedule> wrapper = new LambdaQueryWrapper<>();

        if (isAdmin) {
            // 管理员：可以查询所有排班
            log.info("管理员 {} 查询排班", currentUser.getUsername());

            // 如果前端传入了doctorId，则按医生筛选
            if (doctorId != null) {
                wrapper.eq(Schedule::getDoctorId, doctorId);
            }
        } else {
            // 非管理员（医生）：只能查询自己的排班
            log.info("医生 {} 查询自己的排班", currentUser.getUsername());

            Long currentDoctorId = getCurrentDoctorId(currentUser);
            wrapper.eq(Schedule::getDoctorId, currentDoctorId);
        }

        // 科室筛选
        if (deptId != null) {
            wrapper.eq(Schedule::getDeptId, deptId);
        }

        // 日期筛选
        if (date != null) {
            wrapper.eq(Schedule::getScheduleDate, date);
        }

        // 状态筛选
        if (status != null) {
            wrapper.eq(Schedule::getStatus, status);
        }

        // 排序
        wrapper.orderByDesc(Schedule::getScheduleDate)
                .orderByAsc(Schedule::getTimeSlot)
                .orderByAsc(Schedule::getScheduleId);

        // 分页查询
        Page<Schedule> page = scheduleMapper.selectPage(new Page<>(current, size), wrapper);

        // 获取医生信息
        Set<Long> doctorIds = page.getRecords().stream()
                .map(Schedule::getDoctorId)
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

        // 获取科室信息
        Set<Long> deptIds = page.getRecords().stream()
                .map(Schedule::getDeptId)
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

        // 转换为VO
        List<ScheduleListVo> list = page.getRecords().stream().map(s -> {
            ScheduleListVo vo = new ScheduleListVo();
            vo.setScheduleId(s.getScheduleId());
            vo.setDoctorId(s.getDoctorId());
            vo.setDeptId(s.getDeptId());
            vo.setDeptName(s.getDeptId() != null ? deptNameMap.get(s.getDeptId()) : null);

            Doctor doctor = doctorMap.get(s.getDoctorId());
            if (doctor != null) {
                vo.setDoctorName(doctor.getName());
                vo.setDoctorTitle(doctor.getTitle());
            }

            vo.setScheduleDate(s.getScheduleDate());
            vo.setScheduleTimeSlot(s.getTimeSlot());
            vo.setTotalSlots(s.getTotalSlots());
            vo.setBookedSlots(s.getBookedSlots());

            int totalSlots = s.getTotalSlots() != null ? s.getTotalSlots() : 0;
            int bookedSlots = s.getBookedSlots() != null ? s.getBookedSlots() : 0;
            vo.setRemainingSlots(Math.max(0, totalSlots - bookedSlots));

            vo.setStatus(s.getStatus());
            vo.setStatusText(s.getStatus() != null && s.getStatus() == 1 ? "可预约" : "停诊");
            vo.setRemark(s.getRemark());
            vo.setCreatedTime(s.getCreatedTime());
            return vo;
        }).collect(Collectors.toList());

        PageResult<ScheduleListVo> result = new PageResult<>();
        result.setCurrentPage(page.getCurrent());
        result.setPageSize(page.getSize());
        result.setTotal(page.getTotal());
        result.setList(list);

        return ResultVo.ok(result);
    }

    @PostMapping
    public ResultVo<Void> create(@Valid @RequestBody ScheduleSaveDto dto) {
        Doctor doctor = doctorMapper.selectById(dto.getDoctorId());
        if (doctor == null || doctor.getStatus() == null || doctor.getStatus() != 1) {
            throw new BusinessWarningException("医生不存在或已停用");
        }
        if (doctor.getDeptId() == null) {
            throw new BusinessWarningException("该医生未绑定科室，无法排班");
        }
        validateUnique(dto.getDoctorId(), dto.getScheduleDate(), dto.getTimeSlot(), null);

        LocalDateTime now = LocalDateTime.now();
        Schedule s = new Schedule();
        s.setDoctorId(dto.getDoctorId());
        s.setDeptId(doctor.getDeptId());
        s.setScheduleDate(dto.getScheduleDate());
        s.setTimeSlot(dto.getTimeSlot().trim());
        s.setTotalSlots(dto.getTotalSlots());
        s.setBookedSlots(0);
        s.setStatus(dto.getStatus());
        s.setRemark(StringUtils.hasText(dto.getRemark()) ? dto.getRemark().trim() : null);
        s.setCreatedTime(now);
        s.setUpdatedTime(now);
        scheduleMapper.insert(s);
        return ResultVo.ok();
    }

    /**
     * 一键排班：按日期区间和时段批量生成，已存在的排班自动跳过
     */
    @PostMapping("/batch")
    public ResultVo<Map<String, Object>> batchCreate(@Valid @RequestBody ScheduleBatchCreateDto dto) {
        if (dto.getEndDate().isBefore(dto.getStartDate())) {
            throw new BusinessWarningException("结束日期不能早于开始日期");
        }
        long days = ChronoUnit.DAYS.between(dto.getStartDate(), dto.getEndDate()) + 1;
        if (days > 60) {
            throw new BusinessWarningException("日期区间不能超过60天");
        }

        Set<String> slotSet = new LinkedHashSet<>();
        for (String slot : dto.getTimeSlots()) {
            if (StringUtils.hasText(slot)) {
                slotSet.add(slot.trim());
            }
        }
        if (slotSet.isEmpty()) {
            throw new BusinessWarningException("至少选择一个有效时段");
        }

        List<Long> doctorIds = dto.getDoctorIds().stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (doctorIds.isEmpty()) {
            throw new BusinessWarningException("至少选择一个医生");
        }

        List<Doctor> doctors = doctorMapper.selectBatchIds(doctorIds);
        Map<Long, Doctor> doctorMap = doctors.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Doctor::getDoctorId, d -> d));
        if (doctorMap.size() != doctorIds.size()) {
            throw new BusinessWarningException("存在无效医生，请刷新后重试");
        }

        Set<Long> deptLimit = dto.getDeptIds() == null ? Set.of() :
                dto.getDeptIds().stream().filter(Objects::nonNull).collect(Collectors.toSet());

        for (Long doctorId : doctorIds) {
            Doctor doctor = doctorMap.get(doctorId);
            if (doctor.getStatus() == null || doctor.getStatus() != 1) {
                throw new BusinessWarningException("存在已停用医生，无法一键排班");
            }
            if (doctor.getDeptId() == null) {
                throw new BusinessWarningException("存在未绑定科室医生，无法一键排班");
            }
            if (!deptLimit.isEmpty() && !deptLimit.contains(doctor.getDeptId())) {
                throw new BusinessWarningException("所选医生不在所选科室范围内，请检查后重试");
            }
        }

        LocalDateTime now = LocalDateTime.now();
        int createdCount = 0;
        int skippedCount = 0;
        List<String> createdItems = new ArrayList<>();

        for (Long doctorId : doctorIds) {
            Doctor doctor = doctorMap.get(doctorId);
            for (LocalDate date = dto.getStartDate(); !date.isAfter(dto.getEndDate()); date = date.plusDays(1)) {
                for (String slot : slotSet) {
                    long exists = scheduleMapper.selectCount(new LambdaQueryWrapper<Schedule>()
                            .eq(Schedule::getDoctorId, doctorId)
                            .eq(Schedule::getScheduleDate, date)
                            .eq(Schedule::getTimeSlot, slot));
                    if (exists > 0) {
                        skippedCount++;
                        continue;
                    }

                    Schedule s = new Schedule();
                    s.setDoctorId(doctorId);
                    s.setDeptId(doctor.getDeptId());
                    s.setScheduleDate(date);
                    s.setTimeSlot(slot);
                    s.setTotalSlots(dto.getTotalSlots());
                    s.setBookedSlots(0);
                    s.setStatus(dto.getStatus());
                    s.setRemark(StringUtils.hasText(dto.getRemark()) ? dto.getRemark().trim() : null);
                    s.setCreatedTime(now);
                    s.setUpdatedTime(now);
                    scheduleMapper.insert(s);

                    createdCount++;
                    createdItems.add(doctor.getName() + " " + date + " " + slot);
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("createdCount", createdCount);
        result.put("skippedCount", skippedCount);
        result.put("createdItems", createdItems);
        return ResultVo.ok(result);
    }

    @PutMapping("/{id}")
    public ResultVo<Void> update(@PathVariable("id") Long id, @Valid @RequestBody ScheduleSaveDto dto) {
        Schedule exist = scheduleMapper.selectById(id);
        if (exist == null) {
            throw new ServiceException("排班不存在");
        }
        Doctor doctor = doctorMapper.selectById(dto.getDoctorId());
        if (doctor == null || doctor.getStatus() == null || doctor.getStatus() != 1) {
            throw new BusinessWarningException("医生不存在或已停用");
        }
        if (doctor.getDeptId() == null) {
            throw new BusinessWarningException("该医生未绑定科室，无法排班");
        }
        validateUnique(dto.getDoctorId(), dto.getScheduleDate(), dto.getTimeSlot(), id);

        int newTotal = dto.getTotalSlots();
        int booked = exist.getBookedSlots() != null ? exist.getBookedSlots() : 0;
        if (newTotal < booked) {
            throw new BusinessWarningException("总号源不能小于已预约数（" + booked + "）");
        }

        exist.setDoctorId(dto.getDoctorId());
        exist.setDeptId(doctor.getDeptId());
        exist.setScheduleDate(dto.getScheduleDate());
        exist.setTimeSlot(dto.getTimeSlot().trim());
        exist.setTotalSlots(newTotal);
        exist.setStatus(dto.getStatus());
        exist.setRemark(StringUtils.hasText(dto.getRemark()) ? dto.getRemark().trim() : null);
        exist.setUpdatedTime(LocalDateTime.now());
        scheduleMapper.updateById(exist);
        return ResultVo.ok();
    }

    @DeleteMapping("/{id}")
    public ResultVo<Void> delete(@PathVariable("id") Long id) {
        Schedule exist = scheduleMapper.selectById(id);
        if (exist == null) {
            throw new ServiceException("排班不存在");
        }
        long activeAppointments = appointmentMapper.selectCount(
                new LambdaQueryWrapper<Appointment>()
                        .eq(Appointment::getScheduleId, id)
                        .in(Appointment::getStatus, List.of(1, 2))
        );
        if (activeAppointments > 0) {
            throw new BusinessWarningException("该排班下存在待就诊/已就诊预约，无法删除");
        }
        scheduleMapper.deleteById(id);
        return ResultVo.ok();
    }

    /**
     * 一键停用：已过期且无有效预约（待就诊/已就诊）的排班
     */
    @PutMapping("/disable-expired")
    public ResultVo<Map<String, Integer>> disableExpiredSchedules() {
        LocalDate today = LocalDate.now();
        LocalTime nowTime = LocalTime.now();

        List<Schedule> candidates = scheduleMapper.selectList(
                new LambdaQueryWrapper<Schedule>()
                        .eq(Schedule::getStatus, 1)
                        .le(Schedule::getScheduleDate, today)
        );

        int disabledCount = 0;
        int skippedWithAppointments = 0;

        for (Schedule schedule : candidates) {
            if (!isExpired(schedule, today, nowTime)) {
                continue;
            }
            long activeAppointments = appointmentMapper.selectCount(
                    new LambdaQueryWrapper<Appointment>()
                            .eq(Appointment::getScheduleId, schedule.getScheduleId())
                            .in(Appointment::getStatus, List.of(1, 2))
            );
            if (activeAppointments > 0) {
                skippedWithAppointments++;
                continue;
            }
            schedule.setStatus(0);
            schedule.setUpdatedTime(LocalDateTime.now());
            scheduleMapper.updateById(schedule);
            disabledCount++;
        }

        Map<String, Integer> result = new HashMap<>();
        result.put("disabledCount", disabledCount);
        result.put("skippedWithAppointments", skippedWithAppointments);
        return ResultVo.ok(result);
    }

    private boolean isExpired(Schedule schedule, LocalDate today, LocalTime nowTime) {
        if (schedule.getScheduleDate() == null) {
            return false;
        }
        if (schedule.getScheduleDate().isBefore(today)) {
            return true;
        }
        if (!schedule.getScheduleDate().isEqual(today)) {
            return false;
        }
        LocalTime endTime = extractEndTime(schedule.getTimeSlot());
        return endTime != null && !endTime.isAfter(nowTime);
    }

    private LocalTime extractEndTime(String timeSlot) {
        if (!StringUtils.hasText(timeSlot) || !timeSlot.contains("-")) {
            return null;
        }
        String[] parts = timeSlot.trim().split("-");
        if (parts.length != 2) {
            return null;
        }
        try {
            return LocalTime.parse(parts[1].trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private void validateUnique(Long doctorId, LocalDate date, String timeSlot, Long ignoreId) {
        String normalizedSlot = timeSlot == null ? null : timeSlot.trim();
        LambdaQueryWrapper<Schedule> w = new LambdaQueryWrapper<Schedule>()
                .eq(Schedule::getDoctorId, doctorId)
                .eq(Schedule::getScheduleDate, date)
                .eq(Schedule::getTimeSlot, normalizedSlot);
        if (ignoreId != null) {
            w.ne(Schedule::getScheduleId, ignoreId);
        }
        long dup = scheduleMapper.selectCount(w);
        if (dup > 0) {
            throw new BusinessWarningException("该医生在该日期该时段已存在排班");
        }
    }
}