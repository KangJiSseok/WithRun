package kr.withrun.was.domain.course.service;

import kr.withrun.was.domain.course.dto.CourseLikeStatusResponse;
import kr.withrun.was.domain.course.entity.Course;
import kr.withrun.was.domain.course.entity.CourseLike;
import kr.withrun.was.domain.course.repository.CourseLikeRepository;
import kr.withrun.was.domain.course.repository.CourseRepository;
import kr.withrun.was.domain.user.entity.User;
import kr.withrun.was.domain.user.repository.UserRepository;
import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CourseLikeService {

    private final CourseRepository courseRepository;
    private final CourseLikeRepository courseLikeRepository;
    private final UserRepository userRepository;

    @Transactional
    public CourseLikeStatusResponse likeCourse(Long courseId, Long userId) {
        Course course = getCourse(courseId);
        User user = getUser(userId);

        courseLikeRepository.findByCourseIdAndUserId(courseId, userId)
                .ifPresentOrElse(CourseLike::activate, () -> courseLikeRepository.save(CourseLike.create(course, user, true)));

        long likeCount = courseLikeRepository.countActiveLikedByCourseId(courseId);
        course.promoteToOfficialIfEligible(likeCount);
        return new CourseLikeStatusResponse(courseId, true, likeCount, course.getStatus());
    }

    @Transactional
    public CourseLikeStatusResponse unlikeCourse(Long courseId, Long userId) {
        Course course = getCourse(courseId);
        getUser(userId);

        courseLikeRepository.findByCourseIdAndUserId(courseId, userId)
                .ifPresent(CourseLike::deactivate);

        long likeCount = courseLikeRepository.countActiveLikedByCourseId(courseId);
        return new CourseLikeStatusResponse(courseId, false, likeCount, course.getStatus());
    }

    private Course getCourse(Long courseId) {
        return courseRepository.findActiveById(courseId)
                .orElseThrow(() -> new CustomException(ResponseCode.COURSE_NOT_FOUND));
    }

    private User getUser(Long userId) {
        return userRepository.findActiveUserById(userId)
                .orElseThrow(() -> new CustomException(ResponseCode.USER_NOT_FOUND));
    }
}
