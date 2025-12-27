import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import './Login.css'; // Reuse Login styles

const Signup = () => {
    const [email, setEmail] = useState('');
    // Password fields removed as per request
    const [error, setError] = useState('');
    const { signup } = useAuth();
    const navigate = useNavigate();

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');

        try {
            await signup(email); // Removed password argument
            alert('인증 이메일이 발송되었습니다. 이메일을 확인하여 인증을 완료해주세요.');
            navigate('/login');
        } catch (err) {
            setError(err.response?.data || '회원가입에 실패했습니다.');
        }
    };

    return (
        <div className="login-container">
            <div className="login-card">
                <h1>SIGN UP</h1>
                {error && <div className="error-message">{error}</div>}
                <form onSubmit={handleSubmit}>
                    <div className="form-group">
                        <label>Email</label>
                        <input
                            type="email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            required
                            placeholder="user@example.com"
                        />
                    </div>
                    <button type="submit" className="login-btn">SIGN UP</button>
                </form>
                <div className="login-footer">
                    <span>Already have an account?</span>
                    <Link to="/login" style={{ color: '#646cff' }}>Sign In</Link>
                </div>
            </div>
        </div>
    );
};

export default Signup;
