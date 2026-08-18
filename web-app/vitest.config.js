import { defineConfig } from 'vitest/config';

export default defineConfig({
    test: {
        environment: 'jsdom',
        include: ['src/**/*.unit.test.js'],
        globals: false
    }
});
