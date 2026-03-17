import { create } from 'zustand';
import type { GpsLocation, WatchHealthData } from '@bridge';

interface BridgeDataState {
  gpsLocation: GpsLocation | null;
  watchHealthData: WatchHealthData | null;
  setGpsLocation: (data: GpsLocation) => void;
  setWatchHealthData: (data: WatchHealthData) => void;
}

export const useBridgeDataStore = create<BridgeDataState>((set) => ({
  gpsLocation: null,
  watchHealthData: null,
  setGpsLocation: (data) => set({ gpsLocation: data }),
  setWatchHealthData: (data) => set({ watchHealthData: data }),
}));