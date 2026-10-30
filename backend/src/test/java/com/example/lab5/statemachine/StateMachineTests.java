package com.example.lab5.statemachine;

import com.example.lab5.entity.AppointmentStatus;
import com.example.lab5.entity.OrderStatus;
import com.example.lab5.entity.PaymentStatus;
import com.example.lab5.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StateMachineTests {

    private final OrderStateMachine orders = new OrderStateMachine();
    private final AppointmentStateMachine appointments = new AppointmentStateMachine();
    private final PaymentStateMachine payments = new PaymentStateMachine();

    @Test
    void orderStateMachineAllowsDocumentedTransitions() {
        assertThat(orders.allowedNext(OrderStatus.PENDING))
                .containsExactlyInAnyOrder(OrderStatus.CONFIRMED, OrderStatus.COMPLETED, OrderStatus.CANCELLED);
        assertThat(orders.allowedNext(OrderStatus.CONFIRMED))
                .containsExactlyInAnyOrder(OrderStatus.COMPLETED, OrderStatus.CANCELLED);
        assertThat(orders.allowedNext(OrderStatus.COMPLETED)).isEmpty();
        assertThat(orders.allowedNext(OrderStatus.CANCELLED)).isEmpty();

        assertThatCode(() -> orders.assertCanTransition(OrderStatus.PENDING, OrderStatus.CONFIRMED))
                .doesNotThrowAnyException();
        assertThatCode(() -> orders.assertCanTransition(OrderStatus.CONFIRMED, OrderStatus.COMPLETED))
                .doesNotThrowAnyException();
    }

    @Test
    void orderStateMachineRejectsInvalidTransition() {
        assertThatThrownBy(() -> orders.assertCanTransition(OrderStatus.CANCELLED, OrderStatus.COMPLETED))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Không thể chuyển")
                .hasMessageContaining("trạng thái kết thúc");
    }

    @Test
    void sameStatusIsIdempotentForEveryMachine() {
        assertThatCode(() -> orders.assertCanTransition(OrderStatus.COMPLETED, OrderStatus.COMPLETED))
                .doesNotThrowAnyException();
        assertThatCode(() -> appointments.assertCanTransition(
                AppointmentStatus.COMPLETED, AppointmentStatus.COMPLETED))
                .doesNotThrowAnyException();
        assertThatCode(() -> payments.assertCanTransition(PaymentStatus.PAID, PaymentStatus.PAID))
                .doesNotThrowAnyException();
    }

    @Test
    void appointmentStateMachineRejectsTransitionFromTerminalStatus() {
        assertThat(appointments.allowedNext(AppointmentStatus.CANCELLED)).isEmpty();
        assertThatThrownBy(() -> appointments.assertCanTransition(
                AppointmentStatus.CANCELLED, AppointmentStatus.SCHEDULED))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void paymentStateMachineRejectsInvalidTransition() {
        assertThat(payments.allowedNext(PaymentStatus.PAID)).containsExactly(PaymentStatus.REFUNDED);
        assertThat(payments.allowedNext(PaymentStatus.REFUNDED)).isEmpty();
        assertThatThrownBy(() -> payments.assertCanTransition(PaymentStatus.REFUNDED, PaymentStatus.PAID))
                .isInstanceOf(BadRequestException.class);
    }
}
