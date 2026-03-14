package kr.withrun.was.domain.user.repository;

import kr.withrun.was.domain.user.entity.User;
import kr.withrun.was.domain.user.repository.query.UserQueryRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long>, UserQueryRepository {
}
