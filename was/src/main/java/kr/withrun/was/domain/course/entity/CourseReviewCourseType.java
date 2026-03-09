package kr.withrun.was.domain.course.entity;

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
import kr.withrun.was.domain.course.type.CourseType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "course_review_course_types")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseReviewCourseType extends Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "course_review_course_type_id")
    private Long id;

    @Column(name = "course_type")
    private CourseType courseType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "course_review_id",
            foreignKey = @ForeignKey(name = "fk_course_reviews_course_types_review")
    )
    private CourseReview courseReview;

}
