package com.smarthospital.modules.ipd.repository;

import com.smarthospital.modules.ipd.domain.IpdCharge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface IpdChargeRepository extends JpaRepository<IpdCharge, UUID> {
}
