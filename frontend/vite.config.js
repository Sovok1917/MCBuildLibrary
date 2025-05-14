// File: frontend/vite.config.js
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000, // Your frontend dev server port
    proxy: {
      // Proxy /login requests (no rewrite needed)
      '/login': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      },
      // Proxy /logout requests (no rewrite needed)
      '/logout': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      },
      // Proxy /api requests WITHOUT rewrite
      '/api': {
        target: 'http://localhost:8080', // Your Spring Boot backend URL
        changeOrigin: true,
        secure: false,
        // REMOVED: rewrite: (path) => path.replace(/^\/api/, ''),
      },
    },
  },
});