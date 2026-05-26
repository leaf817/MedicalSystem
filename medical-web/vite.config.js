import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    port: 5173,
    proxy: {
      // Session 场景：代理需转发 Cookie，前端 request 已配置 withCredentials
      '/api': {
        target: 'http://localhost:8081',
        changeOrigin: true
      },
      '/doc.html': {
        target: 'http://localhost:8081',
        changeOrigin: true
      },
      '/v3': {
        target: 'http://localhost:8081',
        changeOrigin: true
      }
    }
  }
})
