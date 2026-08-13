import { defineConfig } from 'vitest/config';
import { VitePWA } from 'vite-plugin-pwa';

// Relative base so the built site works both at a domain root and under a
// GitHub Pages project subpath.
export default defineConfig({
  base: './',
  build: {
    target: 'es2022',
    // Fonts and the icon/city data sets must stay as real files so the service
    // worker can precache them individually instead of inflating the JS bundle.
    assetsInlineLimit: 0,
  },
  plugins: [
    VitePWA({
      registerType: 'prompt',
      includeAssets: ['icons/*.png', 'fonts/**/*', 'data/**/*'],
      manifest: {
        name: 'ClockMods Pro',
        short_name: 'ClockMods',
        description: '高度可定制的全屏时钟：时钟、月历、番茄钟、闹钟、倒计时、秒表',
        lang: 'zh-Hans',
        start_url: './',
        scope: './',
        display: 'standalone',
        orientation: 'any',
        background_color: '#000000',
        theme_color: '#000000',
        icons: [
          { src: 'icons/icon-192.png', sizes: '192x192', type: 'image/png' },
          { src: 'icons/icon-512.png', sizes: '512x512', type: 'image/png' },
          {
            src: 'icons/icon-maskable-512.png',
            sizes: '512x512',
            type: 'image/png',
            purpose: 'maskable',
          },
        ],
      },
      workbox: {
        globPatterns: ['**/*.{js,css,html,json,png,svg,ttf,otf,woff2}'],
        // Noto Sans regular/bold are ~615 KB each; keep the ceiling above them.
        maximumFileSizeToCacheInBytes: 4 * 1024 * 1024,
        navigateFallback: 'index.html',
        runtimeCaching: [
          {
            // Weather stays fresh when online and falls back to the last
            // response offline, mirroring the Android cache-then-refresh flow.
            urlPattern: /^https?:\/\/.*\/(v7|geo|airquality|weatheralert)\//,
            handler: 'NetworkFirst',
            options: {
              cacheName: 'qweather',
              networkTimeoutSeconds: 15,
              expiration: { maxEntries: 64, maxAgeSeconds: 24 * 60 * 60 },
              cacheableResponse: { statuses: [0, 200] },
            },
          },
        ],
      },
    }),
  ],
  test: {
    environment: 'node',
    include: ['tests/**/*.test.ts'],
  },
});
