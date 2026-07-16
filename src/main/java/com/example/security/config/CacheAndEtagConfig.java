package com.example.security.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

import java.util.concurrent.TimeUnit;

@Configuration
public class CacheAndEtagConfig {

    /**
     * Konfigurasi ETag Filter untuk response 304 Not Modified.
     * Mengaktifkan HTTP 304 agar client tidak men-download data yang sama berulang kali.
     */
    @Bean
    public ShallowEtagHeaderFilter shallowEtagHeaderFilter() {
        return new ShallowEtagHeaderFilter();
    }

    /**
     * Konfigurasi Caffeine Cache Manager dengan TTL 30 detik.
     * @Primary agar Spring memilih bean ini ketika ada lebih dari satu CacheManager.
     */
    @Bean
    @Primary
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(30, TimeUnit.SECONDS)
                .maximumSize(1000));
        return cacheManager;
    }
}
