import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";
import path from "path";

function requireDevProxyTarget(devProxyTarget: string | undefined): string {

  if (!devProxyTarget) {
    throw new Error("VITE_DEV_PROXY_TARGET must be set for the Vite dev server.");
  }

  return devProxyTarget;
}

// https://vite.dev/config/
export default defineConfig(({ command, mode }) => {
  const env = loadEnv(mode, process.cwd(), "");
  const devProxyTarget = command === "serve"
    ? requireDevProxyTarget(env.VITE_DEV_PROXY_TARGET)
    : undefined;

  return {
    plugins: [
      react({
        babel: {
          plugins: [["babel-plugin-react-compiler"]],
        },
      }),
      tailwindcss(),
    ],
    resolve: {
      alias: {
        "@": path.resolve(__dirname, "./src"),
        "@bridge": path.resolve(__dirname, "../mobile/src/bridge/types.ts"),
      },
    },
    server: devProxyTarget
      ? {
          proxy: {
            "/api": {
              target: devProxyTarget,
              changeOrigin: false,
            },
            "/oauth2": {
              target: devProxyTarget,
              changeOrigin: false,
            },
            "/login/oauth2": {
              target: devProxyTarget,
              changeOrigin: false,
            },
            "/mobile/oauth2": {
              target: devProxyTarget,
              changeOrigin: false,
            },
          },
        }
      : undefined,
  };
});
