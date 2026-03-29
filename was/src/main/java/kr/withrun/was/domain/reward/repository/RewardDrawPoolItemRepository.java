package kr.withrun.was.domain.reward.repository;

import kr.withrun.was.domain.reward.entity.RewardDrawPoolItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RewardDrawPoolItemRepository extends JpaRepository<RewardDrawPoolItem, Long> {

    List<RewardDrawPoolItem> findAllByRewardDrawPoolIdAndDeletedAtIsNullOrderByIdAsc(Long rewardDrawPoolId);
}
