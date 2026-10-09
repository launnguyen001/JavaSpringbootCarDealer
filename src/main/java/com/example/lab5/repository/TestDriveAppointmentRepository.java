package com.example.lab5.repository;

import com.example.lab5.entity.AppointmentStatus;
import com.example.lab5.entity.TestDriveAppointment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TestDriveAppointmentRepository extends JpaRepository<TestDriveAppointment, Long> {

    List<TestDriveAppointment> findByCustomer_IdOrderByScheduledAtDesc(Long customerId);

    List<TestDriveAppointment> findByStatusOrderByScheduledAtAsc(AppointmentStatus status);

    List<TestDriveAppointment> findAllByOrderByScheduledAtAsc();
}
