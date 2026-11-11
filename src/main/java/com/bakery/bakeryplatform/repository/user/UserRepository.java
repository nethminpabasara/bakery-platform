package com.bakery.bakeryplatform.repository.user;

import com.bakery.bakeryplatform.model.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository

public interface UserRepository extends JpaRepository<User, Long> {
    User findByUsername(String username);

    User findByEmail(String email);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select u from User u where u.id = :id")
    java.util.Optional<User> lockForWebsiteCheckout(@org.springframework.data.repository.query.Param("id") Long id);
}
