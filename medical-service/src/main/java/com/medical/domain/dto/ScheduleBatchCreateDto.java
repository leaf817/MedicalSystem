package com.medical.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class ScheduleBatchCreateDto {
    /**
     * 可选：科室多选（用于约束医生范围）
     */
    private List<Long> deptIds;

    @NotEmpty(message = "至少选择一个医生")
    private List<Long> doctorIds;

    @NotNull(message = "开始日期不能为空")
    private LocalDate startDate;

    @NotNull(message = "结束日期不能为空")
    private LocalDate endDate;

    @NotEmpty(message = "至少选择一个时段")
    private List<@Size(max = 20, message = "时段长度过长") String> timeSlots;

    @NotNull(message = "总号源不能为空")
    @Min(value = 1, message = "总号源至少为1")
    @Max(value = 999, message = "总号源不能超过999")
    private Integer totalSlots;

    @NotNull(message = "状态不能为空")
    @Min(value = 0, message = "状态值无效")
    @Max(value = 1, message = "状态值无效")
    private Integer status;

    @Size(max = 500, message = "备注过长")
    private String remark;
}
