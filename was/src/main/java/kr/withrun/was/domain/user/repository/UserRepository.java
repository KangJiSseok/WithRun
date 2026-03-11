package kr.withrun.was.domain.user.repository;

import kr.withrun.was.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @Query("select user from User user where user.id = :userId and user.deletedAt is null and user.isActive = true")
    Optional<User> findActiveUserById(@Param("userId") Long userId);
}
