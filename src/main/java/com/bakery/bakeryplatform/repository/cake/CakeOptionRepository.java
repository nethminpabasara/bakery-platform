package com.bakery.bakeryplatform.repository.cake;

import com.bakery.bakeryplatform.model.cake.CakeOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CakeOptionRepository extends JpaRepository<CakeOption, Long> {
}