package com.medical.web.api.reception;

import com.medical.common.response.ResultVo;
import com.medical.domain.dto.ReceptionPatientRegisterDto;
import com.medical.domain.vo.ReceptionPatientVo;
import com.medical.service.ReceptionPatientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reception/patient")
@RequiredArgsConstructor
public class ReceptionPatientController {

    private final ReceptionPatientService receptionPatientService;

    @PostMapping("/register")
    public ResultVo<ReceptionPatientVo> register(@Valid @RequestBody ReceptionPatientRegisterDto dto) {
        return ResultVo.ok(receptionPatientService.register(dto));
    }

    @GetMapping("/search")
    public ResultVo<List<ReceptionPatientVo>> search(@RequestParam("keyword") String keyword) {
        return ResultVo.ok(receptionPatientService.search(keyword));
    }

    @GetMapping("/{patientId}")
    public ResultVo<ReceptionPatientVo> detail(@PathVariable Long patientId) {
        return ResultVo.ok(receptionPatientService.getByPatientId(patientId));
    }
}
