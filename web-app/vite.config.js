import { defineConfig } from 'vite';

export default defineConfig({
  root: 'src',
  publicDir: '../public',
  base: '/',
  build: {
    outDir: '../dist',
    assetsDir: 'assets',
    emptyOutDir: true
  },
  server: {
    port: 5173,
    // Bind to all interfaces so the dev-proxy container (compose-devservices.yml)
    // can reach Vite via host.docker.internal. Default 127.0.0.1 causes 502 from nginx.
    host: '0.0.0.0',
    // HMR client connects through the dev-proxy on port 80, not directly to 5173.
    // Direct access to http://localhost:5173 still works for static content, but
    // HMR live-reload only works via the proxy.
    hmr: {
      host: 'localhost',
      clientPort: 80,
      protocol: 'ws'
    },
    proxy: {
      '/q': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/.well-known': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
});
