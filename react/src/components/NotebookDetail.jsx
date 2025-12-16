import { useState } from 'react';
import './NotebookDetail.css';
import AddSourceModal from './AddSourceModal';
import ReportGenerationModal from './ReportGenerationModal';
import SlideCreationModal from './SlideCreationModal';

function NotebookDetail({ notebook, onBack }) {
    const [selectedSources, setSelectedSources] = useState([]);
    const [leftSidebarOpen, setLeftSidebarOpen] = useState(true);
    const [rightPanelOpen, setRightPanelOpen] = useState(true);
    const [isAddSourceModalOpen, setIsAddSourceModalOpen] = useState(false);
    const [isReportModalOpen, setIsReportModalOpen] = useState(false);
    const [isSlideModalOpen, setIsSlideModalOpen] = useState(false);

    const mockSources = [
        { id: 1, name: 'cls_biz_meta.md', type: 'markdown', icon: '📄', checked: true },
        { id: 2, name: 'cls_it_meta.md', type: 'markdown', icon: '📄', checked: true },
        { id: 3, name: 'cls_m_code.md', type: 'markdown', icon: '📄', checked: false },
        { id: 4, name: 'cls_m_dept.md', type: 'markdown', icon: '📄', checked: false },
        { id: 5, name: 'cls_m_emp.md', type: 'markdown', icon: '📄', checked: false },
        { id: 6, name: 'cls_t_project.md', type: 'markdown', icon: '📄', checked: false },
        { id: 7, name: 'cls_t_project_member.md', type: 'markdown', icon: '📄', checked: false },
        { id: 8, name: 'cls_t_project_partner_status.md', type: 'markdown', icon: '📄', checked: false },
        { id: 9, name: 'cls_t_project_resourc_status_norm.md', type: 'markdown', icon: '📄', checked: false },
    ];

    const studioActions = [
        { id: 1, title: '리포트', icon: '📊', color: '#1976D2', description: '주요 내용을 요약한 보고서를 생성합니다' },
        { id: 2, title: '슬라이드', icon: '📑', color: '#E91E63', description: '프레젠테이션용 슬라이드를 만듭니다' },
    ];

    const handleSourceToggle = (id) => {
        setSelectedSources(prev =>
            prev.includes(id) ? prev.filter(sid => sid !== id) : [...prev, id]
        );
    };

    const handleSelectAll = () => {
        if (selectedSources.length === mockSources.length) {
            setSelectedSources([]);
        } else {
            setSelectedSources(mockSources.map(s => s.id));
        }
    };

    return (
        <div className="notebook-detail">
            {/* Header */}
            <div className="detail-header">
                <button className="back-btn" onClick={onBack}>
                    <svg width="20" height="20" viewBox="0 0 20 20" fill="currentColor">
                        <path d="M12.5 15l-5-5 5-5" />
                    </svg>
                    뒤로
                </button>
                <div className="header-actions">
                    <button className="icon-btn">
                        <svg width="20" height="20" viewBox="0 0 20 20" fill="currentColor">
                            <path d="M15.95 10.78c.03-.25.05-.51.05-.78s-.02-.53-.06-.78l1.69-1.32c.15-.12.19-.34.1-.51l-1.6-2.77c-.1-.18-.31-.24-.49-.18l-1.99.8c-.42-.32-.86-.58-1.35-.78L12 2.34c-.03-.2-.2-.34-.4-.34H8.4c-.2 0-.36.14-.39.34l-.3 2.12c-.49.2-.94.47-1.35.78l-1.99-.8c-.18-.07-.39 0-.49.18l-1.6 2.77c-.1.18-.06.39.1.51l1.69 1.32c-.04.25-.07.52-.07.78s.02.53.06.78L2.37 12.1c-.15.12-.19.34-.1.51l1.6 2.77c.1.18.31.24.49.18l1.99-.8c.42.32.86.58 1.35.78l.3 2.12c.04.2.2.34.4.34h3.2c.2 0 .37-.14.39-.34l.3-2.12c.49-.2.94-.47 1.35-.78l1.99.8c.18.07.39 0 .49-.18l1.6-2.77c.1-.18.06-.39-.1-.51l-1.67-1.32zM10 13c-1.65 0-3-1.35-3-3s1.35-3 3-3 3 1.35 3 3-1.35 3-3 3z" />
                        </svg>
                    </button>
                    <button className="icon-btn">
                        <svg width="20" height="20" viewBox="0 0 20 20" fill="currentColor">
                            <circle cx="10" cy="4" r="1.5" />
                            <circle cx="10" cy="10" r="1.5" />
                            <circle cx="10" cy="16" r="1.5" />
                        </svg>
                    </button>
                </div>
            </div>

            <div className="detail-content">
                {/* Left Sidebar - Sources */}
                {leftSidebarOpen ? (
                    <aside className="sources-sidebar">
                        <div className="sidebar-header">
                            <div className="sidebar-header-left">
                                <h2 className="sidebar-title">소스</h2>
                                <span className="source-count">{mockSources.length}</span>
                            </div>
                            <button
                                className="collapse-btn"
                                onClick={() => setLeftSidebarOpen(false)}
                                title="출처 닫기"
                            >
                                <svg width="20" height="20" viewBox="0 0 20 20" fill="currentColor">
                                    <path d="M13 16l-6-6 6-6v12z" />
                                </svg>
                            </button>
                        </div>

                        <button className="add-source-btn" onClick={() => setIsAddSourceModalOpen(true)}>
                            <svg width="18" height="18" viewBox="0 0 18 18" fill="currentColor">
                                <path d="M9 1v16M1 9h16" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
                            </svg>
                            소스 추가
                        </button>

                        <div className="source-search">
                            <svg width="18" height="18" viewBox="0 0 18 18" fill="currentColor">
                                <path d="M12.5 11h-.79l-.28-.27C12.41 9.59 13 8.11 13 6.5 13 2.91 10.09 0 6.5 0S0 2.91 0 6.5 2.91 13 6.5 13c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L17.49 16l-4.99-5zm-6 0C4.01 11 2 8.99 2 6.5S4.01 2 6.5 2 11 4.01 11 6.5 8.99 11 6.5 11z" />
                            </svg>
                            <input type="text" placeholder="찾아내 새 소스 검색하세요" />
                        </div>

                        <div className="source-actions">
                            <label className="select-all">
                                <input
                                    type="checkbox"
                                    checked={selectedSources.length === mockSources.length}
                                    onChange={handleSelectAll}
                                />
                                <span>모두 선택</span>
                            </label>
                        </div>

                        <div className="sources-list">
                            {mockSources.map(source => (
                                <div key={source.id} className="source-item">
                                    <input
                                        type="checkbox"
                                        checked={selectedSources.includes(source.id)}
                                        onChange={() => handleSourceToggle(source.id)}
                                    />
                                    <span className="source-icon">{source.icon}</span>
                                    <span className="source-name">{source.name}</span>
                                    <button className="source-menu">
                                        <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
                                            <circle cx="8" cy="3" r="1.5" />
                                            <circle cx="8" cy="8" r="1.5" />
                                            <circle cx="8" cy="13" r="1.5" />
                                        </svg>
                                    </button>
                                </div>
                            ))}
                        </div>
                    </aside>
                ) : (
                    <aside className="sources-sidebar-collapsed">
                        <button
                            className="expand-btn"
                            onClick={() => setLeftSidebarOpen(true)}
                            title="출처 열기"
                        >
                            <svg width="20" height="20" viewBox="0 0 20 20" fill="currentColor">
                                <path d="M7 4l6 6-6 6V4z" />
                            </svg>
                            <span>출처</span>
                        </button>
                    </aside>
                )}

                {/* Center Content */}
                <main className="notebook-content">
                    <div className="content-header">
                        <h1 className="notebook-title-large">{notebook.title}</h1>
                        <p className="notebook-subtitle">소스 {mockSources.length}개</p>
                    </div>

                    <div className="chat-summary">
                        <p className="summary-text">
                            비즈니스 메타데이터와 인적 자원 데이터에 대한 정보를 제공합니다.
                            이 노트북은 {mockSources.length}개의 소스 파일에서 추출한 데이터를 기반으로 합니다.
                            데이터 모델의 구조와 관계를 분석하고 이해할 수 있습니다.
                        </p>

                        <div className="summary-actions">
                            <button className="action-btn primary">
                                <svg width="18" height="18" viewBox="0 0 18 18" fill="currentColor">
                                    <path d="M14 2H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h10c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm-1 9H5V9h8v2z" />
                                </svg>
                                노트에 추가
                            </button>
                            <button className="icon-btn-small">
                                <svg width="18" height="18" viewBox="0 0 18 18" fill="currentColor">
                                    <path d="M1 16h3.5l9.56-9.56-3.5-3.5L1 12.5V16zm15.71-11.71c.39-.39.39-1.02 0-1.41l-2.12-2.12c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.54 3.54 1.82-1.84z" />
                                </svg>
                            </button>
                            <button className="icon-btn-small">
                                <svg width="18" height="18" viewBox="0 0 18 18" fill="currentColor">
                                    <path d="M9 1L6.5 6.5 1 7.8l4 3.9L4 17l5-2.6 5 2.6-1-5.3 4-3.9-5.5-1.3z" />
                                </svg>
                            </button>
                        </div>
                    </div>

                    <div className="suggested-questions">
                        <h3 className="questions-title">제안된 질문</h3>
                        <div className="questions-list">
                            <button className="question-item">
                                비즈니스 메타데이터와 IT 메타데이터의 차이점은 무엇인가요?
                            </button>
                            <button className="question-item">
                                프로젝트 인력 운영은 어떻게 관리되나요?
                            </button>
                            <button className="question-item">
                                데이터 모델의 주요 엔터티들 간의 관계를 설명해주세요.
                            </button>
                        </div>
                    </div>

                    <div className="chat-input-area">
                        <textarea
                            className="chat-input"
                            placeholder="질문을 입력하세요..."
                            rows="3"
                        />
                        <button className="send-btn">
                            <svg width="20" height="20" viewBox="0 0 20 20" fill="currentColor">
                                <path d="M2.01 2L2 8l10 2-10 2 .01 6L18 10z" />
                            </svg>
                        </button>
                    </div>
                </main>

                {/* Right Panel - Studio */}
                {rightPanelOpen ? (
                    <aside className="studio-panel">
                        <div className="panel-header-with-btn">
                            <div>
                                <h2 className="panel-title">스튜디오</h2>
                                <p className="panel-subtitle">소스를 통해 더 깊이있게 탐색하세요</p>
                            </div>
                            <button
                                className="collapse-btn"
                                onClick={() => setRightPanelOpen(false)}
                                title="스튜디오 닫기"
                            >
                                <svg width="20" height="20" viewBox="0 0 20 20" fill="currentColor">
                                    <path d="M7 4l6 6-6 6V4z" />
                                </svg>
                            </button>
                        </div>

                        <div className="studio-grid">
                            {studioActions.map(action => (
                                <div key={action.id} className="studio-card-wrapper">
                                    <button
                                        className="studio-card"
                                        style={{ '--card-color': action.color }}
                                        onClick={() => {
                                            if (action.id === 1) { // 리포트
                                                setIsReportModalOpen(true);
                                            }
                                        }}
                                    >
                                        <span className="card-icon">{action.icon}</span>
                                        <div className="card-content">
                                            <h3 className="card-title">{action.title}</h3>
                                            <p className="card-description">{action.description}</p>
                                        </div>
                                    </button>
                                    {action.id === 2 && ( // 슬라이드
                                        <button
                                            className="card-edit-btn"
                                            onClick={(e) => {
                                                e.stopPropagation();
                                                setIsSlideModalOpen(true);
                                            }}
                                            aria-label="슬라이드 수정"
                                        >
                                            ✏️
                                        </button>
                                    )}
                                </div>
                            ))}
                        </div>

                        <button className="add-note-btn">
                            <svg width="18" height="18" viewBox="0 0 18 18" fill="currentColor">
                                <path d="M9 1v16M1 9h16" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
                            </svg>
                            메모 추가
                        </button>
                    </aside>
                ) : (
                    <aside className="studio-panel-collapsed">
                        <button
                            className="expand-btn"
                            onClick={() => setRightPanelOpen(true)}
                            title="스튜디오 열기"
                        >
                            <svg width="20" height="20" viewBox="0 0 20 20" fill="currentColor">
                                <path d="M13 16l-6-6 6-6v12z" />
                            </svg>
                            <span>스튜디오</span>
                        </button>
                    </aside>
                )}
            </div>

            {/* Add Source Modal */}
            <AddSourceModal
                isOpen={isAddSourceModalOpen}
                onClose={() => setIsAddSourceModalOpen(false)}
            />

            {/* Report Generation Modal */}
            <ReportGenerationModal
                isOpen={isReportModalOpen}
                onClose={() => setIsReportModalOpen(false)}
            />

            {/* Slide Creation Modal */}
            <SlideCreationModal
                isOpen={isSlideModalOpen}
                onClose={() => setIsSlideModalOpen(false)}
            />
        </div>
    );
}

export default NotebookDetail;
