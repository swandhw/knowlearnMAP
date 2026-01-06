import React from 'react';
import { upgradeApi } from '../services/api';
import { useAuth } from '../context/AuthContext';
import './UpgradeModal.css';

const UpgradeModal = ({ isOpen, onClose }) => {
    const { user } = useAuth();
    const [view, setView] = React.useState('plans'); // 'plans', 'form'
    const [targetType, setTargetType] = React.useState('PRO_UPGRADE'); // 'PRO_UPGRADE', 'MAX_CONSULTATION'
    const [loading, setLoading] = React.useState(false);
    const [consentChecked, setConsentChecked] = React.useState(false);
    const [formData, setFormData] = React.useState({
        company: '',
        name: '',
        phone: '',
        files: []
    });

    if (!isOpen) return null;

    const currentGrade = user?.grade || 'FREE';

    const handleClose = () => {
        if (loading) return;
        setView('plans');
        setFormData({ company: '', name: '', phone: '', files: [] });
        setConsentChecked(false);
        onClose();
    };

    const openForm = (type) => {
        setTargetType(type);
        setConsentChecked(false);
        setView('form');
    };

    const handleFileChange = (e) => {
        const selectedFiles = Array.from(e.target.files);
        if (selectedFiles.length > 2) {
            alert('최대 2개의 파일만 업로드 가능합니다.');
            return;
        }
        setFormData({ ...formData, files: selectedFiles });
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);

        try {
            const data = new FormData();
            data.append('type', targetType);
            data.append('company', formData.company);
            data.append('name', formData.name);
            data.append('phone', formData.phone);

            if (formData.files && formData.files.length > 0) {
                formData.files.forEach(file => {
                    data.append('files', file);
                });
            }

            await upgradeApi.request(data);

            const msg = targetType === 'PRO_UPGRADE'
                ? 'Pro 업그레이드 신청이 완료되었습니다.\n검토 후 승인 처리됩니다.'
                : 'Max 상담 신청이 완료되었습니다.\n담당자가 확인 후 연락드리겠습니다.';

            alert(msg);
            handleClose();
        } catch (error) {
            console.error('Request failed:', error);
            alert('오류가 발생했습니다: ' + (error.message || 'Unknown Error'));
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="upgrade-modal-overlay" onClick={handleClose}>
            <div className="upgrade-modal-content" onClick={e => e.stopPropagation()}>
                <button className="upgrade-modal-close" onClick={handleClose}>&times;</button>

                {view === 'plans' ? (
                    <>
                        <div className="upgrade-header">
                            <h2 className="upgrade-title">함께 성장하는 요금제</h2>
                            <div className="upgrade-toggle-container">
                                <div className="upgrade-toggle">
                                    <button className="toggle-btn active">플랜 선택</button>
                                </div>
                            </div>
                        </div>

                        <div className="plans-container">
                            {/* Free Plan */}
                            <div className={`plan-card ${currentGrade === 'FREE' ? 'current-plan' : ''}`}>
                                <div className="plan-icon">
                                    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"></path></svg>
                                </div>
                                <h3 className="plan-name">Free</h3>
                                <p className="plan-desc">개인 학습 및 기초 체험</p>
                                <div className="plan-price">0원 <span className="plan-period">/월</span></div>
                                <div className="promo-box">모든 기능 무료 제공 (데모)</div>

                                <button className="plan-btn" disabled>
                                    {currentGrade === 'FREE' ? '현재 이용 중' : '기본 포함'}
                                </button>

                                <ul className="plan-features-list">
                                    <li><strong>워크스페이스 1개</strong></li>
                                    <li>데이터: 3개 (각 5장)</li>
                                    <li>Open LLM RAG</li>
                                </ul>
                            </div>

                            {/* Pro Plan */}
                            <div className={`plan-card highlight ${currentGrade === 'PRO' ? 'current-plan' : ''}`}>
                                <div className="plan-icon">
                                    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"></polygon></svg>
                                </div>
                                <h3 className="plan-name">Pro</h3>
                                <p className="plan-desc">팀 단위 지식 자산화</p>
                                <div className="plan-price">0원 <span className="plan-period">/월</span></div>
                                <div className="promo-box">하이브리드 라우팅 잠금 해제</div>

                                {currentGrade === 'PRO' || currentGrade === 'MAX' ? (
                                    <button className="plan-btn primary" disabled style={{ opacity: 0.6, cursor: 'default' }}>
                                        {currentGrade === 'PRO' ? '현재 이용 중' : '포함됨'}
                                    </button>
                                ) : (
                                    <button className="plan-btn primary" onClick={() => openForm('PRO_UPGRADE')}>
                                        무료로 Pro 업그레이드
                                    </button>
                                )}

                                <ul className="plan-features-list">
                                    <li><strong>워크스페이스 3개</strong></li>
                                    <li>데이터: 10개 (각 20장)</li>
                                    <li>에이전트 라우팅</li>
                                </ul>
                            </div>

                            {/* Max Plan */}
                            <div className={`plan-card ${currentGrade === 'MAX' ? 'current-plan' : ''}`}>
                                <div className="plan-icon">
                                    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M12 2a10 10 0 1 0 10 10A10 10 0 0 0 12 2zm0 18a8 8 0 1 1 8-8 8 8 0 0 1-8 8z"></path><path d="M12 6v6l4 2"></path></svg>
                                </div>
                                <h3 className="plan-name">Max</h3>
                                <p className="plan-desc">보안 특화 엔터프라이즈</p>
                                <div className="plan-price">GPU 비용 협의 <span className="plan-period">/별도</span></div>
                                <div className="promo-box">전용 sLLM 구축</div>

                                {currentGrade === 'MAX' ? (
                                    <button className="plan-btn accent" disabled style={{ opacity: 0.6, cursor: 'default' }}>
                                        현재 이용 중
                                    </button>
                                ) : (
                                    <button className="plan-btn accent" onClick={() => openForm('MAX_CONSULTATION')}>
                                        Max 상담 신청하기
                                    </button>
                                )}

                                <ul className="plan-features-list">
                                    <li><strong>워크스페이스 무제한</strong></li>
                                    <li>전용 sLLM 추론 엔진</li>
                                    <li>독자적 보안 환경</li>
                                </ul>
                            </div>
                        </div>
                        <div className="disclaimer">* 본 요금제는 데모 기간 한정입니다.</div>
                    </>
                ) : (
                    <div className="pro-upgrade-form">
                        <div className="upgrade-header">
                            <h2 className="upgrade-title">
                                {targetType === 'PRO_UPGRADE' ? 'Pro 업그레이드 신청' : 'Max 상담 신청'}
                            </h2>
                            <p className="plan-desc" style={{ maxWidth: '600px', margin: '0 auto' }}>
                                {targetType === 'PRO_UPGRADE'
                                    ? '회사 정보와 문서를 등록해주시면 검토 후 승인해드립니다.'
                                    : '전문가 상담을 위해 연락처와 문의 내용을 남겨주세요.'}
                            </p>
                        </div>

                        <form onSubmit={handleSubmit} style={{ maxWidth: '500px', margin: '0 auto' }}>
                            <div className="form-group" style={{ marginBottom: '15px' }}>
                                <label style={{ display: 'block', marginBottom: '5px', fontWeight: 'bold' }}>회사명 (또는 소속)</label>
                                <input type="text" className="modal-input" required value={formData.company} onChange={e => setFormData({ ...formData, company: e.target.value })} style={{ width: '100%', padding: '10px', borderRadius: '6px', border: '1px solid #ddd' }} />
                            </div>
                            <div className="form-group" style={{ marginBottom: '15px' }}>
                                <label style={{ display: 'block', marginBottom: '5px', fontWeight: 'bold' }}>담당자명</label>
                                <input type="text" className="modal-input" required value={formData.name} onChange={e => setFormData({ ...formData, name: e.target.value })} style={{ width: '100%', padding: '10px', borderRadius: '6px', border: '1px solid #ddd' }} />
                            </div>
                            <div className="form-group" style={{ marginBottom: '15px' }}>
                                <label style={{ display: 'block', marginBottom: '5px', fontWeight: 'bold' }}>핸드폰 번호</label>
                                <input type="tel" className="modal-input" required value={formData.phone} onChange={e => setFormData({ ...formData, phone: e.target.value })} placeholder="010-0000-0000" style={{ width: '100%', padding: '10px', borderRadius: '6px', border: '1px solid #ddd' }} />
                            </div>

                            {targetType === 'PRO_UPGRADE' ? (
                                <div className="form-group" style={{ marginBottom: '25px' }}>
                                    <label style={{ display: 'block', marginBottom: '5px', fontWeight: 'bold' }}>대표 문서 (선택, 최대 2개)</label>
                                    <div style={{
                                        border: '2px dashed #ddd',
                                        padding: '20px',
                                        textAlign: 'center',
                                        borderRadius: '8px',
                                        backgroundColor: '#f9f9f9',
                                        cursor: 'pointer'
                                    }} onClick={() => document.getElementById('file-upload').click()}>
                                        <p style={{ margin: 0, color: '#666', fontSize: '0.9rem' }}>
                                            클릭하여 파일 업로드<br />
                                            <span style={{ fontSize: '0.8rem', color: '#999' }}>(PDF, DOCX 등 지원)</span>
                                        </p>
                                        <input
                                            id="file-upload"
                                            type="file"
                                            multiple
                                            accept=".pdf,.doc,.docx,.txt"
                                            onChange={handleFileChange}
                                            style={{ display: 'none' }}
                                        />
                                        {formData.files.length > 0 && (
                                            <div style={{ marginTop: '10px', textAlign: 'left' }}>
                                                {formData.files.map((f, i) => (
                                                    <div key={i} style={{ fontSize: '0.85rem', color: '#2563eb' }}>📄 {f.name}</div>
                                                ))}
                                            </div>
                                        )}
                                    </div>
                                </div>
                            ) : (
                                <div className="form-group" style={{ marginBottom: '25px' }}>
                                    <label style={{
                                        display: 'flex',
                                        alignItems: 'center',
                                        gap: '10px',
                                        cursor: 'pointer',
                                        padding: '15px',
                                        background: '#f8f9fa',
                                        borderRadius: '8px',
                                        border: '1px solid #eee'
                                    }}>
                                        <input
                                            type="checkbox"
                                            checked={consentChecked}
                                            onChange={e => setConsentChecked(e.target.checked)}
                                            style={{ width: '18px', height: '18px', accentColor: '#2563eb' }}
                                        />
                                        <span style={{ fontSize: '0.95rem', color: '#333' }}>
                                            원활한 상담을 위해 <strong>전문가의 유선 연락</strong>을 수신하는 것에 동의합니다.
                                        </span>
                                    </label>
                                </div>
                            )}

                            <div style={{ display: 'flex', gap: '10px' }}>
                                <button
                                    type="button"
                                    className="plan-btn"
                                    onClick={() => setView('plans')}
                                    style={{ background: '#eee', border: 'none', width: '30%' }}
                                >
                                    이전
                                </button>
                                <button
                                    type="submit"
                                    className="plan-btn primary"
                                    style={{ width: '70%', background: '#2563eb', color: 'white', border: 'none' }}
                                    disabled={loading || (targetType === 'MAX_CONSULTATION' && !consentChecked)}
                                >
                                    {loading ? '처리 중...' : '신청하기'}
                                </button>
                            </div>
                        </form>
                    </div>
                )}
            </div>
        </div>
    );
};

export default UpgradeModal;
