package com.example.lab5.service;

public interface ReportExportService {

    /**
     * Xuất báo cáo dạng file.
     *
     * @param type   dashboard-summary | top-selling-cars | inventory-by-brand |
     *               revenue-summary | orders-by-status | revenue-by-month
     * @param format xlsx | csv
     */
    byte[] export(String type, String format);

    String suggestedFilename(String type, String format);
}
