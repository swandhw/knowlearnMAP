import React from 'react';
import './UpgradeModal.css';

const UpgradeModal = ({ isOpen, onClose }) => {
    if (!isOpen) return null;

    return (
        <div className="upgrade-modal-overlay" onClick={onClose}>
            <div className="upgrade-modal-content" onClick={e => e.stopPropagation()}>
                <button className="upgrade-modal-close" onClick={onClose}>&times;</button>

                <div className="upgrade-header">
                    <h2 className="upgrade-title">함께 성장하는 요금제</h2>

                    <div className="upgrade-toggle-container">
                        <div className="upgrade-toggle">
                            <button className="toggle-btn active">개인</button>
                            <button className="toggle-btn">팀 & 엔터프라이즈</button>
                        </div>
                    </div>
                </div>

                <div className="plans-container">
                    {/* Free Plan */}
                    <div className="plan-card">
                        <div className="plan-icon">
                            <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"></path>
                            </svg>
                        </div>
                        <h3 className="plan-name">Free</h3>
                        <p className="plan-desc">개인 학습 및 체험용</p>
                        <div className="plan-price">USD 0</div>
                        <p className="plan-period">/월 (평생 무료)</p>

                        <div className="promo-box">
                            현재 Free 플랜 이용 중입니다.<br />
                            부담 없이 시작해보세요.
                        </div>

                        <button className="plan-btn primary" disabled style={{ opacity: 0.6, cursor: 'default' }}>이용 중</button>

                        <ul className="plan-features-list">
                            <li>기본 문서 업로드 10개</li>
                            <li>기본 챗봇 대화</li>
                            <li>지식 그래프 조회</li>
                            <li>개인 메모장 기능</li>
                        </ul>
                    </div>

                    {/* Pro Plan */}
                    <div className="plan-card highlight">
                        <div className="plan-icon">
                            <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"></polygon>
                            </svg>
                        </div>
                        <h3 className="plan-name">Pro</h3>
                        <p className="plan-desc">리서치, 코딩, 정리</p>
                        <div className="plan-price">USD 19 <span style={{ fontSize: '0.9rem', fontWeight: 'normal' }}>(부가세 포함)</span></div>
                        <p className="plan-period">/월 (연간 청구)</p>

                        <div className="promo-box">
                            월간 요금제를 사용 중입니다.<br />
                            연간 결제 시 17% 할인됩니다.
                        </div>

                        <button className="plan-btn primary">Pro 연간 플랜 구독하기</button>

                        <ul className="plan-features-list">
                            <li><strong>Free의 모든 기능 및:</strong></li>
                            <li>Free보다 많은 사용량*</li>
                            <li>더 많은 Claude 모델에 액세스</li>
                            <li>채팅을 정리할 수 있는 무제한 프로젝트</li>
                            <li>심화 리서치 도구 잠금 해제</li>
                            <li>Google Workspace 연동: 이메일, 캘린더, 문서</li>
                        </ul>
                    </div>

                    {/* Max Plan */}
                    <div className="plan-card">
                        <div className="plan-icon">
                            <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                <path d="M12 2a10 10 0 1 0 10 10A10 10 0 0 0 12 2zm0 18a8 8 0 1 1 8-8 8 8 0 0 1-8 8z"></path>
                                <path d="M12 6v6l4 2"></path>
                            </svg>
                        </div>
                        <h3 className="plan-name">Max</h3>
                        <p className="plan-desc">높은 한도, 우선 액세스</p>
                        <div className="plan-price">USD 110 <span style={{ fontSize: '0.9rem', fontWeight: 'normal' }}>(부가세 포함)부터</span></div>
                        <p className="plan-period">월 단위 청구</p>

                        <div className="promo-box" style={{ visibility: 'hidden' }}>
                            Space Holder
                        </div>

                        <button className="plan-btn accent">Max 플랜 구독하기</button>

                        <ul className="plan-features-list">
                            <li><strong>Pro의 모든 기능에 다음 기능 포함:</strong></li>
                            <li>Pro보다 5배 또는 20배 더 많은 사용량 선택*</li>
                            <li>모든 작업에 대한 더 높은 출력 제한</li>
                            <li>Claude 고급 기능 조기 액세스</li>
                            <li>트래픽이 많은 시간대의 우선 액세스</li>
                            <li>Claude Code 포함</li>
                        </ul>
                    </div>
                </div>

                <div className="disclaimer">
                    *사용 제한이 적용됩니다. 표시된 가격에는 해당 세금이 포함되어 있습니다.
                </div>
            </div>
        </div>
    );
};

export default UpgradeModal;
