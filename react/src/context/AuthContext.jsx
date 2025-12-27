import { createContext, useContext, useState, useEffect } from 'react';
import axios from 'axios';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
    const [user, setUser] = useState(null);
    const [loading, setLoading] = useState(true);

    // Set default axios config
    axios.defaults.withCredentials = true;
    const API_URL = import.meta.env.VITE_APP_API_URL || 'http://localhost:8080';

    const checkAuth = async () => {
        try {
            const response = await axios.get(`${API_URL}/api/auth/check`);
            if (response.status === 200) {
                // response.data is only the username (string) based on my backend code
                // Let's assume it returns just the email string or an object if I change it later
                // For now backend returns String.
                setUser({ email: response.data, role: response.data === 'admin' ? 'ADMIN' : 'USER' });
                // Note: The backend checkAuth returns authentication.getName() which is email.
                // Role is not currently returned by /check, but I can improve backend later.
                // For now, let's assume valid user.
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
        isAuthenticated: !!user
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
