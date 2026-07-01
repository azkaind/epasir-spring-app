package com.example.security.controller;

import com.example.security.dto.request.KomoditasRequest;
import com.example.security.dto.response.ApiResponse;
import com.example.security.dto.response.KomoditasDropdownResponse;
import com.example.security.dto.response.KomoditasResponse;
import com.example.security.dto.response.PagedResponse;
import com.example.security.service.KomoditasService;
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
 * Base path: /api/master/komoditas
 */
@RestController
@RequestMapping("/api/master/komoditas")
@RequiredArgsConstructor
public class KomoditasController {

    private final KomoditasService komoditasService;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<KomoditasResponse>>> resolveAll(

            @RequestParam(value = "q", defaultValue = "")
            String keyword,

            @RequestParam(value = "pageSize", defaultValue = "10")
            int pageSize,

            @RequestParam(value = "pageNumber", defaultValue = "1")
            int pageNumber,

            @RequestParam(value = "sortBy", defaultValue = "createdAt")
            String sortBy,

            @RequestParam(value = "sortType", defaultValue = "DESC")
            String sortType) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        komoditasService.resolveAll(
                                keyword,
                                pageSize,
                                pageNumber,
                                sortBy,
                                sortType),
                        "Data berhasil diambil"));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<KomoditasDropdownResponse>>> getAll() {

        return ResponseEntity.ok(
                ApiResponse.success(
                        komoditasService.getAll(),
                        "Data berhasil diambil"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<KomoditasResponse>> resolveById(
            @PathVariable UUID id) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        komoditasService.resolveById(id),
                        "Data berhasil diambil"));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<KomoditasResponse>> create(

            @Valid
            @RequestBody
            KomoditasRequest request,

            @AuthenticationPrincipal
            UserDetails userDetails) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        komoditasService.create(
                                request,
                                userDetails.getUsername()),
                        "Komoditas berhasil ditambahkan"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<KomoditasResponse>> update(

            @PathVariable
            UUID id,

            @Valid
            @RequestBody
            KomoditasRequest request,

            @AuthenticationPrincipal
            UserDetails userDetails) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        komoditasService.update(
                                id,
                                request,
                                userDetails.getUsername()),
                        "Komoditas berhasil diupdate"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> softDelete(

            @PathVariable
            UUID id,

            @AuthenticationPrincipal
            UserDetails userDetails) {

        komoditasService.softDelete(id, userDetails.getUsername());

        return ResponseEntity.ok(
                ApiResponse.success(
                        "success",
                        "Komoditas berhasil dihapus"));
    }
}