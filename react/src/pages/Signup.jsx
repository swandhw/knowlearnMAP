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

    const [agreed, setAgreed] = useState(false);

    const handleSubmit = async (e) => {
        e.preventDefault();
        if (!agreed) {
            setError('서비스 이용을 위해 면책 조항에 동의해야 합니다.');
            return;
        }
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

                    <div className="disclaimer-box" style={{
                        margin: '20px 0',
                        padding: '15px',
                        backgroundColor: '#f8f9fa',
                        borderRadius: '6px',
                        border: '1px solid #e9ecef',
                        fontSize: '0.85rem',
                        color: '#495057',
                        textAlign: 'left',
                        lineHeight: '1.5'
                    }}>
                        <p style={{ margin: '0 0 10px 0', fontWeight: 'bold' }}>[면책 조항]</p>
                        본 서비스는 기술 데모용으로 제공되며 데이터 보존을 보장하지 않습니다.
                        시스템 최적화, 보안 업데이트 등 운영상 필요 시 고객의 사전 동의나 공지 없이
                        유지 기간 이전이라도 데이터를 즉시 삭제할 수 있습니다.
                        중요 자료는 반드시 별도 백업하시기 바랍니다.

                        <div style={{ marginTop: '10px', display: 'flex', alignItems: 'center', gap: '8px' }}>
                            <input
                                type="checkbox"
                                id="disclaimer-agree"
                                checked={agreed}
                                onChange={(e) => setAgreed(e.target.checked)}
                                style={{ width: 'auto', margin: 0 }}
                            />
                            <label htmlFor="disclaimer-agree" style={{ margin: 0, cursor: 'pointer', fontWeight: 'bold', color: '#2563eb' }}>
                                위 내용을 확인하였으며 이에 동의합니다.
                            </label>
                        </div>
                    </div>

                    <button type="submit" className="login-btn" disabled={!agreed} style={{ opacity: agreed ? 1 : 0.6 }}>
                        SIGN UP
                    </button>
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
