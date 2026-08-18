import { defineConfig } from 'vitest/config';

// Relative base so the built site works both at a domain root and under a
// GitHub Pages project subpath.
export default defineConfig({
  base: './',
  build: {
    // The actual entry is a classic ES5 script; Vite only copies the legacy
    // assets from public/. Keep the build itself free of generated modern
    // service-worker/module bootstrap code so IE never parses it.
    target: 'es5',
    assetsInlineLimit: 0,
  },
  test: {
    environment: 'node',
    include: ['tests/**/*.test.ts'],
    // The legacy branch deliberately has no calendar page. Calendar model and
    // lunar tests still run; only the removed page integration is out of scope.
    exclude: ['tests/calendar-page.test.ts'],
  },
});
