package com.example.lab5.service;

import com.example.lab5.dto.CarItemDetailResponse;

public interface CarItemDetailService {
    CarItemDetailResponse getDetail(Long carItemId);
}
