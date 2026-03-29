import { Audio } from 'expo-av';

import { voiceAssetMap } from './voiceAssetMap';
import type { VoiceCueKey } from '../bridge/types';

const SAME_KEY_COOLDOWN_MS = 1200;

class VoicePlayer {
  private enabled = true;
  private initialized = false;
  private currentSound: Audio.Sound | null = null;
  private lastPlayedAt = new Map<VoiceCueKey, number>();

  private async ensureInitialized() {
    if (this.initialized) return;

    await Audio.setAudioModeAsync({
      allowsRecordingIOS: false,
      playsInSilentModeIOS: true,
      shouldDuckAndroid: true,
      playThroughEarpieceAndroid: false,
      staysActiveInBackground: false,
    });

    this.initialized = true;
  }

  async play(key: VoiceCueKey) {
    if (!this.enabled) return;

    const now = Date.now();
    const last = this.lastPlayedAt.get(key) ?? 0;
    if (now - last < SAME_KEY_COOLDOWN_MS) return;

    await this.ensureInitialized();

    this.lastPlayedAt.set(key, now);

    await this.stop();

    const source = voiceAssetMap[key];
    const { sound } = await Audio.Sound.createAsync(source, {
      shouldPlay: true,
      volume: 1,
      progressUpdateIntervalMillis: 250,
    });

    this.currentSound = sound;

    sound.setOnPlaybackStatusUpdate((status) => {
      if (!status.isLoaded) return;
      if (!status.didJustFinish) return;

      void sound.unloadAsync().catch(() => {});
      if (this.currentSound === sound) {
        this.currentSound = null;
      }
    });
  }

  async stop() {
    if (!this.currentSound) return;

    const sound = this.currentSound;
    this.currentSound = null;

    try {
      await sound.stopAsync();
    } catch {
      // noop
    }

    try {
      await sound.unloadAsync();
    } catch {
      // noop
    }
  }

  setEnabled(enabled: boolean) {
    this.enabled = enabled;
    if (!enabled) {
      void this.stop();
    }
  }
}

export const voicePlayer = new VoicePlayer();
