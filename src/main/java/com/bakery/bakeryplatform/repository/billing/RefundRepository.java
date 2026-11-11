package com.bakery.bakeryplatform.repository.billing;

import com.bakery.bakeryplatform.model.billing.RefundRequest;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefundRepository extends JpaRepository<RefundRequest, Long> {

}
