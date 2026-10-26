package com.histar.be.admin.service;

import com.histar.be.admin.dto.DemoEnvironmentStatus;
import com.histar.be.common.exception.BusinessRuleException;
import com.histar.be.config.DemoProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DemoEnvironmentService {
    public static final String STOP_CONFIRMATION = "TẮT DEMO";

    private final DemoProperties properties;
    private final DemoEnvironmentHostClient hostClient;

    public DemoEnvironmentStatus status() {
        requireProductionEnvironment();
        return hostClient.status();
    }

    public DemoEnvironmentStatus start() {
        requireProductionEnvironment();
        return hostClient.start();
    }

    public DemoEnvironmentStatus stop(String confirmation) {
        requireProductionEnvironment();
        if (!STOP_CONFIRMATION.equals(confirmation)) {
            throw new BusinessRuleException("Vui lòng nhập chính xác “" + STOP_CONFIRMATION + "” để xác nhận");
        }

        DemoEnvironmentStatus latest = hostClient.status();
        if (latest.activeUsers() > 0) {
            throw new BusinessRuleException(
                    "Không thể tắt demo vì còn " + latest.activeUsers() + " người đang hoạt động");
        }
        return hostClient.stop();
    }

    private void requireProductionEnvironment() {
        if (properties.isEnabled()) {
            throw new BusinessRuleException("Không thể điều khiển môi trường demo từ chính môi trường demo");
        }
    }
}
