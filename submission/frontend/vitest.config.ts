import react from '@vitejs/plugin-react'
import { defineConfig } from 'vitest/config'

export default defineConfig({
  plugins: [react()],
  test: {
    environment: 'jsdom',
    include: ['src/**/*.{test,spec}.{ts,tsx}'],
    maxWorkers: 1,
    pool: 'threads',
    setupFiles: ['./src/test/setup.ts'],
    coverage: {
      provider: 'v8',
      reporter: ['text', 'lcov'],
      include: ['src/features/dynamic-form/**/*.{ts,tsx}'],
      exclude: [
        'src/features/dynamic-form/**/*.d.ts',
        'src/features/dynamic-form/**/*.module.css',
        'src/features/dynamic-form/components/designer/index.ts',
      ],
    },
  },
})
