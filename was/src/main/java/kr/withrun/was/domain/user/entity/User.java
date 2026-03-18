package kr.withrun.was.domain.user.entity;

// jakarta, 데이터베이스 설계 도구

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import kr.withrun.was.domain.user.type.Gender;
import kr.withrun.was.global.common.entity.BaseEntity;
import org.hibernate.annotations.SQLDelete;

// lombok을 통해 자바에서 데이터를 담는 객체(Entity나 DTO)를 쉽게 만들 수 있음.
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Entity
@Table(name = "users")
@SQLDelete(sql = "UPDATE users SET deleted_at = NOW() WHERE user_id = ?")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(name = "nickname", length = 40)
    private String nickname;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 16)
    private Gender gender;

    @Column(name = "height_cm")
    private Double height;

    @Column(name = "weight_kg")
    private Double weight;

    @Column(name = "profile_completed", nullable = false)
    private boolean profileCompleted;

    @OneToOne(
            mappedBy = "user",
            cascade = CascadeType.ALL,
            orphanRemoval = true)
    private UserRunningPreference userRunningPreference;

    public static User createPendingSocialUser() {
        User user = new User();
        user.profileCompleted = false;
        return user;
    }

    // 온보딩이 끝났을 대 비어있던 사용자 프로필을 채우는 함수
    public void completeProfile(
        String nickname,
        LocalDate birthDate,
        Gender gender,
        Double height,
        Double weight
    ) {
        this.nickname = nickname;
        this.birthDate = birthDate;
        this.gender = gender;
        this.height = height;
        this.weight = weight;
        this.profileCompleted = true;
    }

}