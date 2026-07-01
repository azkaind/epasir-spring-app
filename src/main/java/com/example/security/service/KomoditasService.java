package com.example.security.service;

import com.example.security.dto.request.KomoditasRequest;
import com.example.security.dto.response.KomoditasDropdownResponse;
import com.example.security.dto.response.KomoditasResponse;
import com.example.security.dto.response.PagedResponse;

import java.util.List;
import java.util.UUID;

public interface KomoditasService {

    PagedResponse<KomoditasResponse> resolveAll(
            String q,
            int pageSize,
            int pageNumber,
            String sortBy,
            String sortType);

    List<KomoditasDropdownResponse> getAll();

    KomoditasResponse resolveById(UUID id);

    KomoditasResponse create(KomoditasRequest request, String userId);

    KomoditasResponse update(UUID id, KomoditasRequest request, String userId);

    void softDelete(UUID id, String userId);
}