package kr.withrun.was.domain.course.entity;

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
import kr.withrun.was.domain.user.entity.User;
import kr.withrun.was.global.common.entity.BaseEntity;
import kr.withrun.was.global.common.type.Difficulty;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "course_reviews")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseReview extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "course_review_id")
    private Long id;

    @Column(name = "rating")
    private Integer rating;

    @Enumerated(EnumType.STRING)
    @Column(name = "submitted_difficulty")
    private Difficulty difficulty;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "course_id",
            foreignKey = @ForeignKey(name = "fk_course_reviews_course")
    )
    private Course course;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            foreignKey = @ForeignKey(name = "fk_course_reviews_user")
    )
    private User user;

}
