import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { promptService } from '../api/promptService';

export const usePrompts = (filters = {}) => {
  return useQuery({
    queryKey: ['prompts', filters],
    queryFn: async () => {
      const response = await promptService.getPrompts(filters);
      return response;
    },
    staleTime: 5 * 60 * 1000, // 5분
    keepPreviousData: true,
  });
};

export const usePromptDetail = (code) => {
  return useQuery({
    queryKey: ['prompt', code],
    queryFn: async () => {
      const response = await promptService.getPrompt(code);
      return response;
    },
    enabled: !!code,
    staleTime: 5 * 60 * 1000, // 5분
  });
};

export const useCreatePrompt = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: promptService.createPrompt,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['prompts'] });
    }
  });
};

export const useUpdatePrompt = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ code, data }) => promptService.updatePrompt(code, data),
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({ queryKey: ['prompt', variables.code] });
      queryClient.invalidateQueries({ queryKey: ['prompts'] });
    }
  });
};

export const useDeletePrompt = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (code) => promptService.deletePrompt(code),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['prompts'] });
    }
  });
};

export const useCheckCode = (code) => {
  return useQuery({
    queryKey: ['check-code', code],
    queryFn: async () => {
      const response = await promptService.checkCode(code);
      return response;
    },
    enabled: !!code
  });
};
