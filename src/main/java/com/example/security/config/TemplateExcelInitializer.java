package com.example.security.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Membuat file template Excel saat startup jika belum ada.
 *
 * Output: src/main/resources/templates/template_import_kartu.xlsx
 *         (diperlukan oleh KartuService.downloadTemplate())
 *
 * Header kolom:
 *   A=No | B=No RFID | C=Kode Kartu | D=No VA | E=Nama WP | F=Nama Komoditas
 */
@Component
@Slf4j
public class TemplateExcelInitializer implements ApplicationRunner {

    private static final String TEMPLATE_CLASSPATH = "templates/template_import_kartu.xlsx";

    @Override
    public void run(ApplicationArguments args) {
        // Cari lokasi resources di classpath root (src/main/resources)
        try {
            // Cek apakah sudah ada di classpath
            InputStream existing = getClass().getClassLoader().getResourceAsStream(TEMPLATE_CLASSPATH);
            if (existing != null) {
                existing.close();
                log.info("Template Excel kartu sudah ada, skip generate.");
                return;
            }
        } catch (Exception ignored) {}

        // Coba tulis ke direktori target (berlaku saat dev dengan Maven)
        try {
            // Cari path target/classes/templates atau src/main/resources/templates
            String[] candidatePaths = {
                "src/main/resources/templates",
                "target/classes/templates"
            };

            for (String dir : candidatePaths) {
                Path templateDir  = Paths.get(dir);
                Path templateFile = templateDir.resolve("template_import_kartu.xlsx");

                Files.createDirectories(templateDir);
                generateTemplate(templateFile);
                log.info("Template Excel kartu berhasil dibuat: {}", templateFile.toAbsolutePath());
            }
        } catch (IOException e) {
            log.warn("Gagal membuat template Excel kartu: {}. " +
                     "Taruh file secara manual di src/main/resources/templates/template_import_kartu.xlsx",
                     e.getMessage());
        }
    }

    private void generateTemplate(Path outputPath) throws IOException {
        if (Files.exists(outputPath)) return; // jangan overwrite jika sudah ada

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Import Kartu");

            // Style header — bold, background kuning muda
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);
            headerStyle.setBorderTop(BorderStyle.THIN);
            headerStyle.setBorderLeft(BorderStyle.THIN);
            headerStyle.setBorderRight(BorderStyle.THIN);

            // Baris header (row 0)
            Row headerRow = sheet.createRow(0);
            String[] headers = {"No", "No RFID", "Kode Kartu", "No VA", "Nama WP", "Nama Komoditas"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, i == 0 ? 1500 : 5000);
            }

            // Baris contoh (row 1)
            Row exampleRow = sheet.createRow(1);
            exampleRow.createCell(0).setCellValue(1);
            exampleRow.createCell(1).setCellValue("RFID001");
            exampleRow.createCell(2).setCellValue("KARTU001");
            exampleRow.createCell(3).setCellValue("VA001");
            exampleRow.createCell(4).setCellValue("Nama Wajib Pajak Contoh");
            exampleRow.createCell(5).setCellValue("Pasir Urug");

            try (OutputStream os = Files.newOutputStream(outputPath)) {
                workbook.write(os);
            }
        }
    }
}
