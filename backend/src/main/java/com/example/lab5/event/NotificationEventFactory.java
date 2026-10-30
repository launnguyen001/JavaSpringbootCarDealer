package com.example.lab5.event;

import com.example.lab5.entity.AppointmentStatus;
import com.example.lab5.entity.Contract;
import com.example.lab5.entity.Customer;
import com.example.lab5.entity.Invoice;
import com.example.lab5.entity.NotificationType;
import com.example.lab5.entity.OrderStatus;
import com.example.lab5.entity.Payment;
import com.example.lab5.entity.SalesOrder;
import com.example.lab5.entity.TestDriveAppointment;
import com.example.lab5.util.Formatters;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Tạo và phát sự kiện thông báo từ các entity nghiệp vụ.
 *
 * <p>Phải được gọi BÊN TRONG transaction đang mở để có thể đọc các quan hệ lazy
 * (khách hàng, xe, tài khoản) trước khi dữ liệu được chuyển thành {@link BusinessEvent}.
 */
@Component
public class NotificationEventFactory {

    private final ApplicationEventPublisher publisher;

    public NotificationEventFactory(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void orderCreated(SalesOrder order) {
        publish(NotificationType.ORDER,
                "Đơn hàng " + order.getOrderCode() + " đã được tạo",
                "Đơn hàng " + order.getOrderCode() + " với tổng giá trị "
                        + Formatters.money(order.getTotalAmount())
                        + " đã được tạo. Chúng tôi sẽ liên hệ xác nhận trong thời gian sớm nhất.",
                "ORDER", order.getId(), order.getCustomer());
    }

    public void orderStatusChanged(SalesOrder order, OrderStatus from, OrderStatus to) {
        String title = "Cập nhật trạng thái đơn " + order.getOrderCode();
        String message = "Đơn hàng " + order.getOrderCode() + " đã chuyển từ "
                + label(from) + " sang " + label(to) + ".";
        if (to == OrderStatus.COMPLETED) {
            message += " Cảm ơn bạn đã hoàn tất giao dịch.";
        } else if (to == OrderStatus.CANCELLED) {
            message += " Nếu cần hỗ trợ thêm, vui lòng liên hệ đại lý.";
        }
        publish(NotificationType.ORDER, title, message, "ORDER", order.getId(), order.getCustomer());
    }

    public void paymentPaid(Payment payment) {
        SalesOrder order = payment.getOrder();
        publish(NotificationType.PAYMENT,
                "Đã ghi nhận thanh toán cho đơn " + order.getOrderCode(),
                "Khoản thanh toán " + Formatters.money(payment.getAmount())
                        + " cho đơn hàng " + order.getOrderCode()
                        + " đã được ghi nhận thành công.",
                "PAYMENT", payment.getId(), order.getCustomer());
    }

    public void contractCreated(Contract contract) {
        SalesOrder order = contract.getOrder();
        publish(NotificationType.CONTRACT,
                "Hợp đồng " + contract.getContractNumber() + " đã sẵn sàng",
                "Hợp đồng " + contract.getContractNumber() + " cho đơn hàng "
                        + order.getOrderCode() + " đã được tạo. Bạn có thể tải bản PDF trong mục tài khoản.",
                "CONTRACT", contract.getId(), order.getCustomer());
    }

    public void invoiceCreated(Invoice invoice) {
        SalesOrder order = invoice.getOrder();
        publish(NotificationType.INVOICE,
                "Hóa đơn " + invoice.getInvoiceNumber() + " đã phát hành",
                "Hóa đơn " + invoice.getInvoiceNumber() + " cho đơn hàng "
                        + order.getOrderCode() + " đã được phát hành với tổng tiền "
                        + Formatters.money(invoice.getTotal()) + ".",
                "INVOICE", invoice.getId(), order.getCustomer());
    }

    public void testDriveCreated(TestDriveAppointment appointment) {
        publish(NotificationType.TEST_DRIVE,
                "Đã ghi nhận lịch lái thử",
                "Lịch lái thử xe " + carName(appointment) + " vào lúc "
                        + Formatters.dateTime(appointment.getScheduledAt())
                        + " đã được ghi nhận. Đại lý sẽ xác nhận với bạn.",
                "TEST_DRIVE", appointment.getId(), appointment.getCustomer());
    }

    public void testDriveStatusChanged(TestDriveAppointment appointment, AppointmentStatus from, AppointmentStatus to) {
        publish(NotificationType.TEST_DRIVE,
                "Cập nhật lịch lái thử",
                "Lịch lái thử xe " + carName(appointment) + " vào lúc "
                        + Formatters.dateTime(appointment.getScheduledAt())
                        + " đã chuyển từ " + label(from) + " sang " + label(to) + ".",
                "TEST_DRIVE", appointment.getId(), appointment.getCustomer());
    }

    private void publish(NotificationType type, String title, String message,
                         String entityType, Long entityId, Customer customer) {
        if (customer == null) {
            return;
        }
        Long userId = customer.getUser() == null ? null : customer.getUser().getId();
        publisher.publishEvent(new BusinessEvent(
                type,
                title,
                message,
                entityType,
                entityId,
                userId,
                customer.getEmail(),
                customer.getFullName()
        ));
    }

    private String carName(TestDriveAppointment appointment) {
        if (appointment.getCarItem() == null || appointment.getCarItem().getCar() == null) {
            return "(không xác định)";
        }
        return appointment.getCarItem().getCar().getName();
    }

    private String label(OrderStatus status) {
        return switch (status) {
            case PENDING -> "Chờ xử lý";
            case CONFIRMED -> "Đã xác nhận";
            case COMPLETED -> "Hoàn tất";
            case CANCELLED -> "Đã hủy";
        };
    }

    private String label(AppointmentStatus status) {
        return switch (status) {
            case SCHEDULED -> "Đã lên lịch";
            case CONFIRMED -> "Đã xác nhận";
            case COMPLETED -> "Hoàn tất";
            case CANCELLED -> "Đã hủy";
        };
    }
}
