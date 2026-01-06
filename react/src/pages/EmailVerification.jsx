import { useEffect, useState } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import axios from 'axios';
import './Login.css';
import { API_URL } from '../config/api';

const EmailVerification = () => {
    const [searchParams] = useSearchParams();
    const [status, setStatus] = useState('Verifying...');
    const [error, setError] = useState(false);
    const navigate = useNavigate();

    useEffect(() => {
        const token = searchParams.get('token');
        if (!token) {
            setStatus('Invalid URL');
            setError(true);
            return;
        }

        const verify = async () => {
            try {
                await axios.get(`${API_URL}/api/auth/verify-email?token=${token}`);
                setStatus('Email Verified Successfully. Redirecting to password setup...');
                setTimeout(() => {
                    navigate(`/set-password?token=${token}`);
                }, 1500);
            } catch (err) {
                const errorMessage = err.response?.data || err.message || 'Verification Failed. Token may be invalid or expired.';
                setStatus(`Verification Failed: ${errorMessage}`);
                setError(true);
            }
        };

        verify();
    }, [searchParams]);

    return (
        <div className="login-container">
            <div className="login-card">
                <h1>Email Verification</h1>
                <div className={`message ${error ? 'error-message' : 'success-message'}`}>
                    {status}
                </div>
                <div className="login-footer">
                    <button className="text-btn" onClick={() => navigate('/login')}>Back to Login</button>
                </div>
            </div>
        </div>
    );
};

export default EmailVerification;
