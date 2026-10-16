package com.example.lab5.mongorepository;

import com.example.lab5.document.ActivityLog;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ActivityLogRepository extends MongoRepository<ActivityLog, String> {

    List<ActivityLog> findByEntityTypeAndEntityIdOrderByTimestampDesc(String entityType, Long entityId);

    List<ActivityLog> findAllByOrderByTimestampDesc();
}
