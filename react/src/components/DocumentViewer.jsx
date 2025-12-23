import React, { useState, useEffect } from 'react';
import { documentApi } from '../services/documentApi';
import './DocumentViewer.css';

function DocumentViewer({ document, onClose }) {
    const [pages, setPages] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        const fetchPages = async () => {
            try {
                setLoading(true);
                setError(null);
                const pagesData = await documentApi.getPages(document.id);
                setPages(pagesData);
            } catch (err) {
                console.error('페이지 로드 실패:', err);
                setError('페이지를 불러오는 데 실패했습니다.');
            } finally {
                setLoading(false);
            }
        };

        if (document?.id) {
            fetchPages();
        }
    }, [document?.id]);

    return (
        <div className="document-viewer">
            {/* 헤더 */}
            <div className="viewer-header">
                <h2 className="viewer-title">{document.filename}</h2>
                <button
                    className="close-viewer-btn"
                    onClick={onClose}
                    aria-label="닫기"
                >
                    ✕
                </button>
            </div>

            {/* 콘텐츠 */}
            <div className="viewer-content">
                {loading ? (
                    <div className="viewer-loading">
                        <div className="spinner"></div>
                        <p>내용을 불러오는 중...</p>
                    </div>
                ) : error ? (
                    <div className="viewer-error">
                        <p>{error}</p>
                        <button onClick={onClose}>돌아가기</button>
                    </div>
                ) : pages.length === 0 ? (
                    <div className="viewer-empty">
                        <p>표시할 페이지가 없습니다.</p>
                    </div>
                ) : (
                    <div className="pages-container">
                        {pages.map((page) => (
                            <div key={page.id} className="page-section">
                                <div className="page-header">
                                    <span className="page-number">Page {page.pageNumber}</span>
                                    {page.wordCount > 0 && (
                                        <span className="word-count">{page.wordCount} 단어</span>
                                    )}
                                </div>
                                <div className="page-content">
                                    {page.content}
                                </div>
                            </div>
                        ))}
                    </div>
                )}
            </div>
        </div>
    );
}

export default DocumentViewer;
