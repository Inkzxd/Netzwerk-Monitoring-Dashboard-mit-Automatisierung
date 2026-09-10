package de.htwsaar.monitoring.model;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface CheckResultRepository extends JpaRepository<CheckResultEntity, Long> {

    List<CheckResultEntity> findAllByOrderByCheckedAtDesc();

    List<CheckResultEntity> findByDeviceNameOrderByCheckedAtDesc(String deviceName);

    List<CheckResultEntity> findByCheckedAtAfterOrderByCheckedAtDesc(LocalDateTime from);

    List<CheckResultEntity> findByCheckedAtBefore(LocalDateTime before);
}