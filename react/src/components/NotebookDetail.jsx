import { useState, useEffect } from 'react';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import { getNotebook } from '../data/mockData';
import './NotebookDetail.css';
import AddSourceModal from './AddSourceModal';
import ReportGenerationModal from './ReportGenerationModal';
import SlideCreationModal from './SlideCreationModal';
import KnowledgeMapView from './KnowledgeMapView';
import DictionaryView from './DictionaryView';

function NotebookDetail() {
    const { id } = useParams();
    const navigate = useNavigate();
    const location = useLocation();
    const [notebook, setNotebook] = useState(null);
    const initialOpenAddSource = location.state?.openAddSource || false;
    const [selectedSources, setSelectedSources] = useState([]);
    const [leftSidebarOpen, setLeftSidebarOpen] = useState(true);
    const [rightPanelOpen, setRightPanelOpen] = useState(true);
    const [isAddSourceModalOpen, setIsAddSourceModalOpen] = useState(initialOpenAddSource);
    const [isReportModalOpen, setIsReportModalOpen] = useState(false);
    const [isSlideModalOpen, setIsSlideModalOpen] = useState(false);
    const [currentTab, setCurrentTab] = useState('chat'); // chat, knowledge, dictionary
    const [memos, setMemos] = useState([]);
    const [activeMemoMenuId, setActiveMemoMenuId] = useState(null);
    const [isMemoEditorOpen, setIsMemoEditorOpen] = useState(false);
    const [currentMemoContent, setCurrentMemoContent] = useState('');
    const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
    const [showHeadings, setShowHeadings] = useState(false);

    useEffect(() => {
        const handleClickOutside = (event) => {
            if (activeMemoMenuId && !event.target.closest('.memo-menu-container')) {
                setActiveMemoMenuId(null);
            }
            if (showHeadings && !event.target.closest('.toolbar-btn.text-btn')) {
                setShowHeadings(false);
            }
        };
        document.addEventListener('click', handleClickOutside);
        return () => document.removeEventListener('click', handleClickOutside);
    }, [activeMemoMenuId, showHeadings]);

    console.log("NotebookDetail Render", { id, notebook, memos });

    // ... existing useEffect ...

    // ... existing handlers ...

    const handleDeleteClick = () => {
        setShowDeleteConfirm(true);
    };

    const handleConfirmDelete = () => {
        setIsMemoEditorOpen(false);
        setCurrentMemoContent('');
        setShowDeleteConfirm(false);
    };

    const handleCancelDelete = () => {
        setShowDeleteConfirm(false);
    };

    const insertHeading = (level) => {
        const prefix = level === 0 ? '' : '#'.repeat(level) + ' ';
        // Simple insertion logic: replace content for now or append?
        // Ideally should insert at cursor, but for simplicity let's just prepend to current line or simplify.
        // Given current simple textarea, let's just append or replace selection logic if possible.
        // For MVP, just append to end or replace content is tricky without ref.
        // Let's assume we just append if empty, or wrap?
        // Actually, let's just set the text area value logic:
        // A better way for textarea manipulation:
        const textarea = document.querySelector('.editor-textarea');
        if (!textarea) return;

        const start = textarea.selectionStart;
        const end = textarea.selectionEnd;
        const text = currentMemoContent;
        const before = text.substring(0, start);
        const after = text.substring(end);

        // Find beginning of current line
        const lastNewLine = before.lastIndexOf('\n');
        const lineStart = lastNewLine === -1 ? 0 : lastNewLine + 1;
        const lineContent = text.substring(lineStart, end); // from line start to cursor/end

        // Check if already has heading
        const match = lineContent.match(/^(#{1,3}\s)?(.*)/);
        const cleanContent = match ? match[2] : lineContent;

        const newContent = text.substring(0, lineStart) + prefix + cleanContent + after;

        setCurrentMemoContent(newContent);
        setShowHeadings(false);
        // Should restore cursor but standard React flow might reset it. 
    };

    const insertList = (type) => {
        const textarea = document.querySelector('.editor-textarea');
        if (!textarea) return;

        const start = textarea.selectionStart;
        const text = currentMemoContent;
        const before = text.substring(0, start);
        const after = text.substring(start);

        // Find beginning of current line
        const lastNewLine = before.lastIndexOf('\n');
        const lineStart = lastNewLine === -1 ? 0 : lastNewLine + 1;

        const prefix = type === 'bullet' ? '- ' : '1. ';
        const newContent = text.substring(0, lineStart) + prefix + text.substring(lineStart);

        setCurrentMemoContent(newContent);
    };

    useEffect(() => {
        if (id) {
            const found = getNotebook(id);
            if (found) {
                setNotebook(found);
                // Initialize selectedSources if needed, or just keep it empty initially
                // If we want to check all by default:
                // setSelectedSources(found.sources.map(s => s.id));
            } else {
                // Handle not found
                navigate('/');
            }
        }
    }, [id, navigate]);

    if (!notebook) return <div>Loading...</div>;


    // Use notebook sources instead of mockSources
    const sources = notebook.sources || [];
    const isEmpty = sources.length === 0;


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
        if (selectedSources.length === sources.length) {
            setSelectedSources([]);
        } else {
            setSelectedSources(sources.map(s => s.id));
        }
    };

    const handleSaveMemo = () => {
        const textToSave = `이 노트북은 ${sources.length}개의 소스 파일에서 추출한 데이터를 기반으로 합니다.`;
        const newMemo = {
            id: Date.now(),
            content: textToSave,
            date: new Date().toLocaleDateString()
        };
        setMemos(prev => [newMemo, ...prev]);
        if (!rightPanelOpen) setRightPanelOpen(true);
    };

    const deleteMemo = (id) => {
        setMemos(prev => prev.filter(memo => memo.id !== id));
        setActiveMemoMenuId(null);
    };

    const convertMemoToSource = (memo) => {
        const newSource = {
            id: `memo-${memo.id}`,
            name: `메모: ${memo.content.substring(0, 20)}...`,
            icon: '📝',
            type: 'text'
        };

        setNotebook(prev => ({
            ...prev,
            sources: [...prev.sources, newSource]
        }));

        // Optional: Remove memo after converting? For now, keep it.
        setActiveMemoMenuId(null);
        alert('메모가 소스로 변환되었습니다.');
    };

    const convertAllMemosToSource = () => {
        if (memos.length === 0) return;

        const newSources = memos.map(memo => ({
            id: `memo-${memo.id}`,
            name: `메모: ${memo.content.substring(0, 20)}...`,
            icon: '📝',
            type: 'text'
        }));

        setNotebook(prev => ({
            ...prev,
            sources: [...prev.sources, ...newSources]
        }));

        setActiveMemoMenuId(null);
        alert('모든 메모가 소스로 변환되었습니다.');
    };

    const handleCopy = () => {
        const textToCopy = `이 노트북은 ${sources.length}개의 소스 파일에서 추출한 데이터를 기반으로 합니다.`;
        navigator.clipboard.writeText(textToCopy).then(() => {
            alert('클립보드에 복사되었습니다.');
        });
    };

    const handleSaveMemoAsSource = () => {
        if (!currentMemoContent.trim()) {
            alert('내용을 입력해주세요.');
            return;
        }

        const newSource = {
            id: `memo-${Date.now()}`,
            name: `메모: ${currentMemoContent.substring(0, 20)}...`,
            icon: '📝',
            type: 'text'
        };

        setNotebook(prev => ({
            ...prev,
            sources: [...prev.sources, newSource]
        }));

        setIsMemoEditorOpen(false);
        setCurrentMemoContent('');
        alert('소스로 변환되었습니다.');
    };

    return (
        <div className="notebook-detail">
            {/* Header */}
            <div className="detail-header">
                <button className="home-btn" onClick={() => navigate('/')} title="홈으로">
                    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"></path>
                        <polyline points="9 22 9 12 15 12 15 22"></polyline>
                    </svg>
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
                            </div>
                            <button
                                className="collapse-btn"
                                onClick={() => setLeftSidebarOpen(false)}
                                title="출처 닫기"
                            >
                                <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                    <rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect>
                                    <line x1="9" y1="3" x2="9" y2="21"></line>
                                    <path d="M10 8l-4 4 4 4"></path>
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
                                    checked={sources.length > 0 && selectedSources.length === sources.length}
                                    onChange={handleSelectAll}
                                    disabled={isEmpty}
                                />
                                <span>모두 선택</span>
                            </label>

                            <div className="sidebar-view-buttons">
                                <button
                                    className={`icon-view-btn ${currentTab === 'knowledge' ? 'active' : ''}`}
                                    onClick={() => setCurrentTab('knowledge')}
                                    title="지식맵"
                                >
                                    <img src="/icons/icon_knowledge.png" alt="지식맵" />
                                </button>
                                <button
                                    className={`icon-view-btn ${currentTab === 'dictionary' ? 'active' : ''}`}
                                    onClick={() => setCurrentTab('dictionary')}
                                    title="사전"
                                >
                                    <img src="/icons/icon_dictionary.png" alt="사전" />
                                </button>
                            </div>
                        </div>

                        <div className="sources-list">
                            {isEmpty ? (
                                <div className="empty-sources-sidebar">
                                    <div className="empty-icon">
                                        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                                            <polyline points="14 2 14 8 20 8"></polyline>
                                            <line x1="16" y1="13" x2="8" y2="13"></line>
                                            <line x1="16" y1="17" x2="8" y2="17"></line>
                                            <polyline points="10 9 9 9 8 9"></polyline>
                                        </svg>
                                    </div>
                                    <p>저장된 소스가 여기에 표시됩니다</p>
                                    <p className="empty-subtext">
                                        위의 소스 추가를 클릭하여 PDF, 웹사이트, 텍스트, 동영상 또는 오디오 파일을 추가하세요.
                                    </p>
                                </div>
                            ) : (
                                sources.map(source => (
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
                                ))
                            )}
                        </div>
                    </aside>
                ) : (
                    <aside className="sources-sidebar-collapsed">
                        <button
                            className="expand-btn vertically"
                            onClick={() => setLeftSidebarOpen(true)}
                            title="출처 열기"
                        >
                            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                <rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect>
                                <line x1="9" y1="3" x2="9" y2="21"></line>
                                <path d="M10 10l4 2-4 2"></path>
                            </svg>
                        </button>
                        <div className="collapsed-icon-list">
                            {sources.slice(0, 5).map(source => (
                                <div key={source.id} className="collapsed-icon-item" title={source.name}>
                                    {source.icon}
                                </div>
                            ))}
                            {sources.length > 5 && (
                                <div className="collapsed-more-indicator">...</div>
                            )}
                        </div>
                    </aside>
                )}

                {/* Center Content */}
                <main className="notebook-content">
                    {!isEmpty && (
                        <div className="content-header-tabs">
                            <button
                                className={`header-tab ${currentTab === 'chat' ? 'active' : ''}`}
                                onClick={() => setCurrentTab('chat')}
                            >
                                채팅
                            </button>
                            <button
                                className={`header-tab ${currentTab === 'knowledge' ? 'active' : ''}`}
                                onClick={() => setCurrentTab('knowledge')}
                            >
                                지식맵
                            </button>
                            <button
                                className={`header-tab ${currentTab === 'dictionary' ? 'active' : ''}`}
                                onClick={() => setCurrentTab('dictionary')}
                            >
                                사전
                            </button>
                        </div>
                    )}

                    {isEmpty ? (
                        <div className="empty-state-container">
                            <div className="empty-state-content">
                                <div className="empty-upload-icon">
                                    <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
                                        <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
                                        <polyline points="17 8 12 3 7 8" />
                                        <line x1="12" y1="3" x2="12" y2="15" />
                                    </svg>
                                </div>
                                <h2>시작하려면 소스 추가</h2>
                                <button className="empty-upload-btn" onClick={() => setIsAddSourceModalOpen(true)}>
                                    소스 업로드
                                </button>
                            </div>
                        </div>
                    ) : (
                        null
                    )}

                    {/* Content based on Tab */}
                    {!isEmpty && currentTab === 'knowledge' && (
                        <div className="tab-content full-height">
                            <KnowledgeMapView sources={sources} title={notebook.title} />
                        </div>
                    )}

                    {!isEmpty && currentTab === 'dictionary' && (
                        <div className="tab-content full-height">
                            <DictionaryView />
                        </div>
                    )}

                    {!isEmpty && currentTab === 'chat' && (
                        <>
                            <div className="chat-summary">
                                <p className="summary-text">
                                    이 노트북은 {sources.length}개의 소스 파일에서 추출한 데이터를 기반으로 합니다.
                                </p>

                                <div className="summary-actions">
                                    <button className="action-btn primary" onClick={handleSaveMemo}>
                                        <svg width="18" height="18" viewBox="0 0 18 18" fill="currentColor">
                                            <path d="M14 2H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h10c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm-1 9H5V9h8v2z" />
                                        </svg>
                                        메모에 저장
                                    </button>
                                    <button className="icon-btn-small" onClick={handleCopy} title="복사">
                                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                            <rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect>
                                            <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path>
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
                        </>
                    )}
                </main>

                {/* Right Panel - Studio */}
                {rightPanelOpen ? (
                    <aside className="studio-panel">
                        {/* Editor Mode */}
                        {isMemoEditorOpen ? (
                            <div className="memo-editor">
                                <div className="editor-header">
                                    <div className="editor-breadcrumb">스튜디오 {'>'} 메모</div>
                                    <button
                                        className="close-editor-btn"
                                        onClick={() => setIsMemoEditorOpen(false)}
                                    >
                                        <svg width="20" height="20" viewBox="0 0 20 20" fill="currentColor">
                                            <path d="M4.293 4.293a1 1 0 011.414 0L10 8.586l4.293-4.293a1 1 0 111.414 1.414L11.414 10l4.293 4.293a1 1 0 01-1.414 1.414L10 11.414l-4.293 4.293a1 1 0 01-1.414-1.414L8.586 10 4.293 5.707a1 1 0 010-1.414z" />
                                        </svg>
                                    </button>
                                </div>
                                <div className="editor-title-row">
                                    <input type="text" value="새 메모" readOnly className="editor-title-input" />
                                    <button className="editor-delete-btn" onClick={handleDeleteClick}>
                                        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                            <polyline points="3 6 5 6 21 6"></polyline>
                                            <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2-2v2"></path>
                                        </svg>
                                    </button>
                                </div>
                                <div className="editor-toolbar">
                                    <button className="toolbar-btn">↩</button>
                                    <button className="toolbar-btn">↪</button>
                                    <div className="toolbar-divider"></div>
                                    <div className="heading-dropdown-container">
                                        <button
                                            className="toolbar-btn text-btn"
                                            onClick={() => setShowHeadings(!showHeadings)}
                                        >
                                            Normal <span className="arrow">▼</span>
                                        </button>
                                        {showHeadings && (
                                            <div className="heading-dropdown">
                                                <button onClick={() => insertHeading(0)}>Normal</button>
                                                <button className="h1-option" onClick={() => insertHeading(1)}>Heading 1</button>
                                                <button className="h2-option" onClick={() => insertHeading(2)}>Heading 2</button>
                                                <button className="h3-option" onClick={() => insertHeading(3)}>Heading 3</button>
                                            </div>
                                        )}
                                    </div>
                                    <div className="toolbar-divider"></div>
                                    <button className="toolbar-btn bold">B</button>
                                    <button className="toolbar-btn italic">I</button>
                                    <button className="toolbar-btn">🔗</button>
                                    <div className="toolbar-divider"></div>
                                    <button className="toolbar-btn" onClick={() => insertList('number')}>≡</button>
                                    <button className="toolbar-btn" onClick={() => insertList('bullet')}>⋮≡</button>
                                </div>
                                <div className="editor-content-area">
                                    <textarea
                                        className="editor-textarea"
                                        placeholder="메모를 입력하세요..."
                                        value={currentMemoContent}
                                        onChange={(e) => setCurrentMemoContent(e.target.value)}
                                    />
                                </div>
                                <div className="editor-footer">
                                    <button className="convert-source-btn" onClick={handleSaveMemoAsSource}>
                                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                                            <polyline points="14 2 14 8 20 8"></polyline>
                                            <line x1="16" y1="13" x2="8" y2="13"></line>
                                            <line x1="16" y1="17" x2="8" y2="17"></line>
                                            <polyline points="10 9 9 9 8 9"></polyline>
                                        </svg>
                                        소스로 전환
                                    </button>
                                </div>

                                {showDeleteConfirm && (
                                    <div className="delete-confirm-overlay">
                                        <div className="delete-confirm-box">
                                            <h3>삭제 메모?</h3>
                                            <div className="delete-confirm-actions">
                                                <button className="cancel-delete-btn" onClick={handleCancelDelete}>취소</button>
                                                <button className="confirm-delete-btn" onClick={handleConfirmDelete}>삭제</button>
                                            </div>
                                        </div>
                                    </div>
                                )}
                            </div>
                        ) : (
                            /* Default List Mode */
                            <>
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
                                        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                            <rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect>
                                            <line x1="15" y1="3" x2="15" y2="21"></line>
                                            <path d="M8 8l4 4-4 4"></path>
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

                                {/* Memos List */}
                                <div className="studio-memos">
                                    {memos.length > 0 && <h3 className="section-title">메모 ({memos.length})</h3>}
                                    <div className="memo-list">
                                        {memos.map(memo => (
                                            <div key={memo.id} className="memo-card memo-menu-container">
                                                <div className="memo-content">{memo.content}</div>
                                                <div className="memo-footer">
                                                    <div className="memo-date">{memo.date}</div>
                                                    <div className="memo-menu-wrapper">
                                                        <button
                                                            className="memo-menu-btn"
                                                            onClick={(e) => {
                                                                e.stopPropagation();
                                                                setActiveMemoMenuId(activeMemoMenuId === memo.id ? null : memo.id);
                                                            }}
                                                        >
                                                            <svg width="16" height="16" viewBox="0 0 16 16" fill="currentColor">
                                                                <circle cx="8" cy="3" r="1.5" />
                                                                <circle cx="8" cy="8" r="1.5" />
                                                                <circle cx="8" cy="13" r="1.5" />
                                                            </svg>
                                                        </button>
                                                        {activeMemoMenuId === memo.id && (
                                                            <div className="memo-dropdown-menu">
                                                                <button onClick={() => convertMemoToSource(memo)}>
                                                                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="menu-icon">
                                                                        <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path>
                                                                        <polyline points="14 2 14 8 20 8"></polyline>
                                                                        <line x1="12" y1="18" x2="12" y2="12"></line>
                                                                        <line x1="9" y1="15" x2="15" y2="15"></line>
                                                                    </svg>
                                                                    소스로 변환
                                                                </button>
                                                                <button onClick={convertAllMemosToSource}>
                                                                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="menu-icon">
                                                                        <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                                                                        <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                                                                    </svg>
                                                                    모든 메모를 소스로 변환
                                                                </button>
                                                                <button
                                                                    className="delete-item"
                                                                    onClick={() => deleteMemo(memo.id)}
                                                                >
                                                                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className="menu-icon">
                                                                        <polyline points="3 6 5 6 21 6"></polyline>
                                                                        <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                                                                    </svg>
                                                                    삭제
                                                                </button>
                                                            </div>
                                                        )}
                                                    </div>
                                                </div>
                                            </div>
                                        ))}
                                    </div>
                                </div>

                                {isEmpty && memos.length === 0 && (
                                    <div className="empty-studio-placeholder">
                                        <div className="placeholder-icon">✨</div>
                                        <p>스튜디오 출력이 여기에 저장됩니다</p>
                                        <p className="placeholder-subtext">
                                            소스를 추가한 후 클릭하여 AI 오디오 오버뷰, 학습 가이드, 마인드맵 등을 추가해 보세요.
                                        </p>
                                    </div>
                                )}

                                <button
                                    className="add-note-btn"
                                    onClick={() => setIsMemoEditorOpen(true)}
                                >
                                    <svg width="18" height="18" viewBox="0 0 18 18" fill="currentColor">
                                        <path d="M9 1v16M1 9h16" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
                                    </svg>
                                    메모 추가
                                </button>
                            </>
                        )}
                    </aside>
                ) : (
                    <aside className="studio-panel-collapsed">
                        <button
                            className="expand-btn vertically"
                            onClick={() => setRightPanelOpen(true)}
                            title="스튜디오 열기"
                        >
                            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                                <rect x="3" y="3" width="18" height="18" rx="2" ry="2"></rect>
                                <line x1="15" y1="3" x2="15" y2="21"></line>
                                <path d="M12 8l-4 4 4 4"></path>
                            </svg>
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
