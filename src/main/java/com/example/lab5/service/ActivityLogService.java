package com.example.lab5.service;

import com.example.lab5.document.ActivityLog;
import com.example.lab5.dto.ActivityLogRequest;

import java.util.List;

public interface ActivityLogService {

    ActivityLog log(ActivityLogRequest request);

    List<ActivityLog> getAll();

    List<ActivityLog> getByEntity(String entityType, Long entityId);
}
