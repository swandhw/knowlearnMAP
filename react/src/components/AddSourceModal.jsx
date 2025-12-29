import { useState, useEffect, useRef } from 'react';
import './AddSourceModal.css';
import { API_URL } from '../config/api';

function AddSourceModal({ isOpen, onClose, workspaceId }) {
    const [currentView, setCurrentView] = useState('main'); // main, website, youtube, text, drive
    const [inputValue, setInputValue] = useState('');
    const [uploading, setUploading] = useState(false);
    const [selectedFile, setSelectedFile] = useState(null);
    const modalRef = useRef(null);

    // Demo mode handler
    const handleDemoClick = (e) => {
        e.stopPropagation();
        alert('데모에서는 지원하지 않습니다.');
    };

    // File upload handler
    const handleFileUpload = async (event) => {
        const file = event.target.files[0];

        if (!file) return;

        // PDF만 허용 (데모)
        if (!file.name.toLowerCase().endsWith('.pdf')) {
            alert('데모에서는 PDF 파일만 업로드 가능합니다.');
            event.target.value = ''; // Reset input
            return;
        }

        if (!workspaceId) {
            alert('워크스페이스 정보를 찾을 수 없습니다.');
            return;
        }

        setUploading(true);
        setSelectedFile(file);

        try {
            const formData = new FormData();
            formData.append('file', file);
            formData.append('workspaceId', workspaceId);

            const response = await fetch(`${API_URL}/api/documents/upload`, {
                method: 'POST',
                credentials: 'include',
                body: formData,
            });

            const responseText = await response.text();
            let result;
            try {
                result = responseText ? JSON.parse(responseText) : {};
            } catch (e) {
                console.warn('Response was not JSON:', responseText);
                result = { message: responseText };
            }

            if (!response.ok) {
                throw new Error(result.message || '파일 업로드에 실패했습니다.');
            }

            console.log('업로드 성공:', result);

            alert('파일이 성공적으로 업로드되었습니다!');

            // 모달 닫기 및 상태 초기화
            event.target.value = ''; // Reset file input
            setSelectedFile(null);
            onClose();

            // 페이지 새로고침하여 소스 목록 갱신
            window.location.reload();

        } catch (error) {
            console.error('업로드 오류:', error);
            alert(error.message || '파일 업로드 중 오류가 발생했습니다.');
            event.target.value = ''; // Reset input on error
        } finally {
            setUploading(false);
            setSelectedFile(null);
        }
    };

    // Reset view when opening
    useEffect(() => {
        if (isOpen) {
            setCurrentView('main');
            setInputValue('');
        }
    }, [isOpen]);

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
                if (currentView === 'main') {
                    onClose();
                } else {
                    setCurrentView('main');
                }
            }
        };

        if (isOpen) {
            document.addEventListener('keydown', handleEscape);
        }

        return () => {
            document.removeEventListener('keydown', handleEscape);
        };
    }, [isOpen, onClose, currentView]);

    if (!isOpen) return null;

    const handleBack = () => {
        setCurrentView('main');
        setInputValue('');
    };

    const handleInsert = () => {
        // Mock insert functionality
        console.log(`Inserting from ${currentView}: ${inputValue}`);
        onClose();
    };

    const renderHeader = (title) => (
        <div className="modal-header with-back">
            <div className="header-left">
                {currentView !== 'main' && (
                    <button className="back-btn" onClick={handleBack}>
                        <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
                            <path d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z" />
                        </svg>
                    </button>
                )}
                <h2 className="modal-title">{title}</h2>
            </div>
            <button className="modal-close-btn" onClick={onClose}>
                <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z" />
                </svg>
            </button>
        </div>
    );

    const renderMainView = () => (
        <>
            <div className="modal-header">
                <h2 className="modal-title">소스 추가</h2>
                <button className="modal-close-btn" onClick={onClose}>
                    <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
                        <path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z" />
                    </svg>
                </button>
            </div>
            <div className="modal-body main-view">
                <p className="modal-description">
                    소스를 추가하면 KNOWLEARN MAP이 가장 중요한 정보에 따라 응답을 제공합니다.
                    <br />
                    (예: 마케팅 계획, 수업 자료, 연구 노트, 회의 스크립트, 판매 문서 등)
                </p>

                <div className="upload-area">
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
                        accept=".pdf"
                        onChange={handleFileUpload}
                        disabled={uploading}
                    />
                    <label htmlFor="file-upload" className="file-label">
                        {uploading ? '업로드 중...' : '파일 선택'}
                    </label>
                    <p className="upload-formats">
                        지원되는 파일 형식: PDF, <span className="unsupported-format">txt, Markdown, 오디오(mp3), .docx, .avif, .bmp, .gif, .ico, .jp2, .png, .webp, .tif, .tiff, .heic, .heif, .jpeg, .jpg, .jpe</span>
                    </p>
                    <p className="demo-notice">※ 데모에서는 PDF만 지원됩니다.</p>
                </div>

                <div className="connection-options">
                    <div className="option-section demo-disabled">
                        <div className="option-header">
                            <svg width="20" height="20" viewBox="0 0 20 20">
                                <path fill="#9e9e9e" d="M19.6 10.23c0-.82-.1-1.42-.25-2.05H10v3.72h5.5c-.15.96-.74 2.31-2.04 3.22v2.45h3.16c1.89-1.73 2.98-4.3 2.98-7.34z" />
                                <path fill="#9e9e9e" d="M13.46 15.13c-.83.59-1.96 1-3.46 1-2.64 0-4.88-1.74-5.68-4.15H1.07v2.52C2.72 17.75 6.09 20 10 20c2.7 0 4.96-.89 6.62-2.42l-3.16-2.45z" />
                                <path fill="#9e9e9e" d="M3.99 10c0-.69.12-1.35.32-1.97V5.51H1.07A9.973 9.973 0 000 10c0 1.61.39 3.14 1.07 4.49l3.24-2.52c-.2-.62-.32-1.28-.32-1.97z" />
                                <path fill="#9e9e9e" d="M10 3.88c1.88 0 3.13.81 3.85 1.48l2.84-2.76C14.96.99 12.7 0 10 0 6.09 0 2.72 2.25 1.07 5.51l3.24 2.52C5.12 5.62 7.36 3.88 10 3.88z" />
                            </svg>
                            <span>Google Workspace</span>
                        </div>
                        <button className="option-btn demo-disabled-btn" onClick={handleDemoClick}>
                            <svg width="20" height="20" viewBox="0 0 20 20" fill="#9e9e9e">
                                <path d="M11.5 6L7 10.5l4.5 4.5L13 13.5l-3-3 3-3z" />
                            </svg>
                            Google Drive
                        </button>
                    </div>

                    <div className="option-section demo-disabled">
                        <div className="option-header">
                            <svg width="20" height="20" viewBox="0 0 20 20" fill="#9e9e9e">
                                <path d="M3.9 12c0-1.71 1.39-3.1 3.1-3.1h4V7H7c-2.76 0-5 2.24-5 5s2.24 5 5 5h4v-1.9H7c-1.71 0-3.1-1.39-3.1-3.1zM8 13h8v-2H8v2zm5-6h4c2.76 0 5 2.24 5 5s-2.24 5-5 5h-4v-1.9h4c1.71 0 3.1-1.39 3.1-3.1 0-1.71-1.39-3.1-3.1-3.1h-4V7z" />
                            </svg>
                            <span>링크</span>
                        </div>
                        <button className="option-btn demo-disabled-btn" onClick={handleDemoClick}>
                            <svg width="20" height="20" viewBox="0 0 20 20" fill="#9e9e9e">
                                <path d="M3.9 12c0-1.71 1.39-3.1 3.1-3.1h4V7H7c-2.76 0-5 2.24-5 5s2.24 5 5 5h4v-1.9H7c-1.71 0-3.1-1.39-3.1-3.1zM8 13h8v-2H8v2z" />
                            </svg>
                            웹사이트
                        </button>
                        <button className="option-btn demo-disabled-btn" onClick={handleDemoClick}>
                            <svg width="20" height="20" viewBox="0 0 20 20" fill="#9e9e9e">
                                <path d="M10 0C4.48 0 0 4.48 0 10s4.48 10 10 10 10-4.48 10-10S15.52 0 10 0zm3.61 6.34c1.07 0 1.93.86 1.93 1.93 0 .27-.05.52-.15.75.58.31 1.15.68 1.69 1.11.05-.42.11-.84.11-1.27 0-2.98-2.42-5.4-5.4-5.4-.44 0-.86.06-1.27.11.42.54.8 1.11 1.11 1.69.23-.1.48-.15.75-.15.18 0 .36.03.53.08zm-3.61 9.3c-2.98 0-5.4-2.42-5.4-5.4 0-.18.03-.36.08-.53.05-.18.03-.36-.08-.53-.54-.42-1.11-.8-1.69-1.11-.42.54-.68 1.15-.91 1.77.27 3.51 3.2 6.25 6.71 6.52.62-.23 1.23-.49 1.77-.91-.17-.11-.35-.13-.53-.08-.17.05-.35.08-.53.08z" />
                            </svg>
                            YouTube
                        </button>
                    </div>

                    <div className="option-section demo-disabled">
                        <div className="option-header">
                            <svg width="20" height="20" viewBox="0 0 20 20" fill="#9e9e9e">
                                <path d="M14 2H6c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm-1 9H7V9h6v2z" />
                            </svg>
                            <span>텍스트 붙여넣기</span>
                        </div>
                        <button className="option-btn demo-disabled-btn" onClick={handleDemoClick}>
                            <svg width="20" height="20" viewBox="0 0 20 20" fill="#9e9e9e">
                                <path d="M14 2H6c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm-1 9H7V9h6v2z" />
                            </svg>
                            복사한 텍스트
                        </button>
                    </div>
                </div>
            </div>
        </>
    );

    const renderInputView = (title, placeholder, description, notes) => (
        <>
            {renderHeader(title)}
            <div className="modal-body sub-view">
                <p className="input-description">{description}</p>
                <div className="input-container">
                    <div className="input-wrapper">
                        {currentView === 'website' && (
                            <div className="input-icon website">
                                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                    <rect x="3" y="3" width="18" height="18" rx="2" ry="2" />
                                    <line x1="3" y1="9" x2="21" y2="9" />
                                </svg>
                            </div>
                        )}
                        {currentView === 'youtube' && (
                            <div className="input-icon youtube">
                                <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
                                    <path d="M19.615 3.184c-3.604-.246-11.631-.245-15.23 0-3.897.266-4.356 2.62-4.385 8.816.029 6.185.484 8.549 4.385 8.816 3.6.245 11.626.246 15.23 0 3.897-.266 4.356-2.62 4.385-8.816-.029-6.185-.484-8.549-4.385-8.816zm-10.615 12.816v-8l8 3.993-8 4.007z" />
                                </svg>
                            </div>
                        )}
                        <input
                            type="text"
                            placeholder={placeholder}
                            className="text-input"
                            value={inputValue}
                            onChange={(e) => setInputValue(e.target.value)}
                            autoFocus
                        />
                    </div>
                    {notes && <div className="notes-list">{notes}</div>}
                </div>
                <div className="footer-actions">
                    <button className="insert-btn" disabled={!inputValue.trim()} onClick={handleInsert}>
                        삽입
                    </button>
                </div>
            </div>
        </>
    );

    const renderTextView = () => (
        <>
            {renderHeader('복사한 텍스트 붙여넣기')}
            <div className="modal-body sub-view">
                <p className="input-description">
                    KNOWLEARN MAP에 소스로 업로드할 복사한 텍스트를 아래에 붙여넣으세요.
                </p>
                <div className="textarea-container">
                    <textarea
                        className="large-textarea"
                        placeholder="여기에 텍스트를 붙여넣으세요.*"
                        value={inputValue}
                        onChange={(e) => setInputValue(e.target.value)}
                        autoFocus
                    />
                </div>
                <div className="footer-actions">
                    <button className="insert-btn" disabled={!inputValue.trim()} onClick={handleInsert}>
                        삽입
                    </button>
                </div>
            </div>
        </>
    );

    const renderDriveView = () => (
        <>
            <div className="drive-header-top">
                <div className="drive-search-bar">
                    <div className="drive-logo">
                        <svg width="24" height="24" viewBox="0 0 87.3 78" className="drive-icon-svg">
                            <path d="M6.6 66.85l16.1-27.9 16.1-27.9H71L54.9 38.95 38.8 66.85H6.6z" fill="#0066DA" />
                            <path d="M43.65 25l-16.1 27.9-16.1 27.9h64.3l-16.1-27.9L43.65 25z" fill="#00AC47" />
                            <path d="M79.75 66.85l-16.1-27.9-16.1-27.9H15.1l16.1 27.9 16.1 27.9h32.45z" fill="#EA4335" />
                            <path d="M43.65 25L27.55 52.9 11.45 80.8h64.3l-16.1-27.9-16.1-27.9z" fill="#FFBA00" />
                        </svg>
                        <span>항목 선택</span>
                    </div>
                    <input type="text" placeholder="Drive에서 검색하거나 URL 붙여넣기" className="drive-search-input" />
                    <button className="drive-close-btn" onClick={onClose}>×</button>
                </div>
                <div className="drive-tabs">
                    <button className="drive-tab active">최근</button>
                    <button className="drive-tab">내 드라이브</button>
                    <button className="drive-tab">공유 문서함</button>
                    <button className="drive-tab">중요 문서함</button>
                    <button className="drive-tab">컴퓨터</button>
                </div>
            </div>

            <div className="drive-body">
                <div className="drive-list-header">
                    <span>최근 문서함</span>
                    <button className="drive-view-toggle">
                        <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M4 11h5V5H4v6zm0 7h5v-6H4v6zm6 0h5v-6h-5v6zm6 0h5v-6h-5v6zm-6-7h5V5h-5v6zm6-6v6h5V5h-5z" /></svg>
                    </button>
                </div>
                <div className="drive-grid">
                    <div className="drive-item">
                        <div className="drive-preview sheet"></div>
                        <div className="drive-name">
                            <span className="file-icon sheet"></span>
                            11월 23-24일 부의금 ...
                        </div>
                    </div>
                    <div className="drive-item">
                        <div className="drive-preview doc"></div>
                        <div className="drive-name">
                            <span className="file-icon doc"></span>
                            JOY_정리노트
                        </div>
                    </div>
                </div>
            </div>

            <div className="drive-footer">
                <button className="drive-select-btn" disabled>선택</button>
            </div>
        </>
    );

    return (
        <div className={`modal-overlay ${currentView === 'drive' ? 'drive-mode' : ''}`}>
            <div className={`modal-container ${currentView}`} ref={modalRef}>
                {currentView === 'main' && renderMainView()}
                {currentView === 'website' && renderInputView(
                    '웹사이트 URL',
                    'URL 붙여넣기*',
                    'KNOWLEARN MAP에 소스로 업로드할 웹 URL을 아래에 붙여넣으세요.',
                    <div className="notes">
                        <h4>참고</h4>
                        <ul>
                            <li>여러 URL을 추가하려면 공백이나 줄 바꿈으로 구분하세요.</li>
                            <li>웹사이트에 표시되는 텍스트만 가져옵니다.</li>
                            <li>유료 기사는 지원되지 않습니다.</li>
                        </ul>
                    </div>
                )}
                {currentView === 'youtube' && renderInputView(
                    'YouTube URL',
                    'YouTube URL 붙여넣기*',
                    'KNOWLEARN MAP에 소스로 업로드할 YouTube URL을 아래에 붙여넣으세요.',
                    <div className="notes">
                        <h4>참고</h4>
                        <ul>
                            <li>현재 텍스트 스크립트만 가져옵니다.</li>
                            <li>공개 YouTube 동영상만 지원됩니다.</li>
                            <li>최근에 업로드된 동영상은 가져올 수 없습니다.</li>
                            <li>업로드에 실패하는 경우 자세히 알아보기 를 통해 일반적인 이유를 확인하세요.</li>
                        </ul>
                    </div>
                )}
                {currentView === 'text' && renderTextView()}
                {currentView === 'drive' && renderDriveView()}
            </div>
        </div>
    );
}

export default AddSourceModal;
