package com.example.lab5.mapper;

import com.example.lab5.dto.TestDriveAppointmentResponse;
import com.example.lab5.entity.TestDriveAppointment;
import org.springframework.stereotype.Component;

@Component
public class TestDriveAppointmentMapper {

    public TestDriveAppointmentResponse toResponse(TestDriveAppointment appointment) {
        return new TestDriveAppointmentResponse(
                appointment.getId(),
                appointment.getScheduledAt(),
                appointment.getStatus(),
                appointment.getNote(),
                appointment.getCustomer() == null ? null : appointment.getCustomer().getId(),
                appointment.getCustomer() == null ? null : appointment.getCustomer().getFullName(),
                appointment.getCar() == null ? null : appointment.getCar().getId(),
                appointment.getCar() == null ? null : appointment.getCar().getName(),
                appointment.getEmployee() == null ? null : appointment.getEmployee().getId(),
                appointment.getEmployee() == null ? null : appointment.getEmployee().getFullName()
        );
    }
}
