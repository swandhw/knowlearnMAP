import axiosClient from './axiosClient';

export const variableService = {
  // 변수 스키마 조회
  getVariableSchemas: async (code) => {
    return axiosClient.get(`/prompts/${code}/variable-schemas`);
  },

  // 변수 스키마 저장/수정
  saveVariableSchemas: async (code, data) => {
    return axiosClient.put(`/prompts/${code}/variable-schemas`, data);
  },
};
