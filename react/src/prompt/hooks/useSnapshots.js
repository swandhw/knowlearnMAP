import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import axiosClient from '../api/axiosClient';

// 스냅샷 목록 조회
export const useSnapshots = (code, params = {}) => {
  return useQuery({
    queryKey: ['snapshots', code, params],
    queryFn: async () => {
      const response = await axiosClient.get(`/prompts/${code}/all-snapshots`, { params });

      // axiosClient는 이미 response.data를 반환하므로
      // response 자체가 { success, message, data } 형태
      if (response && response.data) {
        return response.data; // { content, totalElements, ... }
      }

      return { content: [], totalElements: 0 };
    },
    enabled: !!code,
  });
};

// 만족도 업데이트
export const useUpdateSatisfaction = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async ({ snapshotId, satisfaction }) => {
      const { data } = await axiosClient.put(`/prompts/snapshots/${snapshotId}/satisfaction`, {
        satisfaction,
      });
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries(['snapshots']);
    },
  });
};

// 스냅샷 삭제
export const useDeleteSnapshot = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (snapshotId) => {
      const { data } = await axiosClient.delete(`/prompts/snapshots/${snapshotId}`);
      return data;
    },
    onSuccess: () => {
      queryClient.invalidateQueries(['snapshots']);
    },
  });
};
