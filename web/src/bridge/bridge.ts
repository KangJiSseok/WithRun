import type { AppBridge } from "@bridge";
import { linkBridge } from "@webview-bridge/web";

const bridge = linkBridge<AppBridge>()

export {bridge}