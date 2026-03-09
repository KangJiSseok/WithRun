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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import kr.withrun.was.domain.running.type.DeviceSource;
import kr.withrun.was.domain.user.entity.User;
import kr.withrun.was.global.common.entity.BaseEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "running_health_samples",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_running_health_samples_session_time",
                        columnNames = {"running_session_id", "sampled_at"}
                )
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RunningHealthSample extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "running_health_sample_id")
    private Long id;

    @Column(name = "sampled_at", nullable = false)
    private LocalDateTime sampledAt;

    @Column(name = "heart_rate")
    private Short heartRate;

    @Column(name = "speed_mps")
    private Double speedMps;

    @Column(name = "pace_sec_per_km")
    private Integer paceSecPerKm;

    @Column(name = "distance_m")
    private Integer distanceM;

    @Column(name = "calories_kcal")
    private Double caloriesKcal;

    @Column(name = "altitude_m")
    private Double altitudeM;

    @Column(name = "cadence_spm")
    private Short cadenceSpm;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_device", nullable = false, length = 32)
    private DeviceSource sourceDevice;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "running_session_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_running_health_samples_running_session")
    )
    private RunningSession runningSession;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_running_health_samples_user")
    )
    private User user;


}