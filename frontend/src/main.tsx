import React from 'react';import ReactDOM from 'react-dom/client';import { BrowserRouter } from 'react-router-dom';import { QueryClient,QueryClientProvider } from '@tanstack/react-query';import { refreshCsrfToken } from './api';import App from './App';import './style.css'
void refreshCsrfToken().catch(()=>{})
const client=new QueryClient({defaultOptions:{queries:{retry:1,refetchOnWindowFocus:false}}})
ReactDOM.createRoot(document.getElementById('root')!).render(<React.StrictMode><QueryClientProvider client={client}><BrowserRouter><App/></BrowserRouter></QueryClientProvider></React.StrictMode>)
