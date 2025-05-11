// File: frontend/vite.config.js
import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000, // Your frontend dev server port
    proxy: {
      // Proxy /login requests to your Spring Boot backend
      // Spring Security's default form login processing URL is /login
      '/login': {
        target: 'http://localhost:8080', // Your Spring Boot backend URL
        changeOrigin: true, // Recommended for virtual hosted sites
        secure: false,      // If your backend is HTTP
        // NO rewrite for /login, Spring Security expects the /login path
      },
      // Proxy /logout requests to your Spring Boot backend
      // Spring Security's default logout URL is /logout
      '/logout': {
        target: 'http://localhost:8080', // Your Spring Boot backend URL
        changeOrigin: true,
        secure: false,
        // NO rewrite for /logout
      },
      // Proxy /api requests to Spring Boot backend (existing rule)
      '/api': {
        target: 'http://localhost:8080', // Your Spring Boot backend URL
        changeOrigin: true,
        // The rewrite rule below removes /api from the path before forwarding.
        // This means if your frontend calls /api/builds,
        // your Spring Boot backend should have its controller mapped to /builds.
        // If your Spring Boot controllers are mapped to /api/builds, then remove this rewrite.
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
    },
  },
});