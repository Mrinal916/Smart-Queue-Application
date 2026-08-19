package com.smartqueue.counter.repository;

import com.smartqueue.counter.entity.CounterStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CounterStatusHistoryRepository extends JpaRepository<CounterStatusHistory, Long> {}
