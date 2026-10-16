package com.example.lab5.mapper;

import com.example.lab5.document.ActivityLog;
import com.example.lab5.dto.ActivityLogRequest;
import com.example.lab5.dto.ActivityLogResponse;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class ActivityLogMapper {

    public ActivityLog toEntity(ActivityLogRequest request) {
        if (request == null) {
            return null;
        }
        ActivityLog log = new ActivityLog();
        log.setAction(request.action());
        log.setEntityType(request.entityType());
        log.setEntityId(request.entityId());
        log.setDescription(request.description());
        log.setPerformedBy(request.performedBy());
        log.setTimestamp(request.timestamp() == null ? LocalDateTime.now() : request.timestamp());
        return log;
    }

    public ActivityLogResponse toResponse(ActivityLog log) {
        return new ActivityLogResponse(
                log.getId(),
                log.getAction(),
                log.getEntityType(),
                log.getEntityId(),
                log.getDescription(),
                log.getPerformedBy(),
                log.getTimestamp()
        );
    }
}
