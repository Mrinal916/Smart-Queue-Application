package com.smartqueue.analytics.service;

import com.smartqueue.analytics.dto.ReportResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsExportService {
  private final AnalyticsService analyticsService;

  public AnalyticsExportService(AnalyticsService analyticsService) {
    this.analyticsService = analyticsService;
  }

  public byte[] csv(java.time.LocalDate from, java.time.LocalDate to) {
    ReportResponse r = analyticsService.report(from, to);
    String content =
        "from,to,bookings,waiting,completed,cancelled,no_shows,cancellation_rate,no_show_rate,completion_rate\n"
            + String.join(
                ",",
                r.from().toString(),
                r.to().toString(),
                String.valueOf(r.bookings()),
                String.valueOf(r.waiting()),
                String.valueOf(r.completed()),
                String.valueOf(r.cancelled()),
                String.valueOf(r.noShows()),
                String.valueOf(r.cancellationRate()),
                String.valueOf(r.noShowRate()),
                String.valueOf(r.completionRate()))
            + "\n";
    return content.getBytes(StandardCharsets.UTF_8);
  }

  public byte[] excel(java.time.LocalDate from, java.time.LocalDate to) {
    ReportResponse r = analyticsService.report(from, to);
    try (XSSFWorkbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream output = new ByteArrayOutputStream()) {
      var sheet = workbook.createSheet("Report");
      String[] headers = {
        "From",
        "To",
        "Bookings",
        "Waiting",
        "Completed",
        "Cancelled",
        "No shows",
        "Cancellation rate",
        "No-show rate",
        "Completion rate"
      };
      var header = sheet.createRow(0);
      for (int i = 0; i < headers.length; i++) header.createCell(i).setCellValue(headers[i]);
      var row = sheet.createRow(1);
      Object[] values = {
        r.from().toString(),
        r.to().toString(),
        r.bookings(),
        r.waiting(),
        r.completed(),
        r.cancelled(),
        r.noShows(),
        r.cancellationRate(),
        r.noShowRate(),
        r.completionRate()
      };
      for (int i = 0; i < values.length; i++) {
        if (values[i] instanceof Number n) row.createCell(i).setCellValue(n.doubleValue());
        else row.createCell(i).setCellValue(values[i].toString());
        sheet.autoSizeColumn(i);
      }
      workbook.write(output);
      return output.toByteArray();
    } catch (IOException exception) {
      throw new IllegalStateException("Could not generate Excel report", exception);
    }
  }

  public byte[] officerCsv(Long officerId, java.time.LocalDate from, java.time.LocalDate to) {
    var r = analyticsService.officerPerformance(officerId, from, to);
    return ("officer_id,tokens_served,skipped_tokens,no_shows,average_service_minutes\n"
            + r.officerId()
            + ","
            + r.tokensServed()
            + ","
            + r.skippedTokens()
            + ","
            + r.noShows()
            + ","
            + r.averageServiceMinutes()
            + "\n")
        .getBytes(StandardCharsets.UTF_8);
  }

  public byte[] serviceCsv(Long serviceId, java.time.LocalDate from, java.time.LocalDate to) {
    var r = analyticsService.servicePerformance(serviceId, from, to);
    return ("service_id,daily_volume,average_wait_minutes,average_service_minutes\n"
            + r.serviceId()
            + ","
            + r.dailyVolume()
            + ","
            + r.averageWaitMinutes()
            + ","
            + r.averageServiceMinutes()
            + "\n")
        .getBytes(StandardCharsets.UTF_8);
  }

  public byte[] officerExcel(Long officerId, java.time.LocalDate from, java.time.LocalDate to) {
    var r = analyticsService.officerPerformance(officerId, from, to);
    return workbook(
        new String[] {
          "Officer ID", "Tokens served", "Skipped", "No shows", "Average service minutes"
        },
        new Object[] {
          r.officerId(), r.tokensServed(), r.skippedTokens(), r.noShows(), r.averageServiceMinutes()
        });
  }

  public byte[] serviceExcel(Long serviceId, java.time.LocalDate from, java.time.LocalDate to) {
    var r = analyticsService.servicePerformance(serviceId, from, to);
    return workbook(
        new String[] {
          "Service ID", "Daily volume", "Average wait minutes", "Average service minutes"
        },
        new Object[] {
          r.serviceId(), r.dailyVolume(), r.averageWaitMinutes(), r.averageServiceMinutes()
        });
  }

  private byte[] workbook(String[] headers, Object[] values) {
    try (XSSFWorkbook w = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      var s = w.createSheet("Performance");
      var h = s.createRow(0);
      var r = s.createRow(1);
      for (int i = 0; i < headers.length; i++) {
        h.createCell(i).setCellValue(headers[i]);
        if (values[i] instanceof Number n) r.createCell(i).setCellValue(n.doubleValue());
        else r.createCell(i).setCellValue(values[i].toString());
        s.autoSizeColumn(i);
      }
      w.write(out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new IllegalStateException("Could not generate Excel export", e);
    }
  }
}
