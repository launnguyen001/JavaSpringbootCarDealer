package com.example.lab5.statemachine;

import com.example.lab5.entity.AppointmentStatus;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;

/**
 * Luồng trạng thái lịch lái thử:
 *
 * <pre>
 * SCHEDULED ──► CONFIRMED ──► COMPLETED
 *      │            │
 *      └────────────┴──────► CANCELLED
 * </pre>
 */
@Component
public class AppointmentStateMachine extends StateMachine<AppointmentStatus> {

    public AppointmentStateMachine() {
        super(AppointmentStatus.class, "lịch lái thử", definition());
    }

    private static Map<AppointmentStatus, EnumSet<AppointmentStatus>> definition() {
        Map<AppointmentStatus, EnumSet<AppointmentStatus>> map = new EnumMap<>(AppointmentStatus.class);
        map.put(AppointmentStatus.SCHEDULED,
                EnumSet.of(AppointmentStatus.CONFIRMED, AppointmentStatus.COMPLETED, AppointmentStatus.CANCELLED));
        map.put(AppointmentStatus.CONFIRMED,
                EnumSet.of(AppointmentStatus.COMPLETED, AppointmentStatus.CANCELLED));
        map.put(AppointmentStatus.COMPLETED, EnumSet.noneOf(AppointmentStatus.class));
        map.put(AppointmentStatus.CANCELLED, EnumSet.noneOf(AppointmentStatus.class));
        return map;
    }
}
