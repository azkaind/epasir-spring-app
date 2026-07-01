package com.example.security.controller;

import com.example.security.dto.request.WajibPajakRequest;
import com.example.security.dto.response.ApiResponse;
import com.example.security.dto.response.PagedResponse;
import com.example.security.dto.response.WajibPajakDropdownResponse;
import com.example.security.dto.response.WajibPajakResponse;
import com.example.security.service.WajibPajakService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Base path: /api/master/wajib-pajak
 * Identik dengan sistem Go lama: /master/wajib-pajak
 */
@RestController
@RequestMapping("/api/master/wajib-pajak")
@RequiredArgsConstructor
public class WajibPajakController {

    private final WajibPajakService wpService;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<WajibPajakResponse>>> resolveAll(
            @RequestParam(value = "q",          defaultValue = "")         String keyword,
            @RequestParam(value = "pageSize",   defaultValue = "10")       int    pageSize,
            @RequestParam(value = "pageNumber", defaultValue = "1")        int    pageNumber,
            @RequestParam(value = "sortBy",     defaultValue = "createdAt") String sortBy,
            @RequestParam(value = "sortType",   defaultValue = "DESC")     String sortType) {
        return ResponseEntity.ok(ApiResponse.success(
                wpService.resolveAll(keyword, pageSize, pageNumber, sortBy, sortType),
                "Data berhasil diambil"));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<WajibPajakDropdownResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(wpService.getAll(), "Data berhasil diambil"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WajibPajakResponse>> resolveById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(wpService.resolveById(id), "Data berhasil diambil"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<WajibPajakResponse>> create(
            @Valid @RequestBody WajibPajakRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                ApiResponse.success(wpService.create(request, userDetails.getUsername()),
                        "Wajib Pajak berhasil ditambahkan"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<WajibPajakResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody WajibPajakRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.success(
                wpService.update(id, request, userDetails.getUsername()),
                "Wajib Pajak berhasil diupdate"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> softDelete(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        wpService.softDelete(id, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("success", "Wajib Pajak berhasil dihapus"));
    }
}
