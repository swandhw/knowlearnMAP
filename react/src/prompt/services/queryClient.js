import { QueryClient } from '@tanstack/react-query';

// React Query 클라이언트 생성
const queryClient = new QueryClient({
    defaultOptions: {
        queries: {
            refetchOnWindowFocus: false,
            retry: 1,
            staleTime: 5 * 60 * 1000, // 5분
        },
    },
});

export default queryClient;
