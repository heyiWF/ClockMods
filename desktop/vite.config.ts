import { defineConfig } from 'vitest/config';
// Tauri embeds the production assets; service workers would keep stale app code.
export default defineConfig({
  base: './',
  clearScreen: false,
  server: { host: '127.0.0.1', port: 1420, strictPort: true },
  build: {
    target: 'es2022', assetsInlineLimit: 0,
    rollupOptions: { output: { manualChunks: { 'locale-data': ['./src/core/city-names.json', './src/core/traditional-map.json'] } } },
  },
  test: { environment: 'node', include: ['tests/**/*.test.ts'] },
});
