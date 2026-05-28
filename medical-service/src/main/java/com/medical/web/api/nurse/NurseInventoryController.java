package com.medical.web.api.nurse;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.medical.common.pagination.PageResult;
import com.medical.common.response.ResultVo;
import com.medical.domain.dto.MedicineStockAdjustDto;
import com.medical.domain.dto.MedicineStockInboundDto;
import com.medical.domain.entity.Medicine;
import com.medical.domain.entity.MedicineCategory;
import com.medical.domain.entity.SysUser;
import com.medical.domain.vo.MedicineCategoryVo;
import com.medical.domain.vo.MedicineListVo;
import com.medical.domain.vo.MedicineStockLogVo;
import com.medical.mapper.MedicineCategoryMapper;
import com.medical.mapper.MedicineMapper;
import com.medical.mapper.SysUserMapper;
import com.medical.service.MedicineStockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Tag(name = "药房端-库存盘点")
@RestController
@RequestMapping("/api/nurse/inventory")
@RequiredArgsConstructor
public class NurseInventoryController {

    private final MedicineMapper medicineMapper;
    private final MedicineCategoryMapper medicineCategoryMapper;
    private final MedicineStockService medicineStockService;
    private final SysUserMapper sysUserMapper;

    @Operation(summary = "盘点用药品分页列表")
    @GetMapping("/medicine/page")
    public ResultVo<PageResult<MedicineListVo>> medicinePage(
            @RequestParam(value = "current", defaultValue = "1") Long current,
            @RequestParam(value = "size", defaultValue = "10") Long size,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "status", required = false) Integer status) {
        LambdaQueryWrapper<Medicine> wrapper = buildMedicineWrapper(keyword, categoryId, status);
        wrapper.orderByAsc(Medicine::getMedicineId);
        Page<Medicine> page = medicineMapper.selectPage(new Page<>(current, size), wrapper);
        return ResultVo.ok(toMedicinePage(page));
    }

    @Operation(summary = "药品分类（筛选）")
    @GetMapping("/medicine/categories")
    public ResultVo<List<MedicineCategoryVo>> categories() {
        List<MedicineCategory> list = medicineCategoryMapper.selectList(
                new LambdaQueryWrapper<MedicineCategory>()
                        .eq(MedicineCategory::getStatus, 1)
                        .orderByAsc(MedicineCategory::getSortOrder)
                        .orderByAsc(MedicineCategory::getCategoryId));
        List<MedicineCategoryVo> voList = list.stream().map(c -> {
            MedicineCategoryVo vo = new MedicineCategoryVo();
            vo.setCategoryId(c.getCategoryId());
            vo.setName(c.getName());
            return vo;
        }).collect(Collectors.toList());
        return ResultVo.ok(voList);
    }

    @Operation(summary = "盘点调整（按实盘数量校正账面）")
    @PutMapping("/medicine/{id}/adjust")
    public ResultVo<Void> adjust(
            @PathVariable("id") Long id,
            @Valid @RequestBody MedicineStockAdjustDto dto) {
        medicineStockService.adjustToActual(id, dto.getActualQuantity(), getCurrentUserId(), dto.getRemark());
        return ResultVo.ok();
    }

    @Operation(summary = "药品入库")
    @PostMapping("/medicine/{id}/inbound")
    public ResultVo<Void> inbound(
            @PathVariable("id") Long id,
            @Valid @RequestBody MedicineStockInboundDto dto) {
        medicineStockService.inbound(id, dto.getQuantity(), getCurrentUserId(), dto.getRemark());
        return ResultVo.ok();
    }

    @Operation(summary = "库存变动流水")
    @GetMapping("/stock-log/page")
    public ResultVo<PageResult<MedicineStockLogVo>> stockLogPage(
            @RequestParam(value = "current", defaultValue = "1") Long current,
            @RequestParam(value = "size", defaultValue = "10") Long size,
            @RequestParam(value = "medicineId", required = false) Long medicineId,
            @RequestParam(value = "bizType", required = false) String bizType,
            @RequestParam(value = "keyword", required = false) String keyword) {
        return ResultVo.ok(medicineStockService.pageLogs(current, size, medicineId, bizType, keyword));
    }

    @Operation(summary = "低库存药品数量")
    @GetMapping("/stock-warning/count")
    public ResultVo<Long> stockWarningCount() {
        return ResultVo.ok(medicineStockService.countStockWarning());
    }

    private LambdaQueryWrapper<Medicine> buildMedicineWrapper(String keyword, Long categoryId, Integer status) {
        LambdaQueryWrapper<Medicine> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(Medicine::getName, kw)
                    .or().like(Medicine::getCommonName, kw)
                    .or().like(Medicine::getMedicineCode, kw));
        }
        if (categoryId != null) {
            wrapper.eq(Medicine::getCategoryId, categoryId);
        }
        if (status != null) {
            wrapper.eq(Medicine::getStatus, status);
        }
        return wrapper;
    }

    private PageResult<MedicineListVo> toMedicinePage(Page<Medicine> page) {
        Set<Long> catIds = page.getRecords().stream()
                .map(Medicine::getCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> categoryNameMap = new HashMap<>();
        if (!catIds.isEmpty()) {
            for (MedicineCategory c : medicineCategoryMapper.selectBatchIds(catIds)) {
                categoryNameMap.put(c.getCategoryId(), c.getName());
            }
        }
        List<MedicineListVo> voList = page.getRecords().stream().map(m -> {
            MedicineListVo vo = new MedicineListVo();
            BeanUtils.copyProperties(m, vo);
            if (m.getCategoryId() != null) {
                vo.setCategoryName(categoryNameMap.get(m.getCategoryId()));
            }
            return vo;
        }).collect(Collectors.toList());

        PageResult<MedicineListVo> result = new PageResult<>();
        result.setCurrentPage(page.getCurrent());
        result.setPageSize(page.getSize());
        result.setTotal(page.getTotal());
        result.setList(voList);
        return result;
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, auth.getName()));
        return user != null ? user.getUserId() : null;
    }
}
