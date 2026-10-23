package com.example.lab5.service;

import com.example.lab5.document.ActivityLog;
import com.example.lab5.dto.ActivityLogRequest;
import com.example.lab5.exception.BadRequestException;
import com.example.lab5.mapper.ActivityLogMapper;
import com.example.lab5.mongorepository.ActivityLogRepository;
import com.example.lab5.service.Impl.ActivityLogServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
// import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityLogServiceImplTests {

    @Mock
    private ActivityLogRepository repository;

    @Mock
    private ActivityLogMapper mapper;

    @InjectMocks
    private ActivityLogServiceImpl service;

    @Test
    void logSavesMappedEntity() {
        ActivityLogRequest request = new ActivityLogRequest(
                "CREATE", "Car", 1L, "tạo xe", "admin", null);
        ActivityLog entity = new ActivityLog();
        entity.setAction("CREATE");
        when(mapper.toEntity(request)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(entity);

        ActivityLog result = service.log(request);

        assertThat(result).isSameAs(entity);
        assertThat(result.getAction()).isEqualTo("CREATE");
    }

    @Test
    void logThrowsWhenActionBlank() {
        ActivityLogRequest request = new ActivityLogRequest(
                "  ", "Car", 1L, null, null, null);

        assertThatThrownBy(() -> service.log(request))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void getByEntityThrowsWhenTypeBlank() {
        assertThatThrownBy(() -> service.getByEntity("  ", 1L))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void getByEntityReturnsRepositoryResult() {
        ActivityLog log = new ActivityLog();
        log.setEntityType("Car");
        when(repository.findByEntityTypeAndEntityIdOrderByTimestampDesc("Car", 1L))
                .thenReturn(List.of(log));

        assertThat(service.getByEntity("Car", 1L)).containsExactly(log);
    }
}
