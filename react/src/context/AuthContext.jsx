import { createContext, useContext, useState, useEffect } from 'react';
import axios from 'axios';
import { API_URL } from '../config/api';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);

    // Set default axios config
    axios.defaults.withCredentials = true;

    const checkAuth = async () => {
        try {
            const response = await axios.get(`${API_URL}/api/auth/check`);
            if (response.status === 200) {
                // response.data is { email, role, domain }
                setUser(response.data);
            }
        } catch (error) {
            setUser(null);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        checkAuth();
    }, []);

    const login = async (email, password) => {
        try {
            await axios.post(`${API_URL}/api/auth/login`, { email, password });
            await checkAuth();
            return true;
        } catch (error) {
            console.error('Login failed', error);
            throw error;
        }
    };

    const signup = async (email) => { // Removed password param
        try {
            await axios.post(`${API_URL}/api/auth/signup`, { email }); // Send only email
            return true;
        } catch (error) {
            console.error('Signup failed', error);
            throw error;
        }
    };

    const logout = async () => {
        try {
            await axios.post(`${API_URL}/api/auth/logout`);
            setUser(null);
        } catch (error) {
            console.error('Logout failed', error);
        }
    };

    const value = {
        user,
        loading,
        login,
        signup,
        logout,
        isAuthenticated: !!user,
        isAdmin: user?.role === 'ADMIN' || user?.email === 'admin'
    };

    return (
        <AuthContext.Provider value={value}>
            {!loading && children}
        </AuthContext.Provider>
    );
};

export const useAuth = () => {
    const context = useContext(AuthContext);
    if (!context) {
        throw new Error('useAuth must be used within an AuthProvider');
    }
    return context;
};

export default AuthContext;
