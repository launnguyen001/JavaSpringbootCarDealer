package com.example.lab5.statemachine;

import com.example.lab5.entity.PaymentStatus;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

/**
 * Luồng trạng thái thanh toán:
 *
 * <pre>
 * UNPAID ──► PARTIAL ──► PAID ──► REFUNDED
 *    │           │         │
 *    └───────────┴─────────┘
 * </pre>
 */
@Component
public class PaymentStateMachine extends StateMachine<PaymentStatus> {

    public PaymentStateMachine() {
        super(PaymentStatus.class, "thanh toán", definition());
    }

    private static Map<PaymentStatus, EnumSet<PaymentStatus>> definition() {
        Map<PaymentStatus, EnumSet<PaymentStatus>> map = new EnumMap<>(PaymentStatus.class);
        map.put(PaymentStatus.UNPAID,
                EnumSet.of(PaymentStatus.PARTIAL, PaymentStatus.PAID, PaymentStatus.REFUNDED));
        map.put(PaymentStatus.PARTIAL,
                EnumSet.of(PaymentStatus.PAID, PaymentStatus.REFUNDED));
        map.put(PaymentStatus.PAID, EnumSet.of(PaymentStatus.REFUNDED));
        map.put(PaymentStatus.REFUNDED, EnumSet.noneOf(PaymentStatus.class));
        return map;
    }
}
