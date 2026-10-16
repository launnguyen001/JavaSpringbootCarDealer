package com.example.lab5.service.Impl;

import com.example.lab5.dto.TestDriveAppointmentRequest;
import com.example.lab5.entity.AppointmentStatus;
import com.example.lab5.entity.CarItem;
import com.example.lab5.entity.Customer;
import com.example.lab5.entity.Employee;
import com.example.lab5.entity.TestDriveAppointment;
import com.example.lab5.exception.BadRequestException;
import com.example.lab5.exception.ResourceNotFoundException;
import com.example.lab5.repository.CarItemRepository;
import com.example.lab5.repository.CustomerRepository;
import com.example.lab5.repository.EmployeeRepository;
import com.example.lab5.repository.TestDriveAppointmentRepository;
import com.example.lab5.service.TestDriveAppointmentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TestDriveAppointmentServiceImpl implements TestDriveAppointmentService {

    private final TestDriveAppointmentRepository appointmentRepository;
    private final CustomerRepository customerRepository;
    private final CarItemRepository carItemRepository;
    private final EmployeeRepository employeeRepository;

    public TestDriveAppointmentServiceImpl(
            TestDriveAppointmentRepository appointmentRepository,
            CustomerRepository customerRepository,
            CarItemRepository carItemRepository,
            EmployeeRepository employeeRepository
    ) {
        this.appointmentRepository = appointmentRepository;
        this.customerRepository = customerRepository;
        this.carItemRepository = carItemRepository;
        this.employeeRepository = employeeRepository;
    }

    @Override
    @Transactional
    public TestDriveAppointment create(TestDriveAppointmentRequest request) {
        if (request == null || request.scheduledAt() == null) {
            throw new BadRequestException("Thời gian hẹn không được để trống");
        }
        if (request.customerId() == null) {
            throw new BadRequestException("Khách hàng không được để trống");
        }
        if (request.carItemId() == null) {
            throw new BadRequestException("Xe không được để trống");
        }

        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy khách hàng có id = " + request.customerId()));
        CarItem carItem = findCarItem(request.carItemId());

        Employee employee = null;
        if (request.employeeId() != null) {
            employee = employeeRepository.findById(request.employeeId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Không tìm thấy nhân viên có id = " + request.employeeId()));
        }

        TestDriveAppointment appointment = new TestDriveAppointment();
        appointment.setScheduledAt(request.scheduledAt());
        appointment.setNote(request.note());
        appointment.setCustomer(customer);
        appointment.setCarItem(carItem);
        appointment.setEmployee(employee);
        appointment.setStatus(request.status() == null ? AppointmentStatus.SCHEDULED : request.status());
        return appointmentRepository.save(appointment);
    }

    @Override
    public List<TestDriveAppointment> getAllAppointments() {
        return appointmentRepository.findAllByOrderByScheduledAtAsc();
    }

    @Override
    public TestDriveAppointment getAppointmentById(Long id) {
        return appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch lái thử có id = " + id));
    }

    @Override
    public List<TestDriveAppointment> getByCustomer(Long customerId) {
        if (customerId == null) {
            throw new BadRequestException("Khách hàng không được để trống");
        }
        return appointmentRepository.findByCustomer_IdOrderByScheduledAtDesc(customerId);
    }

    @Override
    @Transactional
    public TestDriveAppointment updateStatus(Long id, AppointmentStatus status) {
        if (status == null) {
            throw new BadRequestException("Trạng thái lịch lái thử không được để trống");
        }
        TestDriveAppointment appointment = getAppointmentById(id);
        appointment.setStatus(status);
        return appointmentRepository.save(appointment);
    }

    @Override
    @Transactional
    public TestDriveAppointment cancel(Long id) {
        TestDriveAppointment appointment = getAppointmentById(id);
        if (appointment.getStatus() == AppointmentStatus.CANCELLED) {
            throw new BadRequestException("Lịch lái thử đã bị huỷ trước đó");
        }
        appointment.setStatus(AppointmentStatus.CANCELLED);
        return appointmentRepository.save(appointment);
    }

    @Override
    @Transactional
    public TestDriveAppointment update(Long id, TestDriveAppointmentRequest request) {
        if (request == null || request.scheduledAt() == null) {
            throw new BadRequestException("Thời gian hẹn không được để trống");
        }
        if (request.carItemId() == null) {
            throw new BadRequestException("Xe không được để trống");
        }
        TestDriveAppointment appointment = getAppointmentById(id);

        CarItem carItem = findCarItem(request.carItemId());
        Employee employee = null;
        if (request.employeeId() != null) {
            employee = employeeRepository.findById(request.employeeId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Không tìm thấy nhân viên có id = " + request.employeeId()));
        }

        appointment.setScheduledAt(request.scheduledAt());
        appointment.setNote(request.note());
        appointment.setCarItem(carItem);
        appointment.setEmployee(employee);
        if (request.status() != null) {
            appointment.setStatus(request.status());
        }
        return appointmentRepository.save(appointment);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        getAppointmentById(id);
        appointmentRepository.deleteById(id);
    }

    private CarItem findCarItem(Long carItemId) {
        return carItemRepository.findById(carItemId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy xe có id = " + carItemId));
    }
}
