package com.bakery.bakeryplatform.repository.cake;

import com.bakery.bakeryplatform.model.cake.CakeDesign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CakeDesignRepository extends JpaRepository<CakeDesign, Long> {
}