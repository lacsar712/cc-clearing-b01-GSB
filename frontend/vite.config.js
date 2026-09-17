import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 3171,
    proxy: {
      '/api': {
        target: 'http://localhost:8171',
        changeOrigin: true
      }
    }
  }
})
