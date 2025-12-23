import axiosClient from './axiosClient';

export const testService = {
  // 환경 설정 저장 (설정 저장/수정)
  saveLlmConfig: async (code, versionId, data) => {
    return axiosClient.post(`/prompts/${code}/versions/${versionId}/llm-config`, data);
  },

  // 환경 설정 조회 (Singleton)
  getLlmConfig: async (code, versionId) => {
    return axiosClient.get(`/prompts/${code}/versions/${versionId}/llm-config`);
  },

  // 테스트 스냅샷 목록 조회
  getSnapshots: async (code, versionId, params = {}) => {
    const { page, size, minSatisfaction } = params;
    const queryParams = new URLSearchParams();

    if (page !== undefined) queryParams.append('page', page);
    if (size !== undefined) queryParams.append('size', size);
    if (minSatisfaction !== undefined) queryParams.append('minSatisfaction', minSatisfaction);

    const query = queryParams.toString();
    return axiosClient.get(`/prompts/${code}/versions/${versionId}/snapshots${query ? `?${query}` : ''}`);
  },

  // 테스트 만족도 저장
  saveSatisfaction: async (snapshotId, data) => {
    return axiosClient.put(`/prompts/snapshots/${snapshotId}/satisfaction`, data);
  },

  // 테스트 스냅샷 삭제
  deleteSnapshot: async (snapshotId) => {
    return axiosClient.delete(`/prompts/snapshots/${snapshotId}`);
  },
};
