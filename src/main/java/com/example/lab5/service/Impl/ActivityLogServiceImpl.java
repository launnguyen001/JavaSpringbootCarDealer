package com.example.lab5.service.Impl;

import com.example.lab5.document.ActivityLog;
import com.example.lab5.dto.ActivityLogRequest;
import com.example.lab5.exception.BadRequestException;
import com.example.lab5.mapper.ActivityLogMapper;
import com.example.lab5.mongorepository.ActivityLogRepository;
import com.example.lab5.service.ActivityLogService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ActivityLogServiceImpl implements ActivityLogService {

    private final ActivityLogRepository repository;
    private final ActivityLogMapper mapper;

    public ActivityLogServiceImpl(ActivityLogRepository repository, ActivityLogMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    public ActivityLog log(ActivityLogRequest request) {
        if (request == null || request.action() == null || request.action().isBlank()) {
            throw new BadRequestException("Hành động không được để trống");
        }
        return repository.save(mapper.toEntity(request));
    }

    @Override
    public List<ActivityLog> getAll() {
        return repository.findAllByOrderByTimestampDesc();
    }

    @Override
    public List<ActivityLog> getByEntity(String entityType, Long entityId) {
        if (entityType == null || entityType.isBlank()) {
            throw new BadRequestException("Loại thực thể không được để trống");
        }
        return repository.findByEntityTypeAndEntityIdOrderByTimestampDesc(entityType, entityId);
    }
}
