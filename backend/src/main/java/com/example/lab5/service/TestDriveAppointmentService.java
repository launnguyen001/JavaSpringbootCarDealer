package com.example.lab5.service;

import com.example.lab5.dto.TestDriveAppointmentRequest;
import com.example.lab5.entity.AppointmentStatus;
import com.example.lab5.entity.TestDriveAppointment;

import java.util.List;

public interface TestDriveAppointmentService {

    TestDriveAppointment create(TestDriveAppointmentRequest request);

    List<TestDriveAppointment> getAllAppointments();

    TestDriveAppointment getAppointmentById(Long id);

    List<TestDriveAppointment> getByCustomer(Long customerId);

    TestDriveAppointment updateStatus(Long id, AppointmentStatus status);

    TestDriveAppointment cancel(Long id);

    TestDriveAppointment update(Long id, TestDriveAppointmentRequest request);

    void delete(Long id);
}
