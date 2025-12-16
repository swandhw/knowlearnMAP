import { useEffect, useRef, useState } from 'react';
import './SlideCreationModal.css';

function SlideCreationModal({ isOpen, onClose }) {
    const modalRef = useRef(null);
    const [selectedFormat, setSelectedFormat] = useState('detailed');
    const [selectedLength, setSelectedLength] = useState('default');

    // Close modal when clicking outside
    useEffect(() => {
        const handleClickOutside = (event) => {
            if (modalRef.current && !modalRef.current.contains(event.target)) {
                onClose();
            }
        };

        if (isOpen) {
            document.addEventListener('mousedown', handleClickOutside);
            document.body.style.overflow = 'hidden';
        }

        return () => {
            document.removeEventListener('mousedown', handleClickOutside);
            document.body.style.overflow = 'unset';
        };
    }, [isOpen, onClose]);

    // Close on Escape key
    useEffect(() => {
        const handleEscape = (event) => {
            if (event.key === 'Escape') {
                onClose();
            }
        };

        if (isOpen) {
            document.addEventListener('keydown', handleEscape);
        }

        return () => {
            document.removeEventListener('keydown', handleEscape);
        };
    }, [isOpen, onClose]);

    if (!isOpen) return null;

    const handleGenerate = () => {
        console.log('Generating slide with:', { selectedFormat, selectedLength });
        // Handle slide generation
    };

    return (
        <div className="modal-overlay">
            <div className="modal-container slide-modal" ref={modalRef}>
                <div className="modal-header">
                    <div className="modal-header-content">
                        <span className="modal-icon">📊</span>
                        <h2 className="modal-title">슬라이드 자료 맞춤설정</h2>
                    </div>
                    <button className="modal-close-btn" onClick={onClose}>
                        <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
                            <path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z" />
                        </svg>
                    </button>
                </div>

                <div className="modal-body">
                    {/* Format Section */}
                    <div className="form-section">
                        <label className="form-label">형식</label>
                        <div className="format-options">
                            <button
                                className={`format-option ${selectedFormat === 'detailed' ? 'selected' : ''}`}
                                onClick={() => setSelectedFormat('detailed')}
                            >
                                <div className="option-content">
                                    <h4 className="option-title">자세한 자료</h4>
                                    <p className="option-description">
                                        전체 텍스트와 세부정보가 포함된 포괄적인 자료로, 이메일로 보내거나 단독으로 읽기에 적합합니다.
                                    </p>
                                </div>
                                {selectedFormat === 'detailed' && (
                                    <svg className="check-icon" width="20" height="20" viewBox="0 0 20 20" fill="currentColor">
                                        <path d="M7.5 14.5l-5-5 1.41-1.41L7.5 11.67l8.59-8.59L17.5 4.5z" />
                                    </svg>
                                )}
                            </button>

                            <button
                                className={`format-option ${selectedFormat === 'presenter' ? 'selected' : ''}`}
                                onClick={() => setSelectedFormat('presenter')}
                            >
                                <div className="option-content">
                                    <h4 className="option-title">발표자 슬라이드</h4>
                                    <p className="option-description">
                                        발표하는 동안 도움이 될 핵심 내용을 담은 깔끔하고 시각적인 슬라이드입니다.
                                    </p>
                                </div>
                                {selectedFormat === 'presenter' && (
                                    <svg className="check-icon" width="20" height="20" viewBox="0 0 20 20" fill="currentColor">
                                        <path d="M7.5 14.5l-5-5 1.41-1.41L7.5 11.67l8.59-8.59L17.5 4.5z" />
                                    </svg>
                                )}
                            </button>
                        </div>
                    </div>

                    {/* Language Selection */}
                    <div className="form-section">
                        <label className="form-label">언어 선택</label>
                        <select className="form-select">
                            <option value="ko">한국어</option>
                            <option value="en">English</option>
                            <option value="ja">日本語</option>
                            <option value="zh">中文</option>
                        </select>
                    </div>

                    {/* Length Selection */}
                    <div className="form-section">
                        <label className="form-label">길이</label>
                        <div className="length-options">
                            <button
                                className={`length-btn ${selectedLength === 'short' ? 'selected' : ''}`}
                                onClick={() => setSelectedLength('short')}
                            >
                                짧게
                            </button>
                            <button
                                className={`length-btn ${selectedLength === 'default' ? 'selected' : ''}`}
                                onClick={() => setSelectedLength('default')}
                            >
                                기본값
                            </button>
                        </div>
                    </div>

                    {/* Custom Description */}
                    <div className="form-section">
                        <label className="form-label">만들려는 슬라이드 자료에 대한 설명</label>
                        <textarea
                            className="form-textarea"
                            placeholder='간략한 개요를 추가하거나 청중, 스타일, 강조할 점에 대한 가이드 제공: "단계별 안내에 중점을 둔 대학하고 재미있는 스타일의 초보자용 자료 만들어 줘."'
                            rows="2"
                        ></textarea>
                    </div>

                    {/* Generate Button */}
                    <div className="form-actions">
                        <button className="btn-generate" onClick={handleGenerate}>
                            생성
                        </button>
                    </div>
                </div>
            </div>
        </div>
    );
}

export default SlideCreationModal;
