import axios from 'axios';

const axiosClient = axios.create({
  baseURL: import.meta.env.VITE_APP_API_URL + '/api/v1',
  timeout: 90000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor
axiosClient.interceptors.request.use(
  (config) => {
    // Add authentication token
    const accessToken = localStorage.getItem('serviceToken');
    if (accessToken) {
      config.headers['Authorization'] = `Bearer ${accessToken}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor
axiosClient.interceptors.response.use(
  (response) => {
    // Return data directly if response has standard ApiResponse format
    if (response.data && response.data.success !== undefined) {
      return response.data;
    }
    return response.data;
  },
  (error) => {
    console.error('Unauthorized access');
    // Handle errors
    if (error.response) {
      // Server responded with error
      const { status, data } = error.response;

      if (status === 401) {
        // Unauthorized - redirect to login
        console.error('Unauthorized access');
        // window.location.href = '/login';
      } else if (status === 403) {
        console.error('Forbidden');
      } else if (status === 404) {
        console.error('Resource not found');
      }

      return Promise.reject(data || error);
    } else if (error.request) {
      // Request made but no response
      console.error('No response from server');
      return Promise.reject({ message: 'Network error. Please try again.' });
    } else {
      // Something else happened
      console.error('Request error:', error.message);
      return Promise.reject(error);
    }
  }
);

export default axiosClient;
