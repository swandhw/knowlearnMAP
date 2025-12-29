import { API_URL } from '../config/api';

/**
 * Document API 서비스
 */
export const documentApi = {
    /**
     * 워크스페이스의 문서 목록 조회
     * @param {number} workspaceId - 워크스페이스 ID
     * @returns {Promise<Array>} 문서 목록
     */
    getByWorkspace: async (workspaceId) => {
        const response = await fetch(`${API_URL}/api/documents/workspace/${workspaceId}`, {
            credentials: 'include'
        });
        if (!response.ok) {
            throw new Error('Failed to fetch documents');
        }
        const result = await response.json();
        return result.data || [];
    },

    /**
     * 문서 상세 조회
     * @param {number} documentId - 문서 ID
     * @returns {Promise<Object>} 문서 정보
     */
    getById: async (documentId) => {
        const response = await fetch(`${API_URL}/api/documents/${documentId}`, {
            credentials: 'include'
        });
        if (!response.ok) {
            throw new Error('Failed to fetch document');
        }
        const result = await response.json();
        return result.data;
    },

    /**
     * 파이프라인 상태 조회
     * @param {number} documentId - 문서 ID
     * @returns {Promise<Object|null>} 파이프라인 상태 정보
     */
    getPipelineStatus: async (documentId) => {
        try {
            const response = await fetch(`${API_URL}/api/pipeline/status/${documentId}`, {
                credentials: 'include'
            });
            if (!response.ok) return null;
            return await response.json();
        } catch (error) {
            console.error('Failed to fetch pipeline status:', error);
            return null;
        }
    },

    /**
     * 문서의 모든 페이지 조회
     * @param {number} documentId - 문서 ID
     * @returns {Promise<Array>} 페이지 목록
     */
    getPages: async (documentId) => {
        const response = await fetch(`${API_URL}/api/documents/${documentId}/pages`, {
            credentials: 'include'
        });
        if (!response.ok) {
            throw new Error('Failed to fetch document pages');
        }
        const result = await response.json();
        return result.data || [];
    }
};
