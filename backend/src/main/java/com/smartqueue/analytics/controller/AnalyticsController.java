package com.smartqueue.analytics.controller;

import com.smartqueue.analytics.dto.CounterAnalyticsResponse;
import com.smartqueue.analytics.dto.DashboardSummaryResponse;
import com.smartqueue.analytics.dto.OfficeAnalyticsResponse;
import com.smartqueue.analytics.dto.OfficerPerformanceResponse;
import com.smartqueue.analytics.dto.QueueStatisticsResponse;
import com.smartqueue.analytics.dto.ReportResponse;
import com.smartqueue.analytics.dto.ServicePerformanceResponse;
import com.smartqueue.analytics.service.AnalyticsService;
import com.smartqueue.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
@SecurityRequirement(name = "bearerAuth")
public class AnalyticsController {
  private final AnalyticsService service;
  private final com.smartqueue.analytics.service.AnalyticsExportService exportService;

  public AnalyticsController(
      AnalyticsService service,
      com.smartqueue.analytics.service.AnalyticsExportService exportService) {
    this.service = service;
    this.exportService = exportService;
  }

  @Operation(
      summary = "Dashboard summary",
      description = "Administrator-only PostgreSQL operational summary.")
  @GetMapping("/dashboard")
  public ApiResponse<DashboardSummaryResponse> dashboard() {
    return ApiResponse.success(service.dashboard());
  }

  @Operation(
      summary = "Date-range report",
      description =
          "Administrator-only report. Optional filters restrict the PostgreSQL aggregation scope.")
  @GetMapping("/reports")
  public ApiResponse<ReportResponse> report(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
      @RequestParam(required = false) Long officeId,
      @RequestParam(required = false) Long departmentId,
      @RequestParam(required = false) Long serviceId,
      @RequestParam(required = false) Long counterId,
      @RequestParam(required = false) Long officerId) {
    return ApiResponse.success(
        service.report(from, to, officeId, departmentId, serviceId, counterId, officerId));
  }

  @Operation(summary = "Daily report")
  @GetMapping("/reports/daily")
  public ApiResponse<ReportResponse> daily(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
    return ApiResponse.success(service.report(date, date));
  }

  @Operation(summary = "Weekly report")
  @GetMapping("/reports/weekly")
  public ApiResponse<ReportResponse> weekly(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekOf) {
    LocalDate from =
        weekOf.with(
            java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY));
    return ApiResponse.success(service.report(from, from.plusDays(6)));
  }

  @Operation(summary = "Monthly report")
  @GetMapping("/reports/monthly")
  public ApiResponse<ReportResponse> monthly(@RequestParam int year, @RequestParam int month) {
    java.time.YearMonth period = java.time.YearMonth.of(year, month);
    return ApiResponse.success(service.report(period.atDay(1), period.atEndOfMonth()));
  }

  @Operation(summary = "Officer performance")
  @GetMapping("/performance/officers/{officerId}")
  public ApiResponse<OfficerPerformanceResponse> officer(
      @org.springframework.web.bind.annotation.PathVariable Long officerId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return ApiResponse.success(service.officerPerformance(officerId, from, to));
  }

  @Operation(summary = "Counter performance")
  @GetMapping("/performance/counters/{counterId}")
  public ApiResponse<CounterAnalyticsResponse> counter(
      @org.springframework.web.bind.annotation.PathVariable Long counterId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return ApiResponse.success(service.counterPerformance(counterId, from, to));
  }

  @Operation(summary = "Service performance")
  @GetMapping("/performance/services/{serviceId}")
  public ApiResponse<ServicePerformanceResponse> service(
      @org.springframework.web.bind.annotation.PathVariable Long serviceId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return ApiResponse.success(this.service.servicePerformance(serviceId, from, to));
  }

  @Operation(summary = "Office performance")
  @GetMapping("/performance/offices/{officeId}")
  public ApiResponse<OfficeAnalyticsResponse> office(
      @org.springframework.web.bind.annotation.PathVariable Long officeId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return ApiResponse.success(service.officePerformance(officeId, from, to));
  }

  @Operation(summary = "Queue statistics")
  @GetMapping("/statistics")
  public ApiResponse<QueueStatisticsResponse> statistics(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return ApiResponse.success(service.statistics(from, to));
  }

  @Operation(summary = "Export CSV report")
  @GetMapping(value = "/exports/report.csv", produces = "text/csv")
  public ResponseEntity<byte[]> csv(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return download(
        exportService.csv(from, to), "report.csv", MediaType.parseMediaType("text/csv"));
  }

  @Operation(summary = "Export Excel report")
  @GetMapping(
      value = "/exports/report.xlsx",
      produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
  public ResponseEntity<byte[]> excel(
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return download(
        exportService.excel(from, to),
        "report.xlsx",
        MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
  }

  @Operation(summary = "Export officer performance CSV")
  @GetMapping(value = "/exports/officers/{officerId}.csv", produces = "text/csv")
  public ResponseEntity<byte[]> officerCsv(
      @org.springframework.web.bind.annotation.PathVariable Long officerId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return download(
        exportService.officerCsv(officerId, from, to),
        "officer-performance.csv",
        MediaType.parseMediaType("text/csv"));
  }

  @Operation(summary = "Export service performance CSV")
  @GetMapping(value = "/exports/services/{serviceId}.csv", produces = "text/csv")
  public ResponseEntity<byte[]> serviceCsv(
      @org.springframework.web.bind.annotation.PathVariable Long serviceId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return download(
        exportService.serviceCsv(serviceId, from, to),
        "service-performance.csv",
        MediaType.parseMediaType("text/csv"));
  }

  @Operation(summary = "Export officer performance Excel")
  @GetMapping(
      value = "/exports/officers/{officerId}.xlsx",
      produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
  public ResponseEntity<byte[]> officerExcel(
      @org.springframework.web.bind.annotation.PathVariable Long officerId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return download(
        exportService.officerExcel(officerId, from, to),
        "officer-performance.xlsx",
        MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
  }

  @Operation(summary = "Export service performance Excel")
  @GetMapping(
      value = "/exports/services/{serviceId}.xlsx",
      produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
  public ResponseEntity<byte[]> serviceExcel(
      @org.springframework.web.bind.annotation.PathVariable Long serviceId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
    return download(
        exportService.serviceExcel(serviceId, from, to),
        "service-performance.xlsx",
        MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
  }

  private ResponseEntity<byte[]> download(byte[] body, String filename, MediaType type) {
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
        .contentType(type)
        .body(body);
  }
}
