package kr.withrun.was.domain.running.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import kr.withrun.was.domain.user.entity.User;
import kr.withrun.was.global.common.entity.BaseEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Getter
@Entity
@Table(
        name = "running_gps_samples",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_running_gps_samples_session_time",
                        columnNames = {"running_session_id", "sampled_at"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RunningGpsSample extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "running_gps_sample_id")
    private Long id;

    @Column(name = "sampled_at", nullable = false)
    private LocalDateTime sampledAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "running_session_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_running_gps_samples_running_session")
    )
    private RunningSession runningSession;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_running_gps_samples_user")
    )
    private User user;

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;

    @Column(name = "altitude_m")
    private Double altitudeM;

    @Column(name = "accuracy_m")
    private Double accuracyM;

    @Column(name = "bearing_deg")
    private Double bearingDeg;
}
