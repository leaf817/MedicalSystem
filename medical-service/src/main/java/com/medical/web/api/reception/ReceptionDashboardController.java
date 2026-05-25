package com.medical.web.api.reception;

import com.medical.common.response.ResultVo;
import com.medical.domain.vo.ReceptionDashboardVo;
import com.medical.service.ReceptionDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reception/dashboard")
@RequiredArgsConstructor
public class ReceptionDashboardController {

    private final ReceptionDashboardService receptionDashboardService;

    @GetMapping("/summary")
    public ResultVo<ReceptionDashboardVo> summary() {
        return ResultVo.ok(receptionDashboardService.getSummary());
    }
}
