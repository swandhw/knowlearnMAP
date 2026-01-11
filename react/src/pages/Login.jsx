import { useState, useEffect } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { useAlert } from '../context/AlertContext';
import './Login.css';

const Login = () => {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [saveId, setSaveId] = useState(false);
    const [savePw, setSavePw] = useState(false);
    const [error, setError] = useState('');
    const { login } = useAuth();
    const { showAlert } = useAlert();
    const navigate = useNavigate();

    useEffect(() => {
        const savedEmail = localStorage.getItem('savedEmail');
        const savedPassword = localStorage.getItem('savedPassword');

        if (savedEmail) {
            setEmail(savedEmail);
            setSaveId(true);
        }
        if (savedPassword) {
            setPassword(savedPassword);
            setSavePw(true);
        }
    }, []);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        try {
            await login(email, password);

            // Handle Save ID
            if (saveId) {
                localStorage.setItem('savedEmail', email);
            } else {
                localStorage.removeItem('savedEmail');
            }

            // Handle Save PW (Security warning: Storing plaintext password in localStorage is unsafe for production)
            if (savePw) {
                localStorage.setItem('savedPassword', password);
            } else {
                localStorage.removeItem('savedPassword');
            }

            navigate('/');
        } catch (err) {
            setError('Login failed. Please check your credentials.');
        }
    };

    return (
        <div className="login-container">
            <div className="login-card">
                <div style={{ textAlign: 'center', marginBottom: '24px' }}>
                    <img src="/knowlearn_logo.png" alt="KNOWLEARN MAP" style={{ height: '48px' }} />
                </div>
                <h1>SIGN IN</h1>
                {error && <div className="error-message">{error}</div>}
                <form onSubmit={handleSubmit}>
                    <div className="form-group">
                        <label>Email ID</label>
                        <input
                            type="text"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            required
                            placeholder="user"
                        />
                    </div>
                    <div className="form-group">
                        <label>Password</label>
                        <input
                            type="password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            required
                            placeholder="password"
                        />
                    </div>
                    <div className="options-group">
                        <label className="checkbox-label">
                            <input
                                type="checkbox"
                                checked={saveId}
                                onChange={(e) => setSaveId(e.target.checked)}
                            />
                            Save ID
                        </label>
                        <label className="checkbox-label">
                            <input
                                type="checkbox"
                                checked={savePw}
                                onChange={(e) => setSavePw(e.target.checked)}
                            />
                            Save PW
                        </label>
                    </div>
                    <button type="submit" className="login-btn">LOGIN</button>
                </form>
                <div className="login-footer">
                    <Link to="/signup" style={{ color: '#646cff' }}>Sign Up</Link>
                    <span className="divider">|</span>
                    <button className="text-btn" onClick={() => showAlert('Not implemented yet.', { title: '알림' })}>Forgot Password</button>
                </div>
            </div>
        </div>
    );
};

export default Login;
