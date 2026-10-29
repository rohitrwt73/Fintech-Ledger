
import axios from 'axios';

export const api = axios.create({
  baseURL: `${import.meta.env.VITE_API_BASE_URL}/api/v1`,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
});

let csrfToken: string | undefined;

export async function refreshCsrfToken(): Promise<void> {
  const response = await api.get<{ token: string }>('/auth/csrf');
  csrfToken = response.data.token;
}

api.interceptors.request.use(async (config) => {
  const method = config.method?.toLowerCase();

  if (
    method &&
    method !== 'get' &&
    method !== 'head' &&
    method !== 'options'
  ) {

    await refreshCsrfToken();

    if (csrfToken) {
      config.headers.set('X-XSRF-TOKEN', csrfToken);
    }
  }

  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response) {
      const data = error.response.data;

      if (data && typeof data === 'object' && !data.error) {
        error.response.data = {
          ...data,
          error: `Request failed with HTTP ${error.response.status}`,
        };
      }
    } else {
      error.response = {
        status: 0,
        data: {
          error:
            'Could not reach the backend API. Check the backend URL and CORS configuration.',
        },
      };
    }

    return Promise.reject(error);
  }
);

export type User = {
  id: string;
  email: string;
  fullName: string;
  role: 'USER' | 'ADMIN';
};

export type Account = {
  id: string;
  accountNumber: string;
  currency: string;
  status: string;
  balance: number;
};

export type Transfer = {
  id: string;
  reference: string;
  sourceAccountId: string;
  destinationAccountId: string;
  amount: number;
  currency: string;
  description: string;
  status: string;
  createdAt: string;
};
