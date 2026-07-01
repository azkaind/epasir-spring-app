package com.example.security.service.impl;

import com.example.security.dto.request.WajibPajakRequest;
import com.example.security.dto.response.PagedResponse;
import com.example.security.dto.response.WajibPajakDropdownResponse;
import com.example.security.dto.response.WajibPajakResponse;
import com.example.security.entity.WajibPajak;
import com.example.security.exception.UserNotFoundException;
import com.example.security.repository.WajibPajakQueryRepository;
import com.example.security.repository.WajibPajakRepository;
import com.example.security.service.WajibPajakService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class WajibPajakServiceImpl implements WajibPajakService {

    private final WajibPajakRepository      wpRepository;
    private final WajibPajakQueryRepository wpQueryRepository;

    @Override @Transactional(readOnly = true)
    public PagedResponse<WajibPajakResponse> resolveAll(String q, int pageSize, int pageNumber,
                                                         String sortBy, String sortType) {
        long total = wpQueryRepository.count(q);
        if (total == 0) return PagedResponse.of(List.of(), pageNumber, pageSize, 0L, null);
        List<WajibPajakResponse> items = wpQueryRepository.findAll(q, sortBy, sortType, pageSize, pageNumber);
        return PagedResponse.of(items, pageNumber, pageSize, total, null);
    }

    @Override @Transactional(readOnly = true)
    public List<WajibPajakDropdownResponse> getAll() {
        return wpRepository.findAllByIsDeletedFalseOrderByNamaWpAsc()
                .stream()
                .map(wp -> WajibPajakDropdownResponse.builder()
                        .id(wp.getId().toString())
                        .namaWp(wp.getNamaWp())
                        .kodeBprd(wp.getKodeBprd())
                        .build())
                .toList();
    }

    @Override @Transactional(readOnly = true)
    public WajibPajakResponse resolveById(UUID id) {
        WajibPajakResponse dto = wpQueryRepository.findById(id.toString());
        if (dto == null) throw new UserNotFoundException("Wajib Pajak dengan ID: " + id + " tidak ditemukan");
        return dto;
    }

    @Override
    public WajibPajakResponse create(WajibPajakRequest req, String userId) {
        WajibPajak entity = WajibPajak.builder()
                .kodeBprd(req.getKodeBprd())
                .kodeBjtm(req.getKodeBjtm())
                .namaWp(req.getNamaWp())
                .desa(req.getDesa())
                .kecamatan(req.getKecamatan())
                .iupUop(req.getIupUop())
                .komoditas(req.getKomoditas())
                .ijinBerlaku(req.getIjinBerlaku())
                .npwp(req.getNpwp())
                .statusPerizinan(req.getStatusPerizinan() != null ? req.getStatusPerizinan() : true)
                .noCustomer(req.getNoCustomer())
                .createdAt(LocalDateTime.now())
                .createdBy(userId)
                .isDeleted(false)
                .build();
        WajibPajak saved = wpRepository.save(entity);
        return resolveById(saved.getId());
    }

    @Override
    public WajibPajakResponse update(UUID id, WajibPajakRequest req, String userId) {
        WajibPajak existing = wpRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Wajib Pajak dengan ID: " + id + " tidak ditemukan"));
        existing.setKodeBprd(req.getKodeBprd());
        existing.setKodeBjtm(req.getKodeBjtm());
        existing.setNamaWp(req.getNamaWp());
        existing.setDesa(req.getDesa());
        existing.setKecamatan(req.getKecamatan());
        existing.setIupUop(req.getIupUop());
        existing.setKomoditas(req.getKomoditas());
        existing.setIjinBerlaku(req.getIjinBerlaku());
        existing.setNpwp(req.getNpwp());
        existing.setStatusPerizinan(req.getStatusPerizinan());
        existing.setNoCustomer(req.getNoCustomer());
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(userId);
        wpRepository.save(existing);
        return resolveById(id);
    }

    @Override
    public void softDelete(UUID id, String userId) {
        WajibPajak existing = wpRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("Wajib Pajak dengan ID: " + id + " tidak ditemukan"));
        existing.setIsDeleted(true);
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy(userId);
        wpRepository.save(existing);
    }
}
