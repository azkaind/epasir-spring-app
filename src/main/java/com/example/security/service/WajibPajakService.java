package com.example.security.service;

import com.example.security.dto.request.WajibPajakRequest;
import com.example.security.dto.response.PagedResponse;
import com.example.security.dto.response.WajibPajakDropdownResponse;
import com.example.security.dto.response.WajibPajakResponse;

import java.util.List;
import java.util.UUID;

public interface WajibPajakService {
    PagedResponse<WajibPajakResponse> resolveAll(String q, int pageSize, int pageNumber,
                                                  String sortBy, String sortType);
    List<WajibPajakDropdownResponse> getAll();
    WajibPajakResponse resolveById(UUID id);
    WajibPajakResponse create(WajibPajakRequest req, String userId);
    WajibPajakResponse update(UUID id, WajibPajakRequest req, String userId);
    void softDelete(UUID id, String userId);
}
