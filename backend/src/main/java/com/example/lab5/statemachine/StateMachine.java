package com.example.lab5.statemachine;

import com.example.lab5.exception.BadRequestException;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Máy trạng thái tổng quát cho các enum trạng thái nghiệp vụ.
 *
 * <p>Mỗi trạng thái khai báo tập trạng thái được phép chuyển tới. Mọi thay đổi trạng thái
 * phải đi qua {@link #assertCanTransition(Enum, Enum)} để tránh nhảy trạng thái tuỳ tiện
 * (ví dụ đơn đã hủy lại chuyển sang hoàn tất).
 *
 * @param <S> enum trạng thái
 */
public abstract class StateMachine<S extends Enum<S>> {

    private final Map<S, Set<S>> transitions;
    private final String resourceLabel;

    protected StateMachine(Class<S> type, String resourceLabel, Map<S, ? extends Set<S>> transitions) {
        this.resourceLabel = resourceLabel;
        Map<S, Set<S>> copy = new EnumMap<>(type);
        transitions.forEach((from, to) -> copy.put(from, to.isEmpty()
                ? Collections.emptySet()
                : Collections.unmodifiableSet(EnumSet.copyOf(to))));
        // Trạng thái không khai báo được hiểu là trạng thái kết thúc.
        for (S value : type.getEnumConstants()) {
            copy.putIfAbsent(value, Collections.emptySet());
        }
        this.transitions = Collections.unmodifiableMap(copy);
    }

    public Set<S> allowedNext(S from) {
        if (from == null) {
            return Collections.emptySet();
        }
        return transitions.getOrDefault(from, Collections.emptySet());
    }

    public boolean canTransition(S from, S to) {
        return from != null && to != null && allowedNext(from).contains(to);
    }

    public void assertCanTransition(S from, S to) {
        if (to == null) {
            throw new BadRequestException("Trạng thái " + resourceLabel + " không được để trống");
        }
        if (from == to) {
            return;
        }
        if (!canTransition(from, to)) {
            String allowed = allowedNext(from).stream()
                    .map(value -> value.name())
                    .collect(Collectors.joining(", "));
            if (allowed.isBlank()) {
                allowed = "(không có — đây là trạng thái kết thúc)";
            }
            throw new BadRequestException(
                    "Không thể chuyển " + resourceLabel + " từ " + from + " sang " + to
                            + ". Trạng thái được phép: " + allowed);
        }
    }

    public Map<S, Set<S>> transitions() {
        return transitions;
    }

    public String resourceLabel() {
        return resourceLabel;
    }
}
