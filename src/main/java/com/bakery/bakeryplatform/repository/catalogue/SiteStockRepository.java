package com.bakery.bakeryplatform.repository.catalogue;

import com.bakery.bakeryplatform.model.catalogue.SiteStock;

import java.util.Optional;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
public interface SiteStockRepository extends JpaRepository<SiteStock,String>{
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SiteStock s where s.productId=:id")
    Optional<SiteStock> lock(@Param("id") String id);
}
