import { defineConfig, loadEnv } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, '.', 'VITE_');
  const apiTarget = env.VITE_API_PROXY_TARGET || 'http://localhost:8080';

  return {
    plugins: [react()],
    server: {
      host: '0.0.0.0',
      port: 5173,
      strictPort: true,
      proxy: {
        '/v1': { target: apiTarget },
        '/s': { target: apiTarget },
        '/swagger-ui.html': { target: apiTarget },
        '/swagger-ui': { target: apiTarget },
        '/v3': { target: apiTarget },
      },
    },
  };
});
