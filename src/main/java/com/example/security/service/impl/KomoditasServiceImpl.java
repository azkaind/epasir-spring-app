package com.example.security.service.impl;

import com.example.security.dto.request.KomoditasRequest;
import com.example.security.dto.response.KomoditasDropdownResponse;
import com.example.security.dto.response.KomoditasResponse;
import com.example.security.dto.response.PagedResponse;
import com.example.security.entity.Komoditas;
import com.example.security.exception.UserNotFoundException;
import com.example.security.repository.KomoditasQueryRepository;
import com.example.security.repository.KomoditasRepository;
import com.example.security.service.KomoditasService;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class KomoditasServiceImpl implements KomoditasService {

    private final KomoditasRepository komoditasRepository;
    private final KomoditasQueryRepository komoditasQueryRepository;

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<KomoditasResponse> resolveAll(
            String q,
            int pageSize,
            int pageNumber,
            String sortBy,
            String sortType) {

        long total = komoditasQueryRepository.count(q);

        if (total == 0) {
            return PagedResponse.of(List.of(), pageNumber, pageSize, 0L, null);
        }

        List<KomoditasResponse> items =
                komoditasQueryRepository.findAll(
                        q,
                        sortBy,
                        sortType,
                        pageSize,
                        pageNumber);

        return PagedResponse.of(items, pageNumber, pageSize, total, null);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable("komoditasDropdown")
    public List<KomoditasDropdownResponse> getAll() {

        return komoditasRepository
                .findAllByIsDeletedFalseOrderByNamaAsc()
                .stream()
                .map(item -> KomoditasDropdownResponse.builder()
                        .id(item.getId().toString())
                        .nama(item.getNama())
                        .nominal(item.getNominal())
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public KomoditasResponse resolveById(UUID id) {

        KomoditasResponse dto =
                komoditasQueryRepository.findById(id.toString());

        if (dto == null) {
            throw new UserNotFoundException(
                    "Komoditas dengan ID: " + id + " tidak ditemukan");
        }

        return dto;
    }

    @Override
    @CacheEvict(value = "komoditasDropdown", allEntries = true)
    public KomoditasResponse create(
            KomoditasRequest request,
            String userId) {

        Komoditas entity = Komoditas.builder()
                .nama(request.getNama())
                .nominal(request.getNominal())
                .tonase(request.getTonase())
                .warnaBackground(request.getWarnaBackground())
                .warnaBackgroundNama(request.getWarnaBackgroundNama())
                .idKelompokKomoditas(request.getIdKelompokKomoditas())
                .nominalOpsen(request.getNominalOpsen())
                .persentaseOpsen(request.getPersentaseOpsen())
                .createdAt(LocalDateTime.now())
                .createdBy(userId)
                .isDeleted(false)
                .build();

        Komoditas saved = komoditasRepository.save(entity);

        return resolveById(saved.getId());
    }

    @Override
    @CacheEvict(value = "komoditasDropdown", allEntries = true)
    public KomoditasResponse update(
            UUID id,
            KomoditasRequest request,
            String userId) {

        Komoditas existing = komoditasRepository.findById(id)
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "Komoditas dengan ID: " + id + " tidak ditemukan"));

        existing.setNama(request.getNama());
        existing.setNominal(request.getNominal());
        existing.setTonase(request.getTonase());
        existing.setWarnaBackground(request.getWarnaBackground());
        existing.setWarnaBackgroundNama(request.getWarnaBackgroundNama());
        existing.setIdKelompokKomoditas(request.getIdKelompokKomoditas());
        existing.setNominalOpsen(request.getNominalOpsen());
        existing.setPersentaseOpsen(request.getPersentaseOpsen());

        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(userId);

        komoditasRepository.save(existing);

        return resolveById(id);
    }

    @Override
    @CacheEvict(value = "komoditasDropdown", allEntries = true)
    public void softDelete(UUID id, String userId) {

        Komoditas existing = komoditasRepository.findById(id)
                .orElseThrow(() ->
                        new UserNotFoundException(
                                "Komoditas dengan ID: " + id + " tidak ditemukan"));

        existing.setIsDeleted(true);
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(userId);

        komoditasRepository.save(existing);
    }
}