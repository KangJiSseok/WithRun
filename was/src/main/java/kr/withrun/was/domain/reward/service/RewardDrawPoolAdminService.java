package kr.withrun.was.domain.reward.service;

import kr.withrun.was.domain.file.service.CloudFrontSignedUrlService;
import kr.withrun.was.domain.reward.dto.CreateRewardDrawPoolRequest;
import kr.withrun.was.domain.reward.dto.RewardDrawPoolItemRequest;
import kr.withrun.was.domain.reward.dto.RewardDrawPoolItemResponse;
import kr.withrun.was.domain.reward.dto.RewardDrawPoolListResponse;
import kr.withrun.was.domain.reward.dto.RewardDrawPoolResponse;
import kr.withrun.was.domain.reward.dto.UpdateRewardDrawPoolRequest;
import kr.withrun.was.domain.reward.entity.RewardDrawPool;
import kr.withrun.was.domain.reward.entity.RewardDrawPoolItem;
import kr.withrun.was.domain.reward.entity.RewardItem;
import kr.withrun.was.domain.reward.repository.RewardDrawPoolItemRepository;
import kr.withrun.was.domain.reward.repository.RewardDrawPoolRepository;
import kr.withrun.was.domain.reward.repository.RewardItemRepository;
import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RewardDrawPoolAdminService {

    private static final int MAX_NAME_LENGTH = 100;

    private final RewardDrawPoolRepository rewardDrawPoolRepository;
    private final RewardDrawPoolItemRepository rewardDrawPoolItemRepository;
    private final RewardItemRepository rewardItemRepository;
    private final CloudFrontSignedUrlService cloudFrontSignedUrlService;

    @Transactional
    public RewardDrawPoolResponse createRewardDrawPool(CreateRewardDrawPoolRequest request) {
        validateRequest(request == null ? null : request.name(), request == null ? null : request.items());

        RewardDrawPool rewardDrawPool = rewardDrawPoolRepository.save(RewardDrawPool.create(normalizeName(request.name())));
        List<RewardDrawPoolItem> rewardDrawPoolItems = savePoolItems(rewardDrawPool, request.items());

        return toResponse(rewardDrawPool, rewardDrawPoolItems);
    }

    @Transactional(readOnly = true)
    public RewardDrawPoolListResponse getRewardDrawPools(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<RewardDrawPool> rewardDrawPools = rewardDrawPoolRepository.findAllByDeletedAtIsNull(pageable);

        return new RewardDrawPoolListResponse(
                rewardDrawPools.getContent().stream()
                        .map(rewardDrawPool -> toResponse(rewardDrawPool, loadPoolItems(rewardDrawPool.getId())))
                        .toList(),
                rewardDrawPools.getNumber(),
                rewardDrawPools.getSize(),
                rewardDrawPools.getTotalElements(),
                rewardDrawPools.getTotalPages(),
                rewardDrawPools.hasNext()
        );
    }

    @Transactional(readOnly = true)
    public RewardDrawPoolResponse getRewardDrawPool(Long rewardDrawPoolId) {
        RewardDrawPool rewardDrawPool = findRewardDrawPool(rewardDrawPoolId);
        return toResponse(rewardDrawPool, loadPoolItems(rewardDrawPoolId));
    }

    @Transactional
    public RewardDrawPoolResponse updateRewardDrawPool(Long rewardDrawPoolId, UpdateRewardDrawPoolRequest request) {
        validateRequest(request == null ? null : request.name(), request == null ? null : request.items());

        RewardDrawPool rewardDrawPool = findRewardDrawPool(rewardDrawPoolId);
        rewardDrawPool.updateName(normalizeName(request.name()));

        List<RewardDrawPoolItem> existingItems = loadPoolItems(rewardDrawPoolId);
        existingItems.forEach(RewardDrawPoolItem::delete);

        List<RewardDrawPoolItem> rewardDrawPoolItems = savePoolItems(rewardDrawPool, request.items());
        return toResponse(rewardDrawPool, rewardDrawPoolItems);
    }

    @Transactional
    public RewardDrawPoolResponse activateRewardDrawPool(Long rewardDrawPoolId) {
        RewardDrawPool rewardDrawPool = findRewardDrawPool(rewardDrawPoolId);

        rewardDrawPoolRepository.deactivateAllActivePools();
        rewardDrawPool.activate();

        return toResponse(rewardDrawPool, loadPoolItems(rewardDrawPoolId));
    }

    @Transactional(readOnly = true)
    public RewardDrawPoolResponse getActiveRewardDrawPool() {
        RewardDrawPool rewardDrawPool = rewardDrawPoolRepository.findByActiveTrueAndDeletedAtIsNull()
                .orElseThrow(() -> new CustomException(ResponseCode.REWARD_DRAW_POOL_NOT_FOUND));

        return toResponse(rewardDrawPool, loadPoolItems(rewardDrawPool.getId()));
    }

    private RewardDrawPool findRewardDrawPool(Long rewardDrawPoolId) {
        return rewardDrawPoolRepository.findByIdAndDeletedAtIsNull(rewardDrawPoolId)
                .orElseThrow(() -> new CustomException(ResponseCode.REWARD_DRAW_POOL_NOT_FOUND));
    }

    private List<RewardDrawPoolItem> savePoolItems(RewardDrawPool rewardDrawPool, List<RewardDrawPoolItemRequest> requests) {
        List<RewardDrawPoolItem> rewardDrawPoolItems = requests.stream()
                .map(request -> RewardDrawPoolItem.create(
                        rewardDrawPool,
                        findRewardItem(request.rewardItemId()),
                        request.weight()
                ))
                .toList();

        return rewardDrawPoolItemRepository.saveAll(rewardDrawPoolItems);
    }

    private RewardItem findRewardItem(Long rewardItemId) {
        return rewardItemRepository.findByIdAndDeletedAtIsNull(rewardItemId)
                .orElseThrow(() -> new CustomException(ResponseCode.REWARD_ITEM_NOT_FOUND));
    }

    private List<RewardDrawPoolItem> loadPoolItems(Long rewardDrawPoolId) {
        return rewardDrawPoolItemRepository.findAllByRewardDrawPoolIdAndDeletedAtIsNullOrderByIdAsc(rewardDrawPoolId);
    }

    private RewardDrawPoolResponse toResponse(RewardDrawPool rewardDrawPool, List<RewardDrawPoolItem> rewardDrawPoolItems) {
        return new RewardDrawPoolResponse(
                rewardDrawPool.getId(),
                rewardDrawPool.getName(),
                rewardDrawPool.isActive(),
                rewardDrawPoolItems.stream().map(this::toItemResponse).toList(),
                rewardDrawPool.getCreatedAt(),
                rewardDrawPool.getUpdatedAt()
        );
    }

    private RewardDrawPoolItemResponse toItemResponse(RewardDrawPoolItem rewardDrawPoolItem) {
        RewardItem rewardItem = rewardDrawPoolItem.getRewardItem();
        return new RewardDrawPoolItemResponse(
                rewardDrawPoolItem.getId(),
                rewardItem.getId(),
                rewardItem.getTitle(),
                cloudFrontSignedUrlService.generateSignedUrl(rewardItem.getImageUrl()),
                rewardDrawPoolItem.getWeight()
        );
    }

    private void validateRequest(String name, List<RewardDrawPoolItemRequest> items) {
        if (!StringUtils.hasText(name) || normalizeName(name).length() > MAX_NAME_LENGTH || items == null || items.isEmpty()) {
            throw new CustomException(ResponseCode.INVALID_INPUT_VALUE);
        }

        Set<Long> rewardItemIds = new HashSet<>();
        for (RewardDrawPoolItemRequest item : items) {
            if (item == null || item.rewardItemId() == null || item.weight() == null || item.weight() <= 0 || !rewardItemIds.add(item.rewardItemId())) {
                throw new CustomException(ResponseCode.INVALID_INPUT_VALUE);
            }
        }
    }

    private String normalizeName(String name) {
        return name == null ? null : name.trim();
    }
}
