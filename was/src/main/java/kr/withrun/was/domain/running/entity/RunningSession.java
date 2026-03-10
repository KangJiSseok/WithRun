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
import kr.withrun.was.domain.course.entity.Course;
import kr.withrun.was.domain.course.vo.Coordinates;
import kr.withrun.was.domain.running.type.RunningMode;
import kr.withrun.was.domain.user.entity.User;
import kr.withrun.was.global.common.entity.BaseEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;


@Getter
@Entity
@Table(name = "running_sessions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RunningSession extends BaseEntity {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    @Column(name = "running_session_id")
    Long id;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    @Column(name = "duration_sec")
    private Integer durationSec;

    @Column(name = "distance_m", nullable = false)
    private Integer distanceM = 0;

    @Column(name = "avg_speed_mps")
    private Double avgSpeedMps;

    @Column(name = "avg_pace_sec_per_km")
    private Integer avgPaceSecPerKm;

    @Column(name = "calories_kcal", nullable = false)
    private Integer caloriesKcal = 0;

    @Column(name = "elevation_gain_m", nullable = false)
    private Integer elevationGainM = 0;

    @Column(name = "snapshot_image_url")
    private String snapshotImageUrl;

    @Column(name = "start_latitude", nullable = false)
    private Double startLatitude;

    @Column(name = "start_longitude", nullable = false)
    private Double startLongitude;

    @Column(name = "end_latitude")
    private Double endLatitude;

    @Column(name = "end_longitude")
    private Double endLongitude;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "coordinates", columnDefinition = "jsonb")
    private Coordinates coordinates;

    @Column(name = "is_public", nullable = false)
    private Boolean isPublic = false;

    @Column(name = "is_completed", nullable = false)
    private Boolean isCompleted = false;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_running_sessions_user")
    )
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "course_id",
            foreignKey = @ForeignKey(name = "fk_running_sessions_course")
    )
    private Course course;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode", nullable = false, length = 16)
    private RunningMode mode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "ghost_target_running_session_id",
            foreignKey = @ForeignKey(name = "fk_running_sessions_ghost_target_running_session")
    )
    private RunningSession ghostTargetRunningSession;

}
