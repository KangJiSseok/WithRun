import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import {
  drawRewardGacha,
  getRewardInventory,
  getRewardShowcase,
} from '@/apis/rewardGacha';

export function useRewardShowcase() {
  return useQuery({
    queryKey: ['rewardGacha', 'showcase'],
    queryFn: async () => {
      const response = await getRewardShowcase();
      return response.data;
    },
  });
}

export function useRewardInventory(enabled: boolean) {
  return useQuery({
    queryKey: ['rewardGacha', 'inventory', { page: 0, size: 20 }],
    queryFn: async () => {
      const response = await getRewardInventory(0, 20);
      return response.data;
    },
    enabled,
  });
}

export function useDrawRewardGacha() {
  const queryClient = useQueryClient();

  async function refreshRewardGachaState() {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ['rewardPoints', 'balance'] }),
      queryClient.invalidateQueries({ queryKey: ['rewardPoints', 'histories'] }),
      queryClient.invalidateQueries({ queryKey: ['rewardGacha', 'inventory'] }),
    ]);
  }

  const mutation = useMutation({
    mutationFn: async () => {
      const response = await drawRewardGacha();
      return response.data;
    },
  });

  return {
    ...mutation,
    refreshRewardGachaState,
  };
}
