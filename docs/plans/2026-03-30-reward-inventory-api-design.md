# Reward Inventory API Design

## 목적

이 문서는 사용자 인벤토리 API의 MVP 기준을 정리한다.

현재 스키마에는 rarity/grade 필드가 없으므로,
인벤토리는 별도 소유 테이블을 새로 만들지 않고
`reward_gacha_draw_cards`에서 실제 당첨 카드만 읽어오는 방식으로 구현한다.

## 기준

- 인벤토리에는 `MISS` 카드를 포함하지 않는다.
- `REWARD` 카드만 인벤토리 대상이다.
- 정렬은 최신 획득순(`acquiredAt DESC`)이다.
- 응답 아이템 정보는 기존 reward item 카탈로그의 `title`, `imageUrl`를 사용한다.

## endpoint

`GET /api/reward-gacha/me/inventory?page=0&size=20`

## 응답 필드

- `rewardGachaDrawCardId`
- `rewardGachaDrawId`
- `rewardItemId`
- `title`
- `imageUrl`
- `acquiredAt`

## 구현 메모

- 소유 근거는 `reward_gacha_draw_cards.card_type = REWARD`
- user scope 는 `reward_gacha_draw_cards -> reward_gacha_draws -> user_id`
- 현재 문서의 “등급 높은 순”은 스키마 불일치로 제거한다.
