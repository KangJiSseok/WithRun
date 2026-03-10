import axiosClient from '@/shared/libs/axiosClient';

type LatLng = { lat: number; lng: number };

export type SaveRunPayload = {
  distance: number;
  duration: number;
  route: LatLng[];
};

export type SaveRunResponse = {
  runId: number;
};

export const saveRun = async (payload: SaveRunPayload): Promise<SaveRunResponse> => {
  const { data } = await axiosClient.post<SaveRunResponse>('/runs/free', payload);
  return data;
};
