package kr.withrun.was.domain.device.entity;

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
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import kr.withrun.was.domain.device.type.DeviceType;
import kr.withrun.was.domain.running.entity.RunningSession;
import kr.withrun.was.domain.user.entity.User;
import kr.withrun.was.global.common.entity.BaseEntity;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "device_sync_states")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Device extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "device_sync_states_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(
            name = "user_id",
            foreignKey = @ForeignKey(name = "fk_device_sync_states_user")
    )
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", length = 16, nullable = false)
    private DeviceType deviceType;

    @Column(name = "device_identifier", length = 128, nullable = false)
    private String deviceIdentifier;

    @Column(name = "last_synced_sampled_at")
    private LocalDateTime lastSyncedSampledAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "running_session_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_device_sync_states_session")
    )
    private RunningSession runningSession;
}
