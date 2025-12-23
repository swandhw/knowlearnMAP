import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { versionService } from '../api/versionService';

export const useVersions = (code, params = {}) => {
  return useQuery({
    queryKey: ['versions', code, params],
    queryFn: async () => {
      const response = await versionService.getVersions(code, params);
      return response;
    },
    enabled: !!code,
    staleTime: 5 * 60 * 1000, // 5분
  });
};

export const useVersion = (code, versionId) => {
  return useQuery({
    queryKey: ['version', code, versionId],
    queryFn: async () => {
      const response = await versionService.getVersion(code, versionId);
      return response;
    },
    enabled: !!code && !!versionId
  });
};

export const useCreateVersion = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ code, data }) => versionService.createVersion(code, data),
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({ queryKey: ['versions', variables.code] });
      queryClient.invalidateQueries({ queryKey: ['prompt', variables.code] });
    }
  });
};

export const useUpdateVersion = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ code, versionId, data }) => versionService.updateVersion(code, versionId, data),
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({ queryKey: ['versions', variables.code] });
      queryClient.invalidateQueries({ queryKey: ['prompt', variables.code] });
      queryClient.invalidateQueries({ queryKey: ['version', variables.code, variables.versionId] });
    }
  });
};

export const usePublishVersion = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ code, versionId, data }) => versionService.publishVersion(code, versionId, data),
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({ queryKey: ['versions', variables.code] });
      queryClient.invalidateQueries({ queryKey: ['prompt', variables.code] });
    }
  });
};

export const useDeleteVersion = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ code, versionId }) => versionService.deleteVersion(code, versionId),
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({ queryKey: ['versions', variables.code] });
      queryClient.invalidateQueries({ queryKey: ['prompt', variables.code] });
    }
  });
};

export const useCopyVersion = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ code, versionId, data }) => versionService.copyVersion(code, versionId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['versions'] });
      queryClient.invalidateQueries({ queryKey: ['prompts'] });
    }
  });
};

export const useUpdateRating = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ code, versionId, data }) => versionService.updateRating(code, versionId, data),
    onSuccess: (data, variables) => {
      queryClient.invalidateQueries({ queryKey: ['version', variables.code, variables.versionId] });
      queryClient.invalidateQueries({ queryKey: ['versions', variables.code] });
    }
  });
};
