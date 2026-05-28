package com.medical.ai.support;

import com.medical.ai.config.AiProperties;
import com.medical.common.exception.BusinessWarningException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 按患者每日调用次数限流（Redis）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiRateLimitService {

    private static final String KEY_PREFIX = "ai:limit:patient:";
    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.BASIC_ISO_DATE;

    private final StringRedisTemplate stringRedisTemplate;
    private final AiProperties aiProperties;

    /**
     * 发送消息前调用：超限则抛业务异常
     */
    public void checkAndIncrement(Long patientId) {
        int limit = aiProperties.getDailyRequestLimitPerUser();
        if (limit <= 0) {
            return;
        }
        String key = KEY_PREFIX + patientId + ":" + LocalDate.now().format(DAY_FMT);
        try {
            Long count = stringRedisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                stringRedisTemplate.expire(key, Duration.ofHours(25));
            }
            if (count != null && count > limit) {
                throw new BusinessWarningException("今日智能问诊次数已达上限（" + limit + "次），请明天再试");
            }
        } catch (BusinessWarningException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Redis rate limit unavailable, skipped for patientId={}: {}", patientId, e.getMessage());
        }
    }
}
