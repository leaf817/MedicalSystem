// src/main/java/com/medical/service/impl/DoctorScheduleServiceImpl.java
package com.medical.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.medical.common.exception.BusinessWarningException;
import com.medical.common.pagination.PageResult;
import com.medical.domain.dto.DoctorScheduleQueryDTO;
import com.medical.domain.entity.Doctor;
import com.medical.domain.entity.Schedule;
import com.medical.domain.entity.SysUser;
import com.medical.domain.vo.DoctorScheduleVO;
import com.medical.mapper.DoctorMapper;
import com.medical.mapper.ScheduleMapper;
import com.medical.mapper.SysUserMapper;
import com.medical.service.DoctorScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DoctorScheduleServiceImpl implements DoctorScheduleService {

    private final ScheduleMapper scheduleMapper;
    private final DoctorMapper doctorMapper;
    private final SysUserMapper sysUserMapper;

    @Override
    public PageResult<DoctorScheduleVO> getDoctorSchedulePage(DoctorScheduleQueryDTO queryDTO) {
        // 查询医生的排班列表
        List<DoctorScheduleVO> allSchedules = getDoctorScheduleList(queryDTO.getDoctorId());

        // 按日期筛选
        if (queryDTO.getStartDate() != null) {
            allSchedules = allSchedules.stream()
                    .filter(s -> !s.getScheduleDate().isBefore(queryDTO.getStartDate()))
                    .collect(Collectors.toList());
        }
        if (queryDTO.getEndDate() != null) {
            allSchedules = allSchedules.stream()
                    .filter(s -> !s.getScheduleDate().isAfter(queryDTO.getEndDate()))
                    .collect(Collectors.toList());
        }
        if (queryDTO.getStatus() != null) {
            allSchedules = allSchedules.stream()
                    .filter(s -> s.getStatus().equals(queryDTO.getStatus()))
                    .collect(Collectors.toList());
        }

        // 手动分页
        int total = allSchedules.size();
        int start = (int) ((queryDTO.getPageNum() - 1) * queryDTO.getPageSize());
        int end = Math.min(start + queryDTO.getPageSize(), total);
        List<DoctorScheduleVO> records = total > start ? allSchedules.subList(start, end) : new ArrayList<>();

        // 使用你现有的 PageResult 类 - 注意使用 setList 而不是 setRecords
        PageResult<DoctorScheduleVO> result = new PageResult<>();
        result.setList(records);  // 改为 setList
        result.setTotal((long) total);
        result.setCurrentPage((long) queryDTO.getPageNum());
        result.setPageSize((long) queryDTO.getPageSize());

        return result;
    }

    private List<DoctorScheduleVO> getDoctorScheduleList(Long doctorId) {
        // 查询医生信息
        Doctor doctor = doctorMapper.selectOne(
                new LambdaQueryWrapper<Doctor>().eq(Doctor::getDoctorId, doctorId)
        );
        if (doctor == null) {
            return new ArrayList<>();
        }

        // 查询排班
        List<Schedule> schedules = scheduleMapper.selectList(
                new LambdaQueryWrapper<Schedule>()
                        .eq(Schedule::getDoctorId, doctorId)
                        .orderByDesc(Schedule::getScheduleDate)
        );

        // 转换为VO
        List<DoctorScheduleVO> voList = new ArrayList<>();
        for (Schedule schedule : schedules) {
            DoctorScheduleVO vo = new DoctorScheduleVO();
            BeanUtil.copyProperties(schedule, vo);
            vo.setDoctorName(doctor.getName());
            vo.setRemainingSlots(schedule.getTotalSlots() - schedule.getBookedSlots());
            vo.setStatusText(schedule.getStatus() == 1 ? "可预约" : "停诊");
            voList.add(vo);
        }

        return voList;
    }

    @Override
    @Transactional
    public boolean updateScheduleStatus(Long scheduleId, Integer status) {
        Schedule schedule = scheduleMapper.selectById(scheduleId);
        if (schedule == null) {
            throw new BusinessWarningException("排班不存在");
        }

        // 如果是停诊，检查是否有已预约的患者
        if (status == 0 && schedule.getBookedSlots() > 0) {
            throw new BusinessWarningException("该排班已有患者预约，无法停诊，请联系管理员处理");
        }

        schedule.setStatus(status);
        return scheduleMapper.updateById(schedule) > 0;
    }

    @Override
    public Long getDoctorIdByUsername(String username) {
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username)
        );
        if (user == null) {
            throw new BusinessWarningException("用户不存在");
        }

        Doctor doctor = doctorMapper.selectOne(
                new LambdaQueryWrapper<Doctor>().eq(Doctor::getUserId, user.getUserId())
        );
        if (doctor == null) {
            throw new BusinessWarningException("未找到对应的医生信息");
        }

        return doctor.getDoctorId();
    }

    @Override
    public Map<String, Object> getScheduleCalendar(Long doctorId, Integer year, Integer month) {
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        // 查询当月所有排班
        List<Schedule> schedules = scheduleMapper.selectList(
                new LambdaQueryWrapper<Schedule>()
                        .eq(Schedule::getDoctorId, doctorId)
                        .ge(Schedule::getScheduleDate, startDate)
                        .le(Schedule::getScheduleDate, endDate)
        );

        // 按日期分组
        Map<String, List<Schedule>> scheduleMap = schedules.stream()
                .collect(Collectors.groupingBy(s -> s.getScheduleDate().toString()));

        Map<String, Object> result = new HashMap<>();
        result.put("year", year);
        result.put("month", month);
        result.put("schedules", scheduleMap);

        return result;
    }
}