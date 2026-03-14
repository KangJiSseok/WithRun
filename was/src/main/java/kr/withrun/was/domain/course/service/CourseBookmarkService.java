package kr.withrun.was.domain.course.service;

import kr.withrun.was.domain.course.dto.CourseBookmarkAddResponse;
import kr.withrun.was.domain.course.dto.CourseBookmarkRemoveResponse;
import kr.withrun.was.domain.course.entity.Course;
import kr.withrun.was.domain.course.entity.CourseBookmark;
import kr.withrun.was.domain.course.repository.CourseBookmarkRepository;
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
public class CourseBookmarkService {

    private final CourseRepository courseRepository;
    private final CourseBookmarkRepository courseBookmarkRepository;
    private final UserRepository userRepository;

    @Transactional
    public CourseBookmarkAddResponse addBookmark(Long courseId, Long userId) {
        Course course = getCourse(courseId);
        User user = getUser(userId);

        return courseBookmarkRepository.findBookmark(courseId, userId)
                .map(bookmark -> new CourseBookmarkAddResponse(courseId, true, bookmark.getCreatedAt()))
                .orElseGet(() -> saveBookmark(course, user));
    }

    @Transactional
    public CourseBookmarkRemoveResponse removeBookmark(Long courseId, Long userId) {
        getCourse(courseId);
        getUser(userId);

        return courseBookmarkRepository.findBookmark(courseId, userId)
                .map(bookmark -> {
                    courseBookmarkRepository.deleteBookmark(courseId, userId);
                    return new CourseBookmarkRemoveResponse(courseId, false);
                })
                .orElseGet(() -> new CourseBookmarkRemoveResponse(courseId, false));
    }

    private CourseBookmarkAddResponse saveBookmark(Course course, User user) {
        CourseBookmark bookmark = courseBookmarkRepository.save(CourseBookmark.create(course, user));
        return new CourseBookmarkAddResponse(course.getId(), true, bookmark.getCreatedAt());
    }

    private Course getCourse(Long courseId) {
        return courseRepository.findNotDeletedCourse(courseId)
                .orElseThrow(() -> new CustomException(ResponseCode.COURSE_NOT_FOUND));
    }

    private User getUser(Long userId) {
        return userRepository.findNotDeletedUser(userId)
                .orElseThrow(() -> new CustomException(ResponseCode.USER_NOT_FOUND));
    }
}
