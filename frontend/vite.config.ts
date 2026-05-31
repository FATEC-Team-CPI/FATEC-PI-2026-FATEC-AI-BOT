import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueJsx from '@vitejs/plugin-vue-jsx'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    vueJsx(),
    // vueDevTools(),
  ],
  server: {
    host: '0.0.0.0',
    proxy: {
      '/api': {
        target: 'http://backend:8082',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
      '/ws': {
        target: 'http://backend:8082',
        ws: true,
        changeOrigin: true,
      },
      // Proxy uploads to the AI service to avoid CORS during development.
      // Use VITE_AI_PROXY_TARGET to override (e.g. http://ai:8002 when running inside compose).
      '/upload': {
        // Use the same env var as the frontend (.env) so you can configure the target in one place.
        target: process.env.VITE_AI_UPLOAD_URL || 'http://localhost:8002',
        changeOrigin: true,
        secure: false,
      },
    },
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
})
