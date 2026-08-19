package com.smartqueue.analytics.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.smartqueue.analytics.dto.ReportResponse;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnalyticsExportServiceTests {
  @Mock AnalyticsService analyticsService;
  @InjectMocks AnalyticsExportService exportService;
  private final LocalDate from = LocalDate.of(2026, 7, 1);
  private final LocalDate to = LocalDate.of(2026, 7, 2);
  private final ReportResponse report = new ReportResponse(from, to, 10, 2, 6, 1, 1, 10, 10, 60);

  @Test
  void createsCsvReport() {
    when(analyticsService.report(from, to)).thenReturn(report);
    String csv = new String(exportService.csv(from, to));
    assertTrue(csv.startsWith("from,to,bookings"));
    assertTrue(csv.contains("2026-07-01,2026-07-02,10"));
  }

  @Test
  void createsReadableExcelReport() throws Exception {
    when(analyticsService.report(from, to)).thenReturn(report);
    try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(exportService.excel(from, to)))) {
      assertEquals("Report", workbook.getSheetAt(0).getSheetName());
      assertEquals("Bookings", workbook.getSheetAt(0).getRow(0).getCell(2).getStringCellValue());
      assertEquals(10d, workbook.getSheetAt(0).getRow(1).getCell(2).getNumericCellValue());
    }
  }
}
