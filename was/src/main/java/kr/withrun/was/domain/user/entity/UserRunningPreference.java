package kr.withrun.was.domain.user.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import kr.withrun.was.global.common.entity.BaseEntity;
import kr.withrun.was.global.common.type.Difficulty;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@Table(name = "user_running_preferences")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserRunningPreference extends BaseEntity {

    @Id
    @Column(name = "user_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_difficulty", nullable = false, length = 16)
    private Difficulty preferredDifficulty;

    @Column(name = "preferred_distance_km", nullable = false)
    private Double preferredDistanceKm;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(name = "fk_user_running_preferences_user"))
    private User user;

    @OneToMany(mappedBy = "userRunningPreference", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<UserPreferencePurpose> purposes = new ArrayList<>();

    @OneToMany(mappedBy = "userRunningPreference", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<UserPreferenceCourseType> courseTypes = new ArrayList<>();

    @OneToMany(mappedBy = "userRunningPreference", cascade = CascadeType.ALL, orphanRemoval = true)
    private final List<UserPreferenceTimeSlot> timeSlots = new ArrayList<>();

    public static UserRunningPreference create(
            User user,
            Difficulty preferredDifficulty,
            Double preferredDistanceKm
    ) {
        UserRunningPreference preference = new UserRunningPreference();
        preference.user = user;
        preference.preferredDifficulty = preferredDifficulty;
        preference.preferredDistanceKm = preferredDistanceKm;
        return preference;
    }

    public void updatePreferredSettings(
            Difficulty preferredDifficulty,
            Double preferredDistanceKm
    ) {
        this.preferredDifficulty = preferredDifficulty;
        this.preferredDistanceKm = preferredDistanceKm;
    }

    public void addPurpose(UserPreferencePurpose purpose) {
        purposes.add(purpose);
        purpose.setUserRunningPreference(this);
    }

    public void addCourseType(UserPreferenceCourseType courseType) {
        courseTypes.add(courseType);
        courseType.setUserRunningPreference(this);
    }

    public void addTimeSlot(UserPreferenceTimeSlot timeSlot) {
        timeSlots.add(timeSlot);
        timeSlot.setUserRunningPreference(this);
    }

    public void replacePurpose(kr.withrun.was.domain.user.type.Purpose purpose) {
        purposes.clear();
        addPurpose(UserPreferencePurpose.create(purpose));
    }

    public void replaceCourseType(kr.withrun.was.domain.course.type.CourseType courseType) {
        courseTypes.clear();
        addCourseType(UserPreferenceCourseType.create(courseType));
    }

    public void replaceTimeSlot(kr.withrun.was.global.common.type.TimeSlot timeSlot) {
        timeSlots.clear();
        addTimeSlot(UserPreferenceTimeSlot.create(timeSlot));
    }
}
