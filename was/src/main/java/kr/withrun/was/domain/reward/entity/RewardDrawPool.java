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

    @Column(name = "cards_per_draw", nullable = false)
    private Integer cardsPerDraw;

    @Column(name = "miss_weight", nullable = false)
    private Integer missWeight;

    protected RewardDrawPool() {
    }

    public static RewardDrawPool create(String name, int cardsPerDraw, int missWeight) {
        RewardDrawPool rewardDrawPool = new RewardDrawPool();
        rewardDrawPool.name = name;
        rewardDrawPool.active = false;
        rewardDrawPool.cardsPerDraw = cardsPerDraw;
        rewardDrawPool.missWeight = missWeight;
        return rewardDrawPool;
    }

    public void updateConfiguration(String name, int cardsPerDraw, int missWeight) {
        this.name = name;
        this.cardsPerDraw = cardsPerDraw;
        this.missWeight = missWeight;
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

    public Integer getCardsPerDraw() {
        return cardsPerDraw;
    }

    public Integer getMissWeight() {
        return missWeight;
    }
}
