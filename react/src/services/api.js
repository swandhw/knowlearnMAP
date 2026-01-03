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
 * Workspace API
 */
export const workspaceApi = {
    /**
     * 모든 워크스페이스 조회
     */
    getAll: async () => {
        return await apiCall('/workspaces');
    },

    /**
     * 워크스페이스 단건 조회
     */
    getById: async (id) => {
        return await apiCall(`/workspaces/${id}`);
    },

    /**
     * 워크스페이스 생성
     */
    create: async (workspaceData) => {
        return await apiCall('/workspaces', {
            method: 'POST',
            body: JSON.stringify(workspaceData),
        });
    },

    /**
     * 워크스페이스 수정
     */
    update: async (id, workspaceData) => {
        return await apiCall(`/workspaces/${id}`, {
            method: 'PUT',
            body: JSON.stringify(workspaceData),
        });
    },

    /**
     * 워크스페이스 삭제
     */
    delete: async (id) => {
        return await apiCall(`/workspaces/${id}`, {
            method: 'DELETE',
        });
    },
};

/**
 * Prompt API
 */
export const promptApi = {
    /**
     * 모든 프롬프트 조회
     */
    getAll: async (params = {}) => {
        const queryString = new URLSearchParams(params).toString();
        const endpoint = queryString ? `/v1/prompts?${queryString}` : '/v1/prompts';
        return await apiCall(endpoint);
    },

    /**
     * 프롬프트 단건 조회
     */
    getById: async (code) => {
        return await apiCall(`/v1/prompts/${code}`);
    },

    /**
     * 프롬프트 생성
     */
    create: async (promptData) => {
        return await apiCall('/v1/prompts', {
            method: 'POST',
            body: JSON.stringify(promptData),
        });
    },

    /**
     * 프롬프트 수정
     */
    update: async (code, promptData) => {
        return await apiCall(`/v1/prompts/${code}`, {
            method: 'PUT',
            body: JSON.stringify(promptData),
        });
    },

    /**
     * 프롬프트 삭제
     */
    delete: async (code) => {
        return await apiCall(`/v1/prompts/${code}`, {
            method: 'DELETE',
        });
    },
};

/**
 * Ontology API
 */
export const ontologyApi = {
    /**
     * ArangoDB 동기화
     */
    sync: async (workspaceId, dropExist = true) => {
        return await apiCall(`/ontology/sync/${workspaceId}?dropExist=${dropExist}`, {
            method: 'POST',
        });
    },
};
