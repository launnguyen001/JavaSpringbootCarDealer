package com.example.lab5.controller;

import com.example.lab5.dto.ActivityLogRequest;
import com.example.lab5.dto.ActivityLogResponse;
import com.example.lab5.mapper.ActivityLogMapper;
import com.example.lab5.service.ActivityLogService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// NoSQL (MongoDB): nhật ký hoạt động / audit log
@RestController
@RequestMapping("/api/activity-logs")
public class ActivityLogController {

    private final ActivityLogService service;
    private final ActivityLogMapper mapper;

    public ActivityLogController(ActivityLogService service, ActivityLogMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ActivityLogResponse log(@Valid @RequestBody ActivityLogRequest request) {
        return mapper.toResponse(service.log(request));
    }

    @GetMapping
    public List<ActivityLogResponse> getAll() {
        return service.getAll().stream().map(mapper::toResponse).toList();
    }

    @GetMapping("/entity")
    public List<ActivityLogResponse> getByEntity(
            @RequestParam String entityType,
            @RequestParam Long entityId
    ) {
        return service.getByEntity(entityType, entityId).stream().map(mapper::toResponse).toList();
    }
}
