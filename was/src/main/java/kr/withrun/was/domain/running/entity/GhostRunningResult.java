package kr.withrun.was.domain.running.entity;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import kr.withrun.was.domain.running.type.GhostResultStatus;
import kr.withrun.was.domain.user.entity.User;
import kr.withrun.was.global.common.entity.BaseEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "ghost_running_results",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_ghost_running_results_session",
                        columnNames = "running_session_id"
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GhostRunningResult extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ghost_running_result_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_status", nullable = false, length = 16)
    private GhostResultStatus resultStatus;

    @Column(name = "time_gap_sec", nullable = false)
    private Integer timeGapSec = 0;

    @Column(name = "distance_gap_m", nullable = false)
    private Integer distanceGapM = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;


    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "running_session_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_ghost_running_results_running_session")
    )
    private RunningSession runningSession;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "ghost_target_running_session_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_ghost_running_results_ghost_target_running_session")
    )
    private RunningSession ghostTargetRunningSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "target_user_id",
            foreignKey = @ForeignKey(name = "fk_ghost_running_results_target_user")
    )
    private User targetUser;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (timeGapSec == null) {
            timeGapSec = 0;
        }
        if (distanceGapM == null) {
            distanceGapM = 0;
        }
    }
}
