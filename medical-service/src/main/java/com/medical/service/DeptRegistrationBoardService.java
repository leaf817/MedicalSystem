package com.medical.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.medical.common.exception.BusinessWarningException;
import com.medical.common.exception.ServiceException;
import com.medical.domain.dto.DeptAlertRuleSaveDto;
import com.medical.domain.entity.Appointment;
import com.medical.domain.entity.DeptRegistrationAlertRule;
import com.medical.domain.entity.Doctor;
import com.medical.domain.entity.MedicalRecord;
import com.medical.domain.entity.SysDept;
import com.medical.domain.vo.DeptAlertRuleVo;
import com.medical.domain.vo.DeptEpidemicHintVo;
import com.medical.domain.vo.DeptRegistrationAlertVo;
import com.medical.domain.vo.DeptRegistrationBoardVo;
import com.medical.domain.vo.DeptRegistrationStatVo;
import com.medical.domain.vo.DeptRegistrationTrendPointVo;
import com.medical.mapper.AppointmentMapper;
import com.medical.mapper.DeptRegistrationAlertRuleMapper;
import com.medical.mapper.DoctorMapper;
import com.medical.mapper.MedicalRecordMapper;
import com.medical.mapper.SysDeptMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeptRegistrationBoardService {

    private static final int MIN_BASELINE_DAYS = 3;

    private final AppointmentMapper appointmentMapper;
    private final DoctorMapper doctorMapper;
    private final SysDeptMapper sysDeptMapper;
    private final MedicalRecordMapper medicalRecordMapper;
    private final DeptRegistrationAlertRuleMapper alertRuleMapper;

    public DeptRegistrationBoardVo getBoard(LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now();
        if (from == null) {
            from = today.minusDays(6);
        }
        if (to == null) {
            to = today;
        }
        if (from.isAfter(to)) {
            throw new BusinessWarningException("开始日期不能晚于结束日期");
        }

        Map<Long, DeptMetrics> metricsMap = buildMetricsMap(today);
        Map<Long, Long> doctorDeptMap = buildDoctorDeptMap();
        Map<Long, Long> periodCountMap = countByDept(from, to, doctorDeptMap);

        DeptRegistrationBoardVo board = new DeptRegistrationBoardVo();
        board.setDeptStats(buildDeptStats(periodCountMap, metricsMap));
        board.setAlerts(evaluateAlerts(metricsMap));
        board.setEpidemicHints(buildEpidemicHints(metricsMap));
        return board;
    }

    public List<DeptRegistrationTrendPointVo> getTrend(Long deptId, int days, int futureDays) {
        if (deptId == null) {
            throw new BusinessWarningException("请选择科室");
        }
        SysDept dept = sysDeptMapper.selectById(deptId);
        if (dept == null) {
            throw new ServiceException("科室不存在");
        }
        int span = Math.max(1, Math.min(days, 90));
        int futureSpan = Math.max(0, Math.min(futureDays, 14));
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(span - 1L);
        LocalDate end = today.plusDays(futureSpan);

        Map<Long, Long> doctorDeptMap = buildDoctorDeptMap();
        Set<Long> doctorIds = doctorDeptMap.entrySet().stream()
                .filter(e -> deptId.equals(e.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        Map<LocalDate, Long> countByDate = new HashMap<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            countByDate.put(d, 0L);
        }

        if (!doctorIds.isEmpty()) {
            List<Appointment> appointments = appointmentMapper.selectList(
                    new LambdaQueryWrapper<Appointment>()
                            .in(Appointment::getDoctorId, doctorIds)
                            .ge(Appointment::getAppointmentDate, start)
                            .le(Appointment::getAppointmentDate, end)
                            .ne(Appointment::getStatus, 3));
            for (Appointment a : appointments) {
                if (a.getAppointmentDate() != null) {
                    countByDate.merge(a.getAppointmentDate(), 1L, Long::sum);
                }
            }
        }

        List<DeptRegistrationTrendPointVo> trend = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            DeptRegistrationTrendPointVo point = new DeptRegistrationTrendPointVo();
            point.setDate(d.toString());
            point.setCount(countByDate.getOrDefault(d, 0L));
            String periodType = resolvePeriodType(d, today);
            point.setPeriodType(periodType);
            point.setPeriodTypeLabel(periodTypeLabel(periodType));
            trend.add(point);
        }
        return trend;
    }

    public List<DeptRegistrationAlertVo> evaluateAlerts() {
        return evaluateAlerts(buildMetricsMap(LocalDate.now()));
    }

    private List<DeptRegistrationAlertVo> evaluateAlerts(Map<Long, DeptMetrics> metricsMap) {
        List<DeptRegistrationAlertRule> rules = alertRuleMapper.selectList(
                new LambdaQueryWrapper<DeptRegistrationAlertRule>()
                        .eq(DeptRegistrationAlertRule::getEnabled, 1));
        if (rules.isEmpty()) {
            return List.of();
        }

        Map<Long, String> deptNameMap = loadDeptNameMap();

        List<DeptRegistrationAlertVo> alerts = new ArrayList<>();
        for (DeptRegistrationAlertRule rule : rules) {
            DeptMetrics metrics = metricsMap.get(rule.getDeptId());
            if (metrics == null) {
                continue;
            }
            int thresholdType = rule.getThresholdType() != null ? rule.getThresholdType() : 1;
            AlertEvaluation evaluation = evaluateRule(rule, metrics, deptNameMap.getOrDefault(rule.getDeptId(), "科室"), thresholdType);
            if (evaluation == null) {
                continue;
            }

            DeptRegistrationAlertVo vo = new DeptRegistrationAlertVo();
            vo.setRuleId(rule.getRuleId());
            vo.setDeptId(rule.getDeptId());
            vo.setDeptName(deptNameMap.get(rule.getDeptId()));
            vo.setRuleName(rule.getRuleName());
            vo.setAlertLevel(rule.getAlertLevel());
            vo.setAlertLevelLabel(alertLevelLabel(rule.getAlertLevel()));
            vo.setMessage(evaluation.message);
            vo.setCurrentValue(evaluation.currentValue);
            vo.setThresholdValue(rule.getThresholdValue());
            vo.setThresholdType(rule.getThresholdType());
            alerts.add(vo);
        }

        alerts.sort(Comparator
                .comparing(DeptRegistrationAlertVo::getAlertLevel, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(DeptRegistrationAlertVo::getDeptName, Comparator.nullsLast(String::compareTo)));
        return alerts;
    }

    public List<DeptEpidemicHintVo> buildEpidemicHints() {
        return buildEpidemicHints(buildMetricsMap(LocalDate.now()));
    }

    private List<DeptEpidemicHintVo> buildEpidemicHints(Map<Long, DeptMetrics> metricsMap) {
        LocalDate today = LocalDate.now();
        Map<Long, SysDept> deptMap = loadActiveDeptMap();
        Map<Long, DiagnosisSignals> diagnosisMap = buildDiagnosisSignals(today.minusDays(6), today);

        List<DeptEpidemicHintVo> hints = new ArrayList<>();
        for (SysDept dept : deptMap.values()) {
            DeptMetrics metrics = metricsMap.get(dept.getDeptId());
            if (metrics == null || metrics.todayCount <= 0) {
                continue;
            }
            DiagnosisSignals signals = diagnosisMap.getOrDefault(dept.getDeptId(), DiagnosisSignals.empty());
            String code = dept.getCode() != null ? dept.getCode() : "";
            String name = dept.getName() != null ? dept.getName() : "";

            boolean pediatric = "PEDIATRICIAN".equalsIgnoreCase(code) || name.contains("儿科");
            boolean respiratory = "INTERNIST".equalsIgnoreCase(code) || "DOCTOR".equalsIgnoreCase(code)
                    || name.contains("内科") || name.contains("通科");
            boolean surge = metrics.changePercent != null && metrics.changePercent.compareTo(BigDecimal.valueOf(30)) >= 0;
            boolean fluSeason = isFluSeason(today.getMonth());

            if (pediatric && (surge || signals.chickenpoxHits >= 2)) {
                int confidence = (surge && signals.chickenpoxHits >= 2) ? 3
                        : (signals.chickenpoxHits >= 1 || surge ? 2 : 1);
                hints.add(buildHint(dept, "CHICKENPOX", "疑似水痘/皮疹聚集",
                        String.format("%s 近期挂号量上升（今日 %d 例），且诊断/主诉中出现皮疹、水痘相关描述 %d 次，建议关注儿童传染性疾病。",
                                name, metrics.todayCount, signals.chickenpoxHits),
                        confidence));
            }

            if (respiratory && fluSeason && (surge || signals.fluHits >= 2)) {
                int confidence = signals.fluHits >= 2 && surge ? 3 : (surge || signals.fluHits >= 2 ? 2 : 1);
                hints.add(buildHint(dept, "FLU", "疑似流感季就诊高峰",
                        String.format("%s 处于呼吸道高发季节，今日挂号 %d 例，发热/咳嗽/流感相关描述 %d 次，建议加强预检分诊与防护提醒。",
                                name, metrics.todayCount, signals.fluHits),
                        confidence));
            }

            if (!pediatric && !respiratory && surge && metrics.todayCount >= 20) {
                hints.add(buildHint(dept, "GENERAL", "就诊量异常升高",
                        String.format("%s 今日挂号 %d 例，较近7日均值增幅 %.1f%%，建议排查是否存在局部聚集性就诊。",
                                name, metrics.todayCount,
                                metrics.changePercent != null ? metrics.changePercent : BigDecimal.ZERO),
                        1));
            }
        }

        hints.sort(Comparator
                .comparing(DeptEpidemicHintVo::getConfidence, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(DeptEpidemicHintVo::getDeptName, Comparator.nullsLast(String::compareTo)));
        return hints;
    }

    public List<DeptAlertRuleVo> listAlertRules() {
        List<DeptRegistrationAlertRule> rules = alertRuleMapper.selectList(
                new LambdaQueryWrapper<DeptRegistrationAlertRule>()
                        .orderByDesc(DeptRegistrationAlertRule::getRuleId));
        Map<Long, String> deptNameMap = loadDeptNameMap();
        return rules.stream().map(r -> toRuleVo(r, deptNameMap)).collect(Collectors.toList());
    }

    public void createAlertRule(DeptAlertRuleSaveDto dto) {
        validateDept(dto.getDeptId());
        LocalDateTime now = LocalDateTime.now();
        DeptRegistrationAlertRule rule = new DeptRegistrationAlertRule();
        applyRuleDto(rule, dto);
        rule.setCreatedTime(now);
        rule.setUpdatedTime(now);
        alertRuleMapper.insert(rule);
    }

    public void updateAlertRule(Long ruleId, DeptAlertRuleSaveDto dto) {
        DeptRegistrationAlertRule exist = alertRuleMapper.selectById(ruleId);
        if (exist == null) {
            throw new ServiceException("预警规则不存在");
        }
        validateDept(dto.getDeptId());
        applyRuleDto(exist, dto);
        exist.setUpdatedTime(LocalDateTime.now());
        alertRuleMapper.updateById(exist);
    }

    public void deleteAlertRule(Long ruleId) {
        DeptRegistrationAlertRule exist = alertRuleMapper.selectById(ruleId);
        if (exist == null) {
            throw new ServiceException("预警规则不存在");
        }
        alertRuleMapper.deleteById(ruleId);
    }

    private List<DeptRegistrationStatVo> buildDeptStats(Map<Long, Long> periodCountMap, Map<Long, DeptMetrics> metricsMap) {
        Map<Long, SysDept> deptMap = loadActiveDeptMap();

        List<DeptRegistrationStatVo> stats = new ArrayList<>();
        for (SysDept dept : deptMap.values()) {
            DeptRegistrationStatVo vo = new DeptRegistrationStatVo();
            vo.setDeptId(dept.getDeptId());
            vo.setDeptName(dept.getName());
            vo.setDeptCode(dept.getCode());

            DeptMetrics metrics = metricsMap.get(dept.getDeptId());
            vo.setTodayCount(metrics != null ? metrics.todayCount : 0L);
            vo.setAvg7Days(metrics != null ? metrics.avg7Days : BigDecimal.ZERO);
            vo.setChangePercent(metrics != null ? metrics.changePercent : null);
            vo.setPeriodCount(periodCountMap.getOrDefault(dept.getDeptId(), 0L));
            vo.setTomorrowCount(metrics != null ? metrics.tomorrowCount : 0L);
            vo.setDayAfterTomorrowCount(metrics != null ? metrics.dayAfterTomorrowCount : 0L);
            stats.add(vo);
        }

        return stats;
    }

    private Map<Long, DeptMetrics> buildMetricsMap(LocalDate today) {
        Map<Long, Long> doctorDeptMap = buildDoctorDeptMap();
        Map<Long, SysDept> deptMap = loadActiveDeptMap();
        Map<Long, DeptMetrics> result = new HashMap<>();
        for (Long deptId : deptMap.keySet()) {
            result.put(deptId, new DeptMetrics());
        }

        LocalDate avgStart = today.minusDays(7);
        LocalDate avgEnd = today.minusDays(1);
        Map<Long, Long> todayCounts = countByDept(today, today, doctorDeptMap);
        Map<Long, Long> tomorrowCounts = countByDept(today.plusDays(1), today.plusDays(1), doctorDeptMap);
        Map<Long, Long> dayAfterTomorrowCounts = countByDept(today.plusDays(2), today.plusDays(2), doctorDeptMap);
        Map<Long, Long> weekPastCounts = countByDept(today.minusDays(6), today, doctorDeptMap);
        Map<Long, Long> baselineCounts = countByDept(avgStart, avgEnd, doctorDeptMap);

        for (Long deptId : deptMap.keySet()) {
            DeptMetrics metrics = result.get(deptId);
            metrics.todayCount = todayCounts.getOrDefault(deptId, 0L);
            metrics.tomorrowCount = tomorrowCounts.getOrDefault(deptId, 0L);
            metrics.dayAfterTomorrowCount = dayAfterTomorrowCounts.getOrDefault(deptId, 0L);
            metrics.weekPastCount = weekPastCounts.getOrDefault(deptId, 0L);

            long baselineTotal = baselineCounts.getOrDefault(deptId, 0L);
            long baselineDays = countDistinctDaysWithData(avgStart, avgEnd, deptId, doctorDeptMap);
            if (baselineDays >= MIN_BASELINE_DAYS && baselineTotal > 0) {
                metrics.avg7Days = BigDecimal.valueOf(baselineTotal)
                        .divide(BigDecimal.valueOf(7), 1, RoundingMode.HALF_UP);
                if (metrics.avg7Days.compareTo(BigDecimal.ZERO) > 0) {
                    metrics.changePercent = BigDecimal.valueOf(metrics.todayCount)
                            .subtract(metrics.avg7Days)
                            .multiply(BigDecimal.valueOf(100))
                            .divide(metrics.avg7Days, 1, RoundingMode.HALF_UP);
                }
            } else if (baselineTotal > 0) {
                metrics.avg7Days = BigDecimal.valueOf(baselineTotal)
                        .divide(BigDecimal.valueOf(Math.max(baselineDays, 1)), 1, RoundingMode.HALF_UP);
            } else {
                metrics.avg7Days = BigDecimal.ZERO;
            }
        }
        return result;
    }

    private long countDistinctDaysWithData(LocalDate from, LocalDate to, Long deptId, Map<Long, Long> doctorDeptMap) {
        Set<Long> doctorIds = doctorDeptMap.entrySet().stream()
                .filter(e -> deptId.equals(e.getValue()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        if (doctorIds.isEmpty()) {
            return 0;
        }
        List<Appointment> list = appointmentMapper.selectList(
                new LambdaQueryWrapper<Appointment>()
                        .in(Appointment::getDoctorId, doctorIds)
                        .ge(Appointment::getAppointmentDate, from)
                        .le(Appointment::getAppointmentDate, to)
                        .ne(Appointment::getStatus, 3)
                        .select(Appointment::getAppointmentDate));
        return list.stream()
                .map(Appointment::getAppointmentDate)
                .filter(Objects::nonNull)
                .distinct()
                .count();
    }

    private Map<Long, Long> countByDept(LocalDate from, LocalDate to, Map<Long, Long> doctorDeptMap) {
        if (doctorDeptMap.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Appointment> appointments = appointmentMapper.selectList(
                new LambdaQueryWrapper<Appointment>()
                        .ge(Appointment::getAppointmentDate, from)
                        .le(Appointment::getAppointmentDate, to)
                        .ne(Appointment::getStatus, 3)
                        .select(Appointment::getDoctorId));
        Map<Long, Long> result = new HashMap<>();
        for (Appointment a : appointments) {
            Long deptId = doctorDeptMap.get(a.getDoctorId());
            if (deptId != null) {
                result.merge(deptId, 1L, Long::sum);
            }
        }
        return result;
    }

    private Map<Long, DiagnosisSignals> buildDiagnosisSignals(LocalDate from, LocalDate to) {
        Map<Long, Long> doctorDeptMap = buildDoctorDeptMap();

        List<MedicalRecord> records = medicalRecordMapper.selectList(
                new LambdaQueryWrapper<MedicalRecord>()
                        .ge(MedicalRecord::getVisitDate, from)
                        .le(MedicalRecord::getVisitDate, to)
                        .and(w -> w.isNull(MedicalRecord::getStatus).or().ne(MedicalRecord::getStatus, 1)));

        Map<Long, DiagnosisSignals> result = new HashMap<>();
        for (MedicalRecord record : records) {
            Long deptId = doctorDeptMap.get(record.getDoctorId());
            if (deptId == null) {
                continue;
            }
            DiagnosisSignals signals = result.computeIfAbsent(deptId, k -> new DiagnosisSignals());
            String text = joinText(record.getChiefComplaint(), record.getDiagnosis());
            if (containsAny(text, "水痘", "皮疹", "疱疹", "手足口", "麻疹")) {
                signals.chickenpoxHits++;
            }
            if (containsAny(text, "流感", "发热", "咳嗽", "咽痛", "乏力", "流行性感冒")) {
                signals.fluHits++;
            }
        }
        return result;
    }

    private Map<Long, Long> buildDoctorDeptMap() {
        List<Doctor> doctors = doctorMapper.selectList(
                new LambdaQueryWrapper<Doctor>()
                        .select(Doctor::getDoctorId, Doctor::getDeptId)
                        .isNotNull(Doctor::getDeptId));
        Map<Long, Long> map = new HashMap<>();
        for (Doctor d : doctors) {
            if (d.getDoctorId() != null && d.getDeptId() != null) {
                map.put(d.getDoctorId(), d.getDeptId());
            }
        }
        return map;
    }

    private Map<Long, SysDept> loadActiveDeptMap() {
        List<SysDept> depts = sysDeptMapper.selectList(
                new LambdaQueryWrapper<SysDept>()
                        .eq(SysDept::getStatus, 1)
                        .orderByAsc(SysDept::getSortOrder)
                        .orderByAsc(SysDept::getDeptId));
        Map<Long, SysDept> map = new HashMap<>();
        for (SysDept d : depts) {
            map.put(d.getDeptId(), d);
        }
        return map;
    }

    private Map<Long, String> loadDeptNameMap() {
        return sysDeptMapper.selectList(null).stream()
                .filter(d -> d.getDeptId() != null)
                .collect(Collectors.toMap(SysDept::getDeptId, SysDept::getName, (a, b) -> a));
    }

    private void validateDept(Long deptId) {
        SysDept dept = sysDeptMapper.selectById(deptId);
        if (dept == null) {
            throw new BusinessWarningException("科室不存在");
        }
    }

    private void applyRuleDto(DeptRegistrationAlertRule rule, DeptAlertRuleSaveDto dto) {
        rule.setDeptId(dto.getDeptId());
        rule.setRuleName(StringUtils.hasText(dto.getRuleName()) ? dto.getRuleName().trim() : null);
        rule.setThresholdType(dto.getThresholdType());
        rule.setThresholdValue(dto.getThresholdValue());
        rule.setAlertLevel(dto.getAlertLevel());
        rule.setEnabled(dto.getEnabled() != null && dto.getEnabled() == 0 ? 0 : 1);
        rule.setRemark(StringUtils.hasText(dto.getRemark()) ? dto.getRemark().trim() : null);
    }

    private DeptAlertRuleVo toRuleVo(DeptRegistrationAlertRule rule, Map<Long, String> deptNameMap) {
        DeptAlertRuleVo vo = new DeptAlertRuleVo();
        vo.setRuleId(rule.getRuleId());
        vo.setDeptId(rule.getDeptId());
        vo.setDeptName(deptNameMap.get(rule.getDeptId()));
        vo.setRuleName(rule.getRuleName());
        vo.setThresholdType(rule.getThresholdType());
        vo.setThresholdTypeLabel(thresholdTypeLabel(rule.getThresholdType()));
        vo.setThresholdValue(rule.getThresholdValue());
        vo.setAlertLevel(rule.getAlertLevel());
        vo.setAlertLevelLabel(alertLevelLabel(rule.getAlertLevel()));
        vo.setEnabled(rule.getEnabled());
        vo.setRemark(rule.getRemark());
        vo.setCreatedTime(rule.getCreatedTime());
        vo.setUpdatedTime(rule.getUpdatedTime());
        return vo;
    }

    private DeptEpidemicHintVo buildHint(SysDept dept, String type, String typeLabel, String message, int confidence) {
        DeptEpidemicHintVo vo = new DeptEpidemicHintVo();
        vo.setDeptId(dept.getDeptId());
        vo.setDeptName(dept.getName());
        vo.setHintType(type);
        vo.setHintTypeLabel(typeLabel);
        vo.setMessage(message);
        vo.setConfidence(confidence);
        vo.setConfidenceLabel(confidenceLabel(confidence));
        return vo;
    }

    private boolean isFluSeason(Month month) {
        return month == Month.NOVEMBER || month == Month.DECEMBER
                || month == Month.JANUARY || month == Month.FEBRUARY || month == Month.MARCH;
    }

    private String joinText(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (StringUtils.hasText(p)) {
                sb.append(p).append(' ');
            }
        }
        return sb.toString();
    }

    private boolean containsAny(String text, String... keywords) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        for (String kw : keywords) {
            if (text.contains(kw)) {
                return true;
            }
        }
        return false;
    }

    private String thresholdTypeLabel(Integer type) {
        if (type == null) {
            return "今日挂号绝对值";
        }
        return switch (type) {
            case 2 -> "较7日均值增幅(%)";
            case 3 -> "明日预约绝对值";
            case 4 -> "过去一周内预约合计";
            case 5 -> "过去一周内预约合计";
            default -> "今日挂号绝对值";
        };
    }

    private AlertEvaluation evaluateRule(DeptRegistrationAlertRule rule, DeptMetrics metrics, String deptName, int thresholdType) {
        BigDecimal threshold = rule.getThresholdValue();
        return switch (thresholdType) {
            case 2 -> evaluateIncreasePercent(metrics, deptName, threshold);
            case 3 -> evaluateAbsolute(metrics.tomorrowCount, deptName, threshold,
                    "%s 明日预约 %d 例，超过阈值 %.0f 例");
            case 4, 5 -> evaluateAbsolute(metrics.weekPastCount, deptName, threshold,
                    "%s 过去一周内预约合计 %d 例（近7日含今日），超过阈值 %.0f 例");
            default -> evaluateAbsolute(metrics.todayCount, deptName, threshold,
                    "%s 今日挂号 %d 例，超过阈值 %.0f 例");
        };
    }

    private AlertEvaluation evaluateIncreasePercent(DeptMetrics metrics, String deptName, BigDecimal threshold) {
        if (metrics.avg7Days == null || metrics.avg7Days.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        BigDecimal currentValue = metrics.changePercent != null ? metrics.changePercent : BigDecimal.ZERO;
        if (currentValue.compareTo(threshold) <= 0) {
            return null;
        }
        String message = String.format("%s 今日挂号 %d 例，较近7日均值 %.1f 例增幅 %.1f%%，超过阈值 %.1f%%",
                deptName, metrics.todayCount, metrics.avg7Days, currentValue, threshold);
        return new AlertEvaluation(currentValue, message);
    }

    private AlertEvaluation evaluateAbsolute(long count, String deptName, BigDecimal threshold, String template) {
        BigDecimal currentValue = BigDecimal.valueOf(count);
        if (currentValue.compareTo(threshold) <= 0) {
            return null;
        }
        String message = String.format(template, deptName, count, threshold);
        return new AlertEvaluation(currentValue, message);
    }

    private String alertLevelLabel(Integer level) {
        if (level == null) {
            return "提示";
        }
        return switch (level) {
            case 3 -> "严重";
            case 2 -> "警告";
            default -> "提示";
        };
    }

    private String confidenceLabel(int confidence) {
        return switch (confidence) {
            case 3 -> "高";
            case 2 -> "中";
            default -> "低";
        };
    }

    private String resolvePeriodType(LocalDate date, LocalDate today) {
        if (date.isBefore(today)) {
            return "past";
        }
        if (date.isAfter(today)) {
            return "future";
        }
        return "today";
    }

    private String periodTypeLabel(String periodType) {
        if ("future".equals(periodType)) {
            return "未来";
        }
        if ("today".equals(periodType)) {
            return "今日";
        }
        return "历史";
    }

    private static class DeptMetrics {
        private long todayCount;
        private long tomorrowCount;
        private long dayAfterTomorrowCount;
        /** 过去7日内有效预约合计（近7日含今日） */
        private long weekPastCount;
        private BigDecimal avg7Days = BigDecimal.ZERO;
        private BigDecimal changePercent;
    }

    private static class DiagnosisSignals {
        private int chickenpoxHits;
        private int fluHits;

        static DiagnosisSignals empty() {
            return new DiagnosisSignals();
        }
    }

    private static class AlertEvaluation {
        private final BigDecimal currentValue;
        private final String message;

        private AlertEvaluation(BigDecimal currentValue, String message) {
            this.currentValue = currentValue;
            this.message = message;
        }
    }
}
