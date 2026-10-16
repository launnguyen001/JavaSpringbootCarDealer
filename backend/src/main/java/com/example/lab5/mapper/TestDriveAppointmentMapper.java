package com.example.lab5.mapper;

import com.example.lab5.dto.TestDriveAppointmentResponse;
import com.example.lab5.entity.TestDriveAppointment;
import org.springframework.stereotype.Component;

@Component
public class TestDriveAppointmentMapper {

    public TestDriveAppointmentResponse toResponse(TestDriveAppointment appointment) {
        Long carItemId = appointment.getCarItem() == null ? null : appointment.getCarItem().getId();
        Long carId = null;
        String carName = null;
        if (appointment.getCarItem() != null && appointment.getCarItem().getCar() != null) {
            carId = appointment.getCarItem().getCar().getId();
            carName = appointment.getCarItem().getCar().getName();
        }
        return new TestDriveAppointmentResponse(
                appointment.getId(),
                appointment.getScheduledAt(),
                appointment.getStatus(),
                appointment.getNote(),
                appointment.getCustomer() == null ? null : appointment.getCustomer().getId(),
                appointment.getCustomer() == null ? null : appointment.getCustomer().getFullName(),
                carItemId,
                carId,
                carName,
                appointment.getCarItem() == null ? null : appointment.getCarItem().getVin(),
                appointment.getEmployee() == null ? null : appointment.getEmployee().getId(),
                appointment.getEmployee() == null ? null : appointment.getEmployee().getFullName()
        );
    }
}
