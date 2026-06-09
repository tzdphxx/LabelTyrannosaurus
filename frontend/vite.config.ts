import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  build: {
    rolldownOptions: {
      output: {
        codeSplitting: {
          groups: [
            {
              name: 'vendor-react',
              test: /node_modules[\\/](react|react-dom|react-router)[\\/]/,
            },
            {
              name: 'vendor-dnd-kit',
              test: /node_modules[\\/]@dnd-kit[\\/]/,
            },
            {
              name: 'vendor-xlsx',
              test: /node_modules[\\/]xlsx[\\/]/,
            },
          ],
        },
      },
    },
  },
})
