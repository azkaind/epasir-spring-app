package com.example.security.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.file")
@Getter @Setter
public class FileStorageProperties {
    private String baseDir         = "./storage";
    private String buktiTransferDir = "bukti-transfer";
    private String templateExcelDir = "template-excel";
    private int    maxSizeMb       = 5;
}
