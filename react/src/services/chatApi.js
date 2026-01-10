import { API_BASE_URL } from '../config/api';

/**
 * API 호출 공통 함수
 */
const apiCall = async (endpoint, options = {}) => {
    try {
        const response = await fetch(`${API_BASE_URL}${endpoint}`, {
            headers: {
                'Content-Type': 'application/json',
                ...options.headers,
            },
            credentials: 'include',
            ...options,
        });

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const data = await response.json();

        // 백엔드 ApiResponse 포맷 처리
        if (data.success === false) {
            throw new Error(data.message || '요청 실패');
        }

        return data.data; // ApiResponse의 data 필드 반환
    } catch (error) {
        console.error('API 호출 실패:', error);
        throw error;
    }
};

/**
 * Chat API
 */
export const chatApi = {
    /**
     * Send chat message with RAG and ontology search
     * 
     * @param {number} workspaceId - Workspace ID
     * @param {string} message - User message
     * @param {number[]} documentIds - Optional document IDs for filtering
     * @returns {Promise<{ragResults: Array, ontologyResults: Array}>}
     */
    send: async (workspaceId, message, documentIds = null) => {
        return await apiCall('/chat/send', {
            method: 'POST',
            body: JSON.stringify({
                workspaceId,
                message,
                documentIds
            })
        });
    }
};
