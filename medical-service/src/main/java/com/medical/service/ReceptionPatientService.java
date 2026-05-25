package com.medical.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.medical.common.exception.BusinessWarningException;
import com.medical.domain.dto.ReceptionPatientRegisterDto;
import com.medical.domain.entity.Patient;
import com.medical.domain.entity.SysRole;
import com.medical.domain.entity.SysUser;
import com.medical.domain.entity.SysUserRole;
import com.medical.domain.vo.ReceptionPatientVo;
import com.medical.mapper.PatientMapper;
import com.medical.mapper.SysRoleMapper;
import com.medical.mapper.SysUserMapper;
import com.medical.mapper.SysUserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReceptionPatientService {

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final PatientMapper patientMapper;
    private final PasswordEncoder passwordEncoder;
    private final PatientExtensionService patientExtensionService;

    @Transactional(rollbackFor = Exception.class)
    public ReceptionPatientVo register(ReceptionPatientRegisterDto dto) {
        String username = dto.getUsername().trim();
        if (StringUtils.hasText(dto.getMobilePhone())
                && !dto.getMobilePhone().trim().matches("^1\\d{10}$")) {
            throw new BusinessWarningException("手机号格式不正确");
        }
        long exist = sysUserMapper.selectCount(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (exist > 0) {
            throw new BusinessWarningException("用户名已被占用");
        }
        if (StringUtils.hasText(dto.getIdCard())) {
            long idCardCount = patientMapper.selectCount(
                    new LambdaQueryWrapper<Patient>().eq(Patient::getIdCard, dto.getIdCard().trim()));
            if (idCardCount > 0) {
                throw new BusinessWarningException("该身份证号已建档");
            }
        }

        SysRole patientRole = sysRoleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>()
                        .eq(SysRole::getRoleCode, "PATIENT")
                        .eq(SysRole::getStatus, 1));
        if (patientRole == null) {
            throw new BusinessWarningException("系统未配置患者角色，请联系管理员");
        }

        LocalDateTime now = LocalDateTime.now();
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setName(dto.getName().trim());
        user.setMobilePhone(StringUtils.hasText(dto.getMobilePhone()) ? dto.getMobilePhone().trim() : null);
        user.setStatus(1);
        user.setCreatedTime(now);
        user.setUpdatedTime(now);
        sysUserMapper.insert(user);

        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(user.getUserId());
        userRole.setRoleId(patientRole.getRoleId());
        userRole.setCreatedTime(now);
        sysUserRoleMapper.insert(userRole);

        patientExtensionService.syncPatientExtension(user.getUserId());

        Patient patient = patientMapper.selectOne(
                new LambdaQueryWrapper<Patient>().eq(Patient::getUserId, user.getUserId()));
        if (patient != null) {
            boolean updated = false;
            if (StringUtils.hasText(dto.getIdCard())) {
                patient.setIdCard(dto.getIdCard().trim());
                updated = true;
            }
            if (StringUtils.hasText(dto.getGender())) {
                patient.setGender(dto.getGender().trim());
                updated = true;
            }
            if (dto.getBirthDate() != null) {
                patient.setBirthDate(dto.getBirthDate());
                updated = true;
            }
            if (StringUtils.hasText(dto.getAddress())) {
                patient.setAddress(dto.getAddress().trim());
                updated = true;
            }
            if (updated) {
                patient.setUpdatedTime(now);
                patientMapper.updateById(patient);
            }
        }

        return toVo(patientMapper.selectOne(
                new LambdaQueryWrapper<Patient>().eq(Patient::getUserId, user.getUserId())));
    }

    public List<ReceptionPatientVo> search(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return List.of();
        }
        String kw = keyword.trim();
        LambdaQueryWrapper<Patient> wrapper = new LambdaQueryWrapper<>();
        wrapper.and(w -> w.like(Patient::getName, kw)
                .or().like(Patient::getPhone, kw)
                .or().like(Patient::getIdCard, kw)
                .or().like(Patient::getPatientNo, kw));
        wrapper.eq(Patient::getStatus, 1);
        wrapper.orderByDesc(Patient::getUpdatedTime).last("limit 20");
        return patientMapper.selectList(wrapper).stream()
                .map(this::toVo)
                .collect(Collectors.toList());
    }

    public ReceptionPatientVo getByPatientId(Long patientId) {
        Patient patient = patientMapper.selectById(patientId);
        if (patient == null) {
            throw new BusinessWarningException("患者不存在");
        }
        return toVo(patient);
    }

    private ReceptionPatientVo toVo(Patient patient) {
        if (patient == null) {
            return null;
        }
        ReceptionPatientVo vo = new ReceptionPatientVo();
        vo.setPatientId(patient.getPatientId());
        vo.setUserId(patient.getUserId());
        vo.setPatientNo(patient.getPatientNo());
        vo.setName(patient.getName());
        vo.setIdCard(patient.getIdCard());
        vo.setGender(patient.getGender());
        vo.setPhone(patient.getPhone());
        vo.setStatus(patient.getStatus());
        return vo;
    }
}
