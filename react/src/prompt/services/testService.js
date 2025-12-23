import { testService as apiTestService } from '../api/testService';

export const testService = {
    // 테스트 셋 저장 (설정 저장/수정)
    saveTestSet: async (params) => {
        const { promptCode, versionId, testName, variables, llmModel, temperature, topP, maxOutputTokens, topK, n } = params;

        return apiTestService.saveTestSet(promptCode, versionId, {
            testName,
            versionId,
            variables: variables || {},
            llmConfig: {
                model: llmModel,
                temperature,
                topP,
                maxOutputTokens,
                topK,
                n
            }
        });
    },

    // 테스트 셋 조회 (Singleton)
    getTestSet: async (promptCode, versionId) => {
        return apiTestService.getTestSet(promptCode, versionId);
    },

    // 만족도 저장
    saveSatisfaction: async (snapshotId, satisfaction, notes) => {
        return apiTestService.saveSatisfaction(snapshotId, {
            satisfaction,
            notes
        });
    }
};
