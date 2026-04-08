package com.krushikranti.repository;

import com.krushikranti.model.FraudAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FraudAlertRepository extends JpaRepository<FraudAlert, Long> {

    List<FraudAlert> findAllByOrderByCreatedAtDesc();

    boolean existsByUserIdAndTypeAndStatus(Long userId, String type, String status);
}
