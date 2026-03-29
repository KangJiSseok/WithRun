import manifestJson from '../../assets/voices/manifest.json';
import type { VoiceCueKey } from '../bridge/types';

export type VoiceManifestItem = {
  index: number;
  key: VoiceCueKey;
  text: string;
  file: string;
  sample_rate: number;
};

export const voiceManifest = manifestJson as VoiceManifestItem[];

export const voiceKeySet = new Set<VoiceCueKey>(
  voiceManifest.map((item) => item.key)
);
