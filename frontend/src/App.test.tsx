import { afterEach, describe, expect, it, vi } from 'vitest'
import { cleanup, render, screen } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'
import App from './App'

vi.mock('./api', () => ({
  api: { get: vi.fn(async (path: string) => ({ data: path === '/auth/me' ? { id: '1', email: 'user@example.com', fullName: 'Demo User', role: 'USER' } : [] })), post: vi.fn() }
}))

afterEach(() => { cleanup(); vi.clearAllMocks() })

describe('dashboard', () => {
  it('shows the empty account state when the API returns no accounts', async () => {
    const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
    render(<QueryClientProvider client={client}><MemoryRouter><App /></MemoryRouter></QueryClientProvider>)
    expect(await screen.findByText('No accounts yet')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /new transfer/i })).toBeInTheDocument()
  })
})
