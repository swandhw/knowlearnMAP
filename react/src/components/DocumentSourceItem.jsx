import React, { useState, useRef, useEffect } from 'react';
import './DocumentSourceItem.css';

function DocumentSourceItem({
    document,
    progress,
    onSelect,
    isChecked,
    onCheckChange,
    onRename,
    onDelete
}) {
    const [menuOpen, setMenuOpen] = useState(false);
    const menuRef = useRef(null);

    const getStatusIcon = () => {
        const status = progress?.status || document.pipelineStatus;
        switch (status) {
            case 'COMPLETED':
                return '✓';
            case 'PROCESSING':
                return '⟳';
            case 'FAILED':
                return '✗';
            case 'PENDING':
            default:
                return '⋯';
        }
    };

    const getStatusColor = () => {
        const status = progress?.status || document.pipelineStatus;
        switch (status) {
            case 'COMPLETED':
                return '#4caf50';
            case 'PROCESSING':
                return '#2196f3';
            case 'FAILED':
                return '#f44336';
            case 'PENDING':
            default:
                return '#9e9e9e';
        }
    };

    const getStatusText = () => {
        const status = progress?.status || document.pipelineStatus;
        switch (status) {
            case 'COMPLETED':
                return '완료';
            case 'PROCESSING':
                return progress?.currentStage || '처리 중';
            case 'FAILED':
                return '실패';
            case 'PENDING':
            default:
                return '대기 중';
        }
    };

    const progressValue = progress?.progress || 0;
    const isProcessing = (progress?.status || document.pipelineStatus) === 'PROCESSING';
    const isRotating = getStatusIcon() === '⟳';

    // 메뉴 외부 클릭 감지
    useEffect(() => {
        const handleClickOutside = (event) => {
            if (menuRef.current && !menuRef.current.contains(event.target)) {
                setMenuOpen(false);
            }
        };

        if (menuOpen) {
            window.document.addEventListener('mousedown', handleClickOutside);
        }

        return () => {
            window.document.removeEventListener('mousedown', handleClickOutside);
        };
    }, [menuOpen]);

    const handleCheckboxClick = (e) => {
        e.stopPropagation();
        onCheckChange(document.id);
    };

    const handleMenuClick = (e) => {
        e.stopPropagation();
        setMenuOpen(!menuOpen);
    };

    const handleMenuItemClick = (action) => {
        setMenuOpen(false);
        if (action === 'rename') {
            onRename(document);
        } else if (action === 'delete') {
            onDelete(document);
        }
    };

    return (
        <div className="document-source-item">
            {/* 체크박스 */}
            <div className="document-checkbox" onClick={handleCheckboxClick}>
                <input
                    type="checkbox"
                    checked={isChecked}
                    onChange={() => { }}
                    onClick={handleCheckboxClick}
                />
            </div>

            {/* 문서 정보 (클릭 시 상세 보기) */}
            <div className="document-content" onClick={onSelect}>
                <div className="source-header">
                    <span className="source-icon">📄</span>
                    <div className="source-info">
                        <div className="source-name" title={document.filename}>
                            {document.filename}
                        </div>
                        <div className="source-meta">
                            <span
                                className="status-badge"
                                style={{ color: getStatusColor() }}
                            >
                                <span className={`status-icon ${isRotating ? 'rotating' : ''}`}>
                                    {getStatusIcon()}
                                </span>
                                {getStatusText()}
                            </span>
                            {document.pageCount > 0 && (
                                <span className="page-count">
                                    {document.pageCount} 페이지
                                </span>
                            )}
                        </div>
                    </div>
                </div>

                {isProcessing && (
                    <div className="progress-container">
                        <div className="progress-bar">
                            <div
                                className="progress-fill"
                                style={{
                                    width: `${progressValue}%`,
                                    backgroundColor: getStatusColor()
                                }}
                            />
                        </div>
                        <span className="progress-text">{progressValue}%</span>
                    </div>
                )}
            </div>

            {/* 메뉴 버튼 */}
            <div className="document-menu" ref={menuRef}>
                <button
                    className="menu-trigger"
                    onClick={handleMenuClick}
                    aria-label="메뉴"
                >
                    ⋮
                </button>

                {menuOpen && (
                    <div className="menu-dropdown">
                        <button
                            className="menu-item"
                            onClick={() => handleMenuItemClick('rename')}
                        >
                            제목수정
                        </button>
                        <button
                            className="menu-item delete"
                            onClick={() => handleMenuItemClick('delete')}
                        >
                            삭제
                        </button>
                    </div>
                )}
            </div>
        </div>
    );
}

export default DocumentSourceItem;
