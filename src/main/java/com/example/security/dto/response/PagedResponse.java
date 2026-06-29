package com.example.security.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Response wrapper untuk list data dengan pagination meta.
 * Kompatibel dengan format pagination yang dipakai sistem Go lama.
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PagedResponse<T> {

    private List<T> items;
    private Meta    meta;

    @Data
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Meta {
        private int    page;
        private int    limit;
        private long   totalData;
        private int    totalPage;
        /** Total nominal seluruh data (bukan per halaman) */
        private Double totalRupiah;
    }

    /**
     * Factory method — menghitung totalPage secara otomatis.
     */
    public static <T> PagedResponse<T> of(List<T> items,
                                          int page,
                                          int limit,
                                          long totalData,
                                          Double totalRupiah) {
        int totalPage = (int) Math.ceil((double) totalData / limit);
        return PagedResponse.<T>builder()
                .items(items)
                .meta(Meta.builder()
                        .page(page)
                        .limit(limit)
                        .totalData(totalData)
                        .totalPage(totalPage)
                        .totalRupiah(totalRupiah)
                        .build())
                .build();
    }
}
