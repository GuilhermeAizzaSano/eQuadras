import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 3000,
    proxy: {
      '/usuarios': 'http://localhost:8080',
      '/quadras': 'http://localhost:8080',
      '/agendamentos': 'http://localhost:8080',
      '/notificacoes': 'http://localhost:8080'
    }
  }
});
