export class RewardGachaContractUnavailableError extends Error {
  constructor(action: 'draw' | 'inventory') {
    super(
      action === 'draw'
        ? '가챠 뽑기 API 계약이 아직 준비되지 않았어요.'
        : '인벤토리 조회 API 계약이 아직 준비되지 않았어요.'
    );
    this.name = 'RewardGachaContractUnavailableError';
  }
}

export async function drawRewardGacha(): Promise<never> {
  throw new RewardGachaContractUnavailableError('draw');
}

export async function getRewardInventory(): Promise<never> {
  throw new RewardGachaContractUnavailableError('inventory');
}
