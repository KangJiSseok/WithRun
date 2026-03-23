package kr.withrun.was.domain.user.service;

import kr.withrun.was.domain.user.dto.UpdateUserRunningPreferenceRequest;
import kr.withrun.was.domain.user.dto.UserRunningPreferenceResponse;
import kr.withrun.was.domain.user.entity.User;
import kr.withrun.was.domain.user.entity.UserRunningPreference;
import kr.withrun.was.domain.user.repository.UserRepository;
import kr.withrun.was.domain.user.repository.UserRunningPreferenceRepository;
import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserRunningPreferenceService {

    private final UserRepository userRepository;
    private final UserRunningPreferenceRepository userRunningPreferenceRepository;

    public UserRunningPreferenceResponse getRunningPreference(Long userId) {
        User user = findUser(userId);
        return UserRunningPreferenceResponse.from(user.getUserRunningPreference());
    }

    @Transactional
    public void updateRunningPreference(Long userId, UpdateUserRunningPreferenceRequest request) {
        User user = findUser(userId);
        UserRunningPreference runningPreference = user.getUserRunningPreference();

        if (runningPreference == null) {
            runningPreference = UserRunningPreference.create(
                    user,
                    request.preferredDifficulty(),
                    request.preferredDistanceKm()
            );
            user.assignRunningPreference(runningPreference);
        } else {
            runningPreference.updatePreferredSettings(
                    request.preferredDifficulty(),
                    request.preferredDistanceKm()
            );
        }

        // 현재 제품은 각 카테고리당 단일 기본 설정만 받기 때문에,
        // 내부 엔티티의 컬렉션 구조에는 "선택된 값 1개"만 유지합니다.
        runningPreference.replacePurpose(request.purpose());
        runningPreference.replaceTimeSlot(request.timeSlot());
        runningPreference.replaceCourseType(request.courseType());

        userRunningPreferenceRepository.save(runningPreference);
    }

    private User findUser(Long userId) {
        return userRepository.findNotDeletedUser(userId)
                .orElseThrow(() -> new CustomException(ResponseCode.USER_NOT_FOUND));
    }
}
