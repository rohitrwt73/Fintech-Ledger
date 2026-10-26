import axios from 'axios'
export const api = axios.create({
  baseURL: `${import.meta.env.VITE_API_BASE_URL}/api/v1`,
  withCredentials: true,
  headers: { "Content-Type": "application/json" },
});
let csrfToken: string | undefined
export async function refreshCsrfToken() { await api.get('/auth/csrf'); const cookie = document.cookie.split('; ').find(row => row.startsWith('XSRF-TOKEN='))?.split('=').slice(1).join('='); csrfToken = cookie ? decodeURIComponent(cookie) : undefined }
api.interceptors.request.use(async (config) => { if (config.method?.toLowerCase() !== 'get' && !csrfToken) { await axios.get('/api/v1/auth/csrf', { withCredentials: true }); const issued = document.cookie.split('; ').find(row => row.startsWith('XSRF-TOKEN='))?.split('=').slice(1).join('='); csrfToken = issued ? decodeURIComponent(issued) : undefined } const cookieToken = document.cookie.split('; ').find(row => row.startsWith('XSRF-TOKEN='))?.split('=').slice(1).join('='); const csrf = csrfToken ?? (cookieToken ? decodeURIComponent(cookieToken) : undefined); if (csrf) config.headers['X-XSRF-TOKEN'] = csrf; return config })
api.interceptors.response.use(response => response, error => {
  if (error.response && !error.response.data?.error) error.response.data = { ...error.response.data, error: `Request failed with HTTP ${error.response.status}` }
  else if (!error.response) error.response = { status: 0, data: { error: 'Could not reach the backend API. Check that it is running and the frontend proxy is configured.' } }
  return Promise.reject(error)
})
export type User = { id: string; email: string; fullName: string; role: 'USER'|'ADMIN' }
export type Account = { id:string;accountNumber:string;currency:string;status:string;balance:number }
export type Transfer = { id:string;reference:string;sourceAccountId:string;destinationAccountId:string;amount:number;currency:string;description:string;status:string;createdAt:string }
