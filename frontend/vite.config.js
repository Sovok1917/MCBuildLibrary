// File: frontend/vite.config.js
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000, // Your frontend dev server port
    proxy: {
      // Proxy /api requests (this will catch /api/perform_login, /api/perform_logout, etc.)
      '/api': {
        target: 'http://localhost:8080', // Your Spring Boot backend URL
        changeOrigin: true,
        secure: false,
      },
      // REMOVE specific proxies for /login and /logout if they are now under /api
      // or if GET requests to them should be handled by Vite serving index.html
    },
  },
});