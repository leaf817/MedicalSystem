package com.medical.service;

import com.medical.common.pagination.PageResult;
import com.medical.domain.dto.DoctorScheduleQueryDTO;
import com.medical.domain.vo.DoctorScheduleVO;

import java.util.Map;

public interface DoctorScheduleService {

    PageResult<DoctorScheduleVO> getDoctorSchedulePage(DoctorScheduleQueryDTO queryDTO);

    boolean updateScheduleStatus(Long scheduleId, Integer status);

    Long getDoctorIdByUsername(String username);

    Map<String, Object> getScheduleCalendar(Long doctorId, Integer year, Integer month);
}