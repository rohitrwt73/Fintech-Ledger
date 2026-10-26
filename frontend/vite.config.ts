import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
export default defineConfig({ plugins: [react(), tailwindcss()], server: { proxy: { '/api': 'http://localhost:8080' } }, test: { environment: 'jsdom', setupFiles: './src/testSetup.ts' } })
