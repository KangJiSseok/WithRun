package kr.withrun.was.domain.reward.repository;

import kr.withrun.was.domain.reward.entity.RewardItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RewardItemRepository extends JpaRepository<RewardItem, Long> {

    Page<RewardItem> findAllByDeletedAtIsNull(Pageable pageable);

    Page<RewardItem> findAllByDeletedAtIsNullAndActive(boolean active, Pageable pageable);

    List<RewardItem> findAllByDeletedAtIsNullAndActiveTrueOrderByCreatedAtDescIdDesc();

    @Query("""
            select distinct rewardItem
            from RewardDrawPoolItem rewardDrawPoolItem
            join rewardDrawPoolItem.rewardDrawPool rewardDrawPool
            join rewardDrawPoolItem.rewardItem rewardItem
            where rewardDrawPoolItem.deletedAt is null
              and rewardDrawPool.deletedAt is null
              and rewardDrawPool.active = true
              and rewardItem.deletedAt is null
              and rewardItem.active = true
            order by rewardItem.createdAt desc, rewardItem.id desc
            """)
    List<RewardItem> findAllShowcaseItemsOrderByCreatedAtDescIdDesc();

    Optional<RewardItem> findByIdAndDeletedAtIsNull(Long id);
}
