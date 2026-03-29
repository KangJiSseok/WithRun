package kr.withrun.was.domain.reward.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import kr.withrun.was.global.common.entity.BaseEntity;

@Entity
@Table(name = "reward_draw_pools")
public class RewardDrawPool extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "reward_draw_pool_id")
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    protected RewardDrawPool() {
    }

    public static RewardDrawPool create(String name) {
        RewardDrawPool rewardDrawPool = new RewardDrawPool();
        rewardDrawPool.name = name;
        rewardDrawPool.active = false;
        return rewardDrawPool;
    }

    public void updateName(String name) {
        this.name = name;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isActive() {
        return active;
    }
}
