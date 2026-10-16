package com.example.lab5.controller;

import com.example.lab5.dto.TestDriveAppointmentRequest;
import com.example.lab5.dto.TestDriveAppointmentResponse;
import com.example.lab5.entity.AppointmentStatus;
import com.example.lab5.entity.TestDriveAppointment;
import com.example.lab5.mapper.TestDriveAppointmentMapper;
import com.example.lab5.service.TestDriveAppointmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/test-drives")
public class TestDriveAppointmentController {

    private final TestDriveAppointmentService appointmentService;
    private final TestDriveAppointmentMapper appointmentMapper;

    public TestDriveAppointmentController(
            TestDriveAppointmentService appointmentService,
            TestDriveAppointmentMapper appointmentMapper
    ) {
        this.appointmentService = appointmentService;
        this.appointmentMapper = appointmentMapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TestDriveAppointmentResponse createAppointment(
            @Valid @RequestBody TestDriveAppointmentRequest request
    ) {
        return appointmentMapper.toResponse(appointmentService.create(request));
    }

    @GetMapping
    public List<TestDriveAppointmentResponse> getAllAppointments() {
        return toResponses(appointmentService.getAllAppointments());
    }

    @GetMapping("/{id}")
    public TestDriveAppointmentResponse getAppointmentById(@PathVariable Long id) {
        return appointmentMapper.toResponse(appointmentService.getAppointmentById(id));
    }

    @GetMapping("/customer/{customerId}")
    public List<TestDriveAppointmentResponse> getByCustomer(@PathVariable Long customerId) {
        return toResponses(appointmentService.getByCustomer(customerId));
    }

    @PatchMapping("/{id}/status")
    public TestDriveAppointmentResponse updateStatus(
            @PathVariable Long id,
            @RequestParam AppointmentStatus status
    ) {
        return appointmentMapper.toResponse(appointmentService.updateStatus(id, status));
    }

    @PostMapping("/{id}/cancel")
    public TestDriveAppointmentResponse cancelAppointment(@PathVariable Long id) {
        return appointmentMapper.toResponse(appointmentService.cancel(id));
    }

    @PutMapping("/{id}")
    public TestDriveAppointmentResponse updateAppointment(
            @PathVariable Long id,
            @Valid @RequestBody TestDriveAppointmentRequest request
    ) {
        return appointmentMapper.toResponse(appointmentService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAppointment(@PathVariable Long id) {
        appointmentService.delete(id);
    }

    private List<TestDriveAppointmentResponse> toResponses(List<TestDriveAppointment> appointments) {
        return appointments.stream().map(appointmentMapper::toResponse).toList();
    }
}
