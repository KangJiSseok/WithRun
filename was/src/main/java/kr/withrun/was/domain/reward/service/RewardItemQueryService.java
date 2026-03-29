package kr.withrun.was.domain.reward.service;

import kr.withrun.was.domain.file.service.CloudFrontSignedUrlService;
import kr.withrun.was.domain.reward.dto.RewardItemShowcaseListResponse;
import kr.withrun.was.domain.reward.dto.RewardItemShowcaseResponse;
import kr.withrun.was.domain.reward.entity.RewardItem;
import kr.withrun.was.domain.reward.repository.RewardItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RewardItemQueryService {

    private final RewardItemRepository rewardItemRepository;
    private final CloudFrontSignedUrlService cloudFrontSignedUrlService;

    public RewardItemShowcaseListResponse getRewardItemShowcase() {
        return new RewardItemShowcaseListResponse(
                rewardItemRepository.findAllByDeletedAtIsNullAndActiveTrueOrderByCreatedAtDescIdDesc()
                        .stream()
                        .map(this::toShowcaseResponse)
                        .toList()
        );
    }

    private RewardItemShowcaseResponse toShowcaseResponse(RewardItem rewardItem) {
        return new RewardItemShowcaseResponse(
                rewardItem.getId(),
                rewardItem.getTitle(),
                cloudFrontSignedUrlService.generateSignedUrl(rewardItem.getImageUrl())
        );
    }
}
