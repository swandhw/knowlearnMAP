import axiosClient from './axiosClient';

export const versionService = {
  // 버전 목록 조회
  getVersions: async (code, params = {}) => {
    const { page, size, status } = params;
    const queryParams = new URLSearchParams();

    if (page !== undefined) queryParams.append('page', page);
    if (size !== undefined) queryParams.append('size', size);
    if (status) queryParams.append('status', status);

    const query = queryParams.toString();
    return axiosClient.get(`/prompts/${code}/versions${query ? `?${query}` : ''}`);
  },

  // 버전 상세 조회
  getVersion: async (code, versionId) => {
    return axiosClient.get(`/prompts/${code}/versions/${versionId}`);
  },

  // 버전 생성 (Draft 저장)
  createVersion: async (code, data) => {
    return axiosClient.post(`/prompts/${code}/versions`, data);
  },

  // 버전 수정
  updateVersion: async (code, versionId, data) => {
    return axiosClient.put(`/prompts/${code}/versions/${versionId}`, data);
  },

  // 버전 배포
  publishVersion: async (code, versionId, data) => {
    return axiosClient.post(`/prompts/${code}/versions/${versionId}/publish`, data);
  },

  // 버전 삭제
  deleteVersion: async (code, versionId) => {
    return axiosClient.delete(`/prompts/${code}/versions/${versionId}`);
  },

  // 버전 복사
  copyVersion: async (code, versionId, data) => {
    return axiosClient.post(`/prompts/${code}/versions/${versionId}/copy`, data);
  },

  // 버전 종합 평가 수정
  updateRating: async (code, versionId, data) => {
    return axiosClient.put(`/prompts/${code}/versions/${versionId}/rating`, data);
  },
};
