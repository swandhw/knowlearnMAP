import { useState, useEffect } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import axios from 'axios';
import { useAlert } from '../context/AlertContext';
import './Login.css';
import { API_URL } from '../config/api';

const SetPassword = () => {
    const [searchParams] = useSearchParams();
    const [password, setPassword] = useState('');
    const [confirmPassword, setConfirmPassword] = useState('');
    const [status, setStatus] = useState('');
    const [token, setToken] = useState('');
    const { showAlert } = useAlert();
    const navigate = useNavigate();

    useEffect(() => {
        const t = searchParams.get('token');
        if (t) {
            setToken(t);
        } else {
            setStatus('Invalid Link');
        }
    }, [searchParams]);

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (password !== confirmPassword) {
            setStatus('Passwords do not match');
            return;
        }

        try {
            await axios.post(`${API_URL}/api/auth/set-password`, { token, password });
            showAlert('비밀번호가 성공적으로 설정되었습니다. 로그인해주세요.', { title: '성공', onConfirm: () => navigate('/login') });
        } catch (err) {
            setStatus('Failed to set password. Token may be invalid or expired.');
        }
    };

    if (!token) {
        return (
            <div className="login-container">
                <div className="login-card">
                    <div className="error-message">Invalid Link (No Token)</div>
                </div>
            </div>
        );
    }

    return (
        <div className="login-container">
            <div className="login-card">
                <h1>Set Password</h1>
                {status && <div className="error-message">{status}</div>}
                <form onSubmit={handleSubmit}>
                    <div className="form-group">
                        <label>New Password</label>
                        <input
                            type="password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            required
                        />
                    </div>
                    <div className="form-group">
                        <label>Confirm Password</label>
                        <input
                            type="password"
                            value={confirmPassword}
                            onChange={(e) => setConfirmPassword(e.target.value)}
                            required
                        />
                    </div>
                    <button type="submit" className="login-btn">Set Password</button>
                </form>
            </div>
        </div>
    );
};

export default SetPassword;
