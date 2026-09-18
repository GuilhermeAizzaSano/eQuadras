import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    host: '0.0.0.0',
    port: 3000,
    proxy: {
      '/usuarios': 'http://localhost:8080',
      '/quadras': 'http://localhost:8080',
      '/agendamentos': 'http://localhost:8080',
      '/notificacoes': 'http://localhost:8080',
      '/pagamentos': 'http://localhost:8080',
      '/uploads': 'http://localhost:8080',
      '/api': 'http://localhost:8080',
    }
  },
  build: {
    sourcemap: false,
    rollupOptions: {
      output: {
        manualChunks: {
          vendor: ['react', 'react-dom'],
          icons: ['lucide-react'],
        },
      },
    },
  },
  esbuild: {
    drop: ['debugger'],
  },
  // @ts-ignore
  test: {
    globals: true,
    environment: 'jsdom',
  },
});
