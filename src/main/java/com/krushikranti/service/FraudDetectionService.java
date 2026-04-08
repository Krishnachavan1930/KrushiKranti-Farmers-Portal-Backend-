package com.krushikranti.service;

import com.krushikranti.model.FraudAlert;
import com.krushikranti.model.Order;
import com.krushikranti.repository.FraudAlertRepository;
import com.krushikranti.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudDetectionService {

    private static final String ALERT_TYPE_HIGH_ACTIVITY = "HIGH_ACTIVITY";
    private static final String SEVERITY_HIGH = "HIGH";
    private static final String STATUS_PENDING = "PENDING";

    private final OrderRepository orderRepository;
    private final FraudAlertRepository fraudAlertRepository;

    @Transactional
    public void checkUserOrderFrequency(Long userId) {
        if (userId == null) {
            return;
        }

        List<Order> recentOrders = orderRepository.findByUserIdAndCreatedAtAfter(
                userId,
                LocalDateTime.now().minusMinutes(2)
        );

        if (recentOrders.size() > 10) {
            createFraudAlert(userId, "User placed too many orders in short time");
        }
    }

    @Transactional
    public void createFraudAlert(Long userId, String message) {
        boolean pendingExists = fraudAlertRepository.existsByUserIdAndTypeAndStatus(
                userId,
                ALERT_TYPE_HIGH_ACTIVITY,
                STATUS_PENDING
        );

        if (pendingExists) {
            log.debug("Skipping duplicate pending fraud alert for userId={}", userId);
            return;
        }

        FraudAlert alert = FraudAlert.builder()
                .userId(userId)
                .type(ALERT_TYPE_HIGH_ACTIVITY)
                .message(message)
                .severity(SEVERITY_HIGH)
                .status(STATUS_PENDING)
                .build();

        fraudAlertRepository.save(alert);
        log.warn("Fraud alert created for userId={} type={}", userId, ALERT_TYPE_HIGH_ACTIVITY);
    }
}
