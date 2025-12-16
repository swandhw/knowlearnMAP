import { useEffect, useRef } from 'react';
import './AddSourceModal.css';

function AddSourceModal({ isOpen, onClose }) {
    const modalRef = useRef(null);

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

    const handleFileUpload = (e) => {
        console.log('File upload:', e.target.files);
    };

    const handleDragOver = (e) => {
        e.preventDefault();
    };

    const handleDrop = (e) => {
        e.preventDefault();
        console.log('Files dropped:', e.dataTransfer.files);
    };

    return (
        <div className="modal-overlay">
            <div className="modal-container" ref={modalRef}>
                <div className="modal-header">
                    <h2 className="modal-title">소스 추가</h2>
                    <button className="modal-close-btn" onClick={onClose}>
                        <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
                            <path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z" />
                        </svg>
                    </button>
                </div>

                <div className="modal-body">
                    <p className="modal-description">
                        소스를 추가하면 KNOWLEARN MAP이 가장 중요한 정보에 따라 응답을 제공합니다.
                        <br />
                        (예: 마케팅 계획, 수업 자료, 연구 노트, 회의 스크립트, 판매 문서 등)
                    </p>

                    {/* File Upload Area */}
                    <div
                        className="upload-area"
                        onDragOver={handleDragOver}
                        onDrop={handleDrop}
                    >
                        <div className="upload-icon">
                            <svg width="48" height="48" viewBox="0 0 48 48" fill="#1a73e8">
                                <path d="M24 16l-8 8h5v8h6v-8h5l-8-8zm-8 18v4h16v-4H16z" />
                            </svg>
                        </div>
                        <p className="upload-title">소스 업로드</p>
                        <p className="upload-subtitle">
                            업로드할 <button className="link-btn">파일 선택</button>하거나 드래그 앤 드롭하세요.
                        </p>
                        <input
                            type="file"
                            id="file-upload"
                            className="file-input"
                            onChange={handleFileUpload}
                            multiple
                            accept=".pdf,.txt,.md,.docx,.pptx,.xlsx,.csv,.html,.json"
                        />
                        <label htmlFor="file-upload" className="file-label">파일 선택</label>
                        <p className="upload-formats">
                            지원되는 파일 형식: PDF, txt, Markdown, 오디오(mp3), .docx, .avif, .bmp, .gif, .ico, .jp2, .png, .webp, .tif, .tiff, .heic, .heif, .jpeg, .jpg, .jpe
                        </p>
                    </div>

                    {/* Connection Options */}
                    <div className="connection-options">
                        {/* Google Workspace */}
                        <div className="option-section">
                            <div className="option-header">
                                <svg width="20" height="20" viewBox="0 0 20 20">
                                    <path fill="#4285F4" d="M19.6 10.23c0-.82-.1-1.42-.25-2.05H10v3.72h5.5c-.15.96-.74 2.31-2.04 3.22v2.45h3.16c1.89-1.73 2.98-4.3 2.98-7.34z" />
                                    <path fill="#34A853" d="M13.46 15.13c-.83.59-1.96 1-3.46 1-2.64 0-4.88-1.74-5.68-4.15H1.07v2.52C2.72 17.75 6.09 20 10 20c2.7 0 4.96-.89 6.62-2.42l-3.16-2.45z" />
                                    <path fill="#FBBC05" d="M3.99 10c0-.69.12-1.35.32-1.97V5.51H1.07A9.973 9.973 0 000 10c0 1.61.39 3.14 1.07 4.49l3.24-2.52c-.2-.62-.32-1.28-.32-1.97z" />
                                    <path fill="#EA4335" d="M10 3.88c1.88 0 3.13.81 3.85 1.48l2.84-2.76C14.96.99 12.7 0 10 0 6.09 0 2.72 2.25 1.07 5.51l3.24 2.52C5.12 5.62 7.36 3.88 10 3.88z" />
                                </svg>
                                <span>Google Workspace</span>
                            </div>
                            <button className="option-btn">
                                <svg width="20" height="20" viewBox="0 0 20 20" fill="#4285F4">
                                    <path d="M11.5 6L7 10.5l4.5 4.5L13 13.5l-3-3 3-3z" />
                                </svg>
                                Google Drive
                            </button>
                        </div>

                        {/* Links */}
                        <div className="option-section">
                            <div className="option-header">
                                <svg width="20" height="20" viewBox="0 0 20 20" fill="#5f6368">
                                    <path d="M3.9 12c0-1.71 1.39-3.1 3.1-3.1h4V7H7c-2.76 0-5 2.24-5 5s2.24 5 5 5h4v-1.9H7c-1.71 0-3.1-1.39-3.1-3.1zM8 13h8v-2H8v2zm5-6h4c2.76 0 5 2.24 5 5s-2.24 5-5 5h-4v-1.9h4c1.71 0 3.1-1.39 3.1-3.1 0-1.71-1.39-3.1-3.1-3.1h-4V7z" />
                                </svg>
                                <span>링크</span>
                            </div>
                            <button className="option-btn">
                                <svg width="20" height="20" viewBox="0 0 20 20" fill="#1a73e8">
                                    <path d="M3.9 12c0-1.71 1.39-3.1 3.1-3.1h4V7H7c-2.76 0-5 2.24-5 5s2.24 5 5 5h4v-1.9H7c-1.71 0-3.1-1.39-3.1-3.1zM8 13h8v-2H8v2z" />
                                </svg>
                                웹사이트
                            </button>
                            <button className="option-btn">
                                <svg width="20" height="20" viewBox="0 0 20 20" fill="#FF0000">
                                    <path d="M10 0C4.48 0 0 4.48 0 10s4.48 10 10 10 10-4.48 10-10S15.52 0 10 0zm3.61 6.34c1.07 0 1.93.86 1.93 1.93 0 .27-.05.52-.15.75.58.31 1.15.68 1.69 1.11.05-.42.11-.84.11-1.27 0-2.98-2.42-5.4-5.4-5.4-.44 0-.86.06-1.27.11.42.54.8 1.11 1.11 1.69.23-.1.48-.15.75-.15.18 0 .36.03.53.08zm-3.61 9.3c-2.98 0-5.4-2.42-5.4-5.4 0-.18.03-.36.08-.53.05-.18.03-.36-.08-.53-.54-.42-1.11-.8-1.69-1.11-.42.54-.68 1.15-.91 1.77.27 3.51 3.2 6.25 6.71 6.52.62-.23 1.23-.49 1.77-.91-.17-.11-.35-.13-.53-.08-.17.05-.35.08-.53.08z" />
                                </svg>
                                YouTube
                            </button>
                        </div>

                        {/* Text Paste */}
                        <div className="option-section">
                            <div className="option-header">
                                <svg width="20" height="20" viewBox="0 0 20 20" fill="#5f6368">
                                    <path d="M14 2H6c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm-1 9H7V9h6v2z" />
                                </svg>
                                <span>텍스트 붙여넣기</span>
                            </div>
                            <button className="option-btn">
                                <svg width="20" height="20" viewBox="0 0 20 20" fill="#7c4dff">
                                    <path d="M14 2H6c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm-1 9H7V9h6v2z" />
                                </svg>
                                복사한 텍스트
                            </button>
                        </div>
                    </div>

                    {/* Source Count */}
                    <div className="source-count">
                        <svg width="16" height="16" viewBox="0 0 16 16" fill="#5f6368">
                            <path d="M8 2C4.7 2 2 4.7 2 8s2.7 6 6 6 6-2.7 6-6-2.7-6-6-6zm1 9H7V7h2v4z" />
                        </svg>
                        <span>소스 원도</span>
                        <span className="count">0/300</span>
                    </div>
                </div>
            </div>
        </div>
    );
}

export default AddSourceModal;
