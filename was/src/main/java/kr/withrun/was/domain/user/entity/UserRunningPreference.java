package kr.withrun.was.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import kr.withrun.was.global.common.entity.BaseEntity;
import kr.withrun.was.global.common.type.Difficulty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "user_running_preferences")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserRunningPreference extends BaseEntity {
    
    @Id
    @Column(name = "user_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_difficulty", nullable = false)
    private Difficulty preferredDifficulty;

    @Column(name = "preferred_distance_km", nullable = false)
    private Double preferredDistanceKm;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

}
