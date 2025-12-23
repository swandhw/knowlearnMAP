import React from 'react';
import './DocumentSourceItem.css';

function DocumentSourceItem({ document, progress, onSelect }) {
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

    return (
        <div className="document-source-item" onClick={onSelect}>
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
    );
}

export default DocumentSourceItem;
