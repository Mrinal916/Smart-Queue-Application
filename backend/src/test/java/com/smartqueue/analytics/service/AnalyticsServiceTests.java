package com.smartqueue.analytics.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import com.smartqueue.analytics.repository.AnalyticsRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTests {
  @Mock AnalyticsRepository repository;
  @InjectMocks AnalyticsService service;

  @Test
  void dashboardUsesBookingCountRatherThanWaitingCount() {
    when(repository.activeOffices()).thenReturn(2L);
    when(repository.activeCounters()).thenReturn(3L);
    when(repository.activeQueues()).thenReturn(4L);
    when(repository.activeOfficers()).thenReturn(5L);
    when(repository.citizens()).thenReturn(6L);
    when(repository.bookingsToday()).thenReturn(9L);
    when(repository.statusToday("WAITING")).thenReturn(7L);
    when(repository.statusToday("COMPLETED")).thenReturn(8L);
    when(repository.statusToday("CANCELLED")).thenReturn(1L);
    when(repository.statusToday("NO_SHOW")).thenReturn(2L);
    var result = service.dashboard();
    assertEquals(9L, result.totalBookingsToday());
    assertEquals(7L, result.tokensWaiting());
  }

  @Test
  void rejectsInvertedDateRange() {
    assertThrows(
        IllegalArgumentException.class,
        () -> service.report(LocalDate.of(2026, 2, 2), LocalDate.of(2026, 2, 1)));
  }
}
