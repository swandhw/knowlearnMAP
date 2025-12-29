/**
 * API URL Configuration
 * - localhost: http://localhost:8080
 * - mapdev.knowlearn.kr: same origin (proxy)
 * - map.knowlearn.kr: same origin (proxy)
 */

const getApiUrl = () => {
    const hostname = window.location.hostname;

    if (hostname === 'localhost' || hostname === '127.0.0.1') {
        return 'http://localhost:8080';
    }

    // For production domains (mapdev.knowlearn.kr, map.knowlearn.kr)
    // Use same origin - Nginx will proxy /api to backend
    return '';
};

export const API_URL = getApiUrl();
export const API_BASE_URL = `${API_URL}/api`;
