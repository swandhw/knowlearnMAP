import { useState, useRef, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { workspaceApi } from '../services/api';
import { useAuth } from '../context/AuthContext';

import LoginModal from '../components/LoginModal';
import '../components/LoginModal.css';

function Home() {
    const { isAdmin, isAuthenticated, user, logout } = useAuth();
    const [loginModalOpen, setLoginModalOpen] = useState(false);
    const [activeTab, setActiveTab] = useState('내 워크스페이스');
    const [viewMode, setViewMode] = useState('grid');
    const [sortBy, setSortBy] = useState('최신순');
    const [openMenuId, setOpenMenuId] = useState(null);
    const [notebooks, setNotebooks] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [renameModalOpen, setRenameModalOpen] = useState(false);
    const [renamingNotebook, setRenamingNotebook] = useState(null);
    const [newName, setNewName] = useState('');
    const menuRef = useRef(null);
    const navigate = useNavigate();

    // 워크스페이스 목록 불러오기
    useEffect(() => {
        const fetchWorkspaces = async () => {
            try {
                setLoading(true);
                const data = await workspaceApi.getAll();
                setNotebooks(data || []);
                setError(null);
            } catch (err) {
                console.error('워크스페이스 로드 실패:', err);
                setError('워크스페이스를 불러올 수 없습니다.');
                setNotebooks([]);
            } finally {
                setLoading(false);
            }
        };

        fetchWorkspaces();
    }, []);

    // Close menu when clicking outside
    useEffect(() => {
        const handleClickOutside = (event) => {
            if (menuRef.current && !menuRef.current.contains(event.target)) {
                setOpenMenuId(null);
            }
        };

        if (openMenuId !== null) {
            document.addEventListener('mousedown', handleClickOutside);
            return () => {
                document.removeEventListener('mousedown', handleClickOutside);
            };
        }
    }, [openMenuId]);

    const handleMenuToggle = (e, notebookId) => {
        e.stopPropagation();
        setOpenMenuId(openMenuId === notebookId ? null : notebookId);
    };

    const handleDelete = async (e, notebookId) => {
        e.stopPropagation();

        if (!window.confirm('정말 삭제하시겠습니까?')) {
            return;
        }

        try {
            await workspaceApi.delete(notebookId);
            // 삭제 후 목록에서 제거
            setNotebooks(prev => prev.filter(nb => nb.id !== notebookId));
            setOpenMenuId(null);
        } catch (err) {
            console.error('삭제 실패:', err);
            alert('삭제에 실패했습니다.');
        }
    };

    const handleRename = (e, notebookId) => {
        e.stopPropagation();
        const notebook = notebooks.find(nb => nb.id === notebookId);
        if (notebook) {
            setRenamingNotebook(notebook);
            setNewName(notebook.name || notebook.title || '');
            setRenameModalOpen(true);
        }
        setOpenMenuId(null);
    };

    const handleRenameSubmit = async () => {
        if (!newName.trim()) {
            alert('워크스페이스 이름을 입력해주세요.');
            return;
        }

        try {
            const updated = await workspaceApi.update(renamingNotebook.id, {
                ...renamingNotebook,
                name: newName.trim()
            });

            // 목록 업데이트
            setNotebooks(prev => prev.map(nb =>
                nb.id === updated.id ? { ...nb, name: updated.name, title: updated.name } : nb
            ));

            // 모달 닫기
            setRenameModalOpen(false);
            setRenamingNotebook(null);
            setNewName('');
        } catch (err) {
            console.error('이름 변경 실패:', err);
            alert('이름 변경에 실패했습니다.');
        }
    };

    const handleCreateNew = async () => {
        try {
            const newWorkspace = {
                name: 'Untitled notebook',
                description: '',
                icon: '📄',
                color: 'yellow'
            };

            const created = await workspaceApi.create(newWorkspace);

            // 목록에 추가
            setNotebooks(prev => [created, ...prev]);

            // 상세 페이지로 이동
            navigate(`/notebook/${created.id}`, { state: { openAddSource: true } });
        } catch (err) {
            console.error('워크스페이스 생성 실패:', err);
            alert('워크스페이스 생성에 실패했습니다.');
        }
    };

    const handleNotebookClick = (id) => {
        navigate(`/notebook/${id}`);
    };

    return (
        <div className="home-container">
            {/* Header */}
            <header className="app-header">
                <div className="header-left">
                    <button className="icon-btn">
                        <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
                            <path d="M3 18h18v-2H3v2zm0-5h18v-2H3v2zm0-7v2h18V6H3z" />
                        </svg>
                    </button>
                    <div className="logo">
                        <span className="logo-text">KNOWLEARN MAP</span>
                    </div>
                </div>
            </header>

            <div className="main-content">
                {/* Tab Navigation */}
                <nav className="tab-navigation">
                    <div className="tabs-left">
                        <button
                            className={`tab ${activeTab === '전체' ? 'active' : ''}`}
                            onClick={() => setActiveTab('전체')}
                        >
                            전체
                        </button>
                        <button
                            className={`tab ${activeTab === '내 워크스페이스' ? 'active' : ''}`}
                            onClick={() => setActiveTab('내 워크스페이스')}
                        >
                            내 워크스페이스
                        </button>
                        {isAdmin && (
                            <>
                                <button
                                    className="tab admin-tab"
                                    onClick={() => navigate('/admin')}
                                >
                                    ⚙️ 도메인 관리
                                </button>
                                <button
                                    className="tab admin-tab"
                                    onClick={() => navigate('/prompts')}
                                >
                                    💬 프롬프트 관리
                                </button>
                            </>
                        )}
                    </div>

                    {isAuthenticated ? (
                        <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginLeft: 'auto' }}>
                            <span style={{ fontSize: '14px', color: 'var(--color-text-secondary)' }}>
                                {user?.username || user?.email}
                            </span>
                            <button
                                className="icon-btn"
                                onClick={logout}
                                title="로그아웃"
                            >
                                <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
                                    <path d="M17 7l-1.41 1.41L18.17 11H8v2h10.17l-2.58 2.58L17 17l5-5zM4 5h8V3H4c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h8v-2H4V5z" />
                                </svg>
                            </button>
                        </div>
                    ) : (
                        <button
                            className="icon-btn login-btn"
                            onClick={() => setLoginModalOpen(true)}
                            title="로그인"
                            style={{ marginLeft: 'auto' }}
                        >
                            <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
                                <path d="M12 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm0 2c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z" />
                            </svg>
                        </button>
                    )}
                </nav>

                {/* Toolbar */}
                <div className="toolbar">
                    <h1 className="page-title">내 워크스페이스</h1>

                    <div className="toolbar-actions">
                        <div className="view-toggle">
                            <button
                                className={`view-btn ${viewMode === 'grid' ? 'active' : ''}`}
                                onClick={() => setViewMode('grid')}
                            >
                                <svg width="20" height="20" viewBox="0 0 20 20" fill="currentColor">
                                    <rect x="2" y="2" width="7" height="7" />
                                    <rect x="11" y="2" width="7" height="7" />
                                    <rect x="2" y="11" width="7" height="7" />
                                    <rect x="11" y="11" width="7" height="7" />
                                </svg>
                            </button>
                            <button
                                className={`view-btn ${viewMode === 'list' ? 'active' : ''}`}
                                onClick={() => setViewMode('list')}
                            >
                                <svg width="20" height="20" viewBox="0 0 20 20" fill="currentColor">
                                    <rect x="2" y="3" width="16" height="2" />
                                    <rect x="2" y="8" width="16" height="2" />
                                    <rect x="2" y="13" width="16" height="2" />
                                </svg>
                            </button>
                        </div>

                        <select
                            className="sort-select"
                            value={sortBy}
                            onChange={(e) => setSortBy(e.target.value)}
                        >
                            <option value="최신순">최신순</option>
                            <option value="오래된순">오래된순</option>
                            <option value="이름순">이름순</option>
                        </select>

                        <button className="new-note-btn" onClick={handleCreateNew}>
                            <svg width="20" height="20" viewBox="0 0 20 20" fill="currentColor">
                                <path d="M10 4v12M4 10h12" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
                            </svg>
                            새 워크스페이스
                        </button>
                    </div>
                </div>

                {/* 로딩 상태 */}
                {loading && (
                    <div style={{ padding: '40px', textAlign: 'center' }}>
                        <p>로딩 중...</p>
                    </div>
                )}

                {/* 에러 상태 */}
                {error && (
                    <div style={{ padding: '40px', textAlign: 'center', color: '#d32f2f' }}>
                        <p>{error}</p>
                        <button
                            onClick={() => window.location.reload()}
                            style={{
                                marginTop: '10px',
                                padding: '8px 16px',
                                cursor: 'pointer'
                            }}
                        >
                            다시 시도
                        </button>
                    </div>
                )}

                {/* Notebooks Grid */}
                {!loading && !error && (
                    <div className={`notebooks-container ${viewMode}`}>
                        {/* Table Header (List View Only) */}
                        {viewMode === 'list' && (
                            <div className="table-header">
                                <div className="header-icon"></div>
                                <div className="header-title">제목</div>
                                <div className="header-source">소스</div>
                                <div className="header-date">생성일</div>
                                <div className="header-role">역할</div>
                                <div className="header-actions"></div>
                            </div>
                        )}

                        {/* Create New Card */}
                        <div className="notebook-card create-card" onClick={handleCreateNew}>
                            <div className="create-card-content">
                                <div className="create-icon">
                                    <svg width="48" height="48" viewBox="0 0 48 48" fill="none" stroke="currentColor" strokeWidth="2">
                                        <circle cx="24" cy="24" r="20" />
                                        <path d="M24 16v16M16 24h16" strokeLinecap="round" />
                                    </svg>
                                </div>
                                <span className="create-text">새 워크스페이스 만들기</span>
                            </div>
                        </div>

                        {/* Notebook Cards */}
                        {notebooks.map((notebook) => (
                            <div
                                key={notebook.id}
                                className={`notebook-card ${notebook.color}`}
                                onClick={() => handleNotebookClick(notebook.id)}
                            >
                                <div className="card-header">
                                    <div className="notebook-icon">{notebook.icon}</div>
                                    {/* More button in header for grid view */}
                                    {viewMode === 'grid' && (
                                        <div className="more-btn-container" ref={openMenuId === notebook.id ? menuRef : null}>
                                            <button
                                                className="more-btn"
                                                onClick={(e) => handleMenuToggle(e, notebook.id)}
                                            >
                                                <svg width="20" height="20" viewBox="0 0 20 20" fill="currentColor">
                                                    <circle cx="10" cy="4" r="1.5" />
                                                    <circle cx="10" cy="10" r="1.5" />
                                                    <circle cx="10" cy="16" r="1.5" />
                                                </svg>
                                            </button>
                                            {openMenuId === notebook.id && (
                                                <div className="popup-menu">
                                                    <button
                                                        className="menu-item"
                                                        onClick={(e) => handleRename(e, notebook.id)}
                                                    >
                                                        <svg width="18" height="18" viewBox="0 0 18 18" fill="currentColor">
                                                            <path d="M2 12.88V16h3.12L14.06 7.06l-3.12-3.12L2 12.88zM16.71 4.71l-2.42-2.42a1 1 0 0 0-1.42 0l-1.83 1.83 3.12 3.12 1.83-1.83a1 1 0 0 0 0-1.42l.72-.72z" />
                                                        </svg>
                                                        제목 수정
                                                    </button>
                                                    <button
                                                        className="menu-item delete"
                                                        onClick={(e) => handleDelete(e, notebook.id)}
                                                    >
                                                        <svg width="18" height="18" viewBox="0 0 18 18" fill="currentColor">
                                                            <path d="M6 16c0 1.1.9 2 2 2h2c1.1 0 2-.9 2-2V6H6v10zm1-9h4v9H7V7zm6.5-5H11L10.5 1h-3l-.5 1H4.5v2h9V2z" />
                                                        </svg>
                                                        삭제
                                                    </button>
                                                </div>
                                            )}
                                        </div>
                                    )}
                                </div>
                                <div className="card-body">
                                    {/* Icon at start for list view */}
                                    {viewMode === 'list' && <div className="notebook-icon">{notebook.icon}</div>}
                                    <h3 className="notebook-title">{notebook.title}</h3>
                                    <p className="notebook-source">{notebook.source}</p>
                                    <p className="notebook-date">{notebook.date}</p>
                                    <p className="notebook-role">{notebook.role}</p>
                                    {/* More button at end for list view */}
                                    {viewMode === 'list' && (
                                        <div className="more-btn-container" ref={openMenuId === notebook.id ? menuRef : null}>
                                            <button
                                                className="more-btn"
                                                onClick={(e) => handleMenuToggle(e, notebook.id)}
                                            >
                                                <svg width="20" height="20" viewBox="0 0 20 20" fill="currentColor">
                                                    <circle cx="10" cy="4" r="1.5" />
                                                    <circle cx="10" cy="10" r="1.5" />
                                                    <circle cx="10" cy="16" r="1.5" />
                                                </svg>
                                            </button>
                                            {openMenuId === notebook.id && (
                                                <div className="popup-menu">
                                                    <button
                                                        className="menu-item"
                                                        onClick={(e) => handleRename(e, notebook.id)}
                                                    >
                                                        <svg width="18" height="18" viewBox="0 0 18 18" fill="currentColor">
                                                            <path d="M2 12.88V16h3.12L14.06 7.06l-3.12-3.12L2 12.88zM16.71 4.71l-2.42-2.42a1 1 0 0 0-1.42 0l-1.83 1.83 3.12 3.12 1.83-1.83a1 1 0 0 0 0-1.42l.72-.72z" />
                                                        </svg>
                                                        제목 수정
                                                    </button>
                                                    <button
                                                        className="menu-item delete"
                                                        onClick={(e) => handleDelete(e, notebook.id)}
                                                    >
                                                        <svg width="18" height="18" viewBox="0 0 18 18" fill="currentColor">
                                                            <path d="M6 16c0 1.1.9 2 2 2h2c1.1 0 2-.9 2-2V6H6v10zm1-9h4v9H7V7zm6.5-5H11L10.5 1h-3l-.5 1H4.5v2h9V2z" />
                                                        </svg>
                                                        삭제
                                                    </button>
                                                </div>
                                            )}
                                        </div>
                                    )}
                                </div>
                            </div>
                        ))}
                    </div>
                )}

                {/* Rename Modal */}
                {renameModalOpen && (
                    <div className="modal-overlay" onClick={() => setRenameModalOpen(false)}>
                        <div className="modal-content" onClick={(e) => e.stopPropagation()}>
                            <div className="modal-icon">
                                <svg width="64" height="64" viewBox="0 0 64 64" fill="none">
                                    <circle cx="32" cy="32" r="28" fill="#4a5568" />
                                    <path d="M32 20v24M20 32h24" stroke="white" strokeWidth="4" strokeLinecap="round" />
                                </svg>
                            </div>
                            <h2 className="modal-title">워크스페이스 이름 변경</h2>
                            <input
                                type="text"
                                className="modal-input"
                                value={newName}
                                onChange={(e) => setNewName(e.target.value)}
                                onKeyPress={(e) => {
                                    if (e.key === 'Enter') {
                                        handleRenameSubmit();
                                    }
                                }}
                                placeholder="워크스페이스 이름"
                                autoFocus
                            />
                            <div className="modal-buttons">
                                <button
                                    className="modal-btn cancel-btn"
                                    onClick={() => setRenameModalOpen(false)}
                                >
                                    취소
                                </button>
                                <button
                                    className="modal-btn confirm-btn"
                                    onClick={handleRenameSubmit}
                                >
                                    저장
                                </button>
                            </div>
                        </div>
                    </div>
                )}

                <LoginModal
                    isOpen={loginModalOpen}
                    onClose={() => setLoginModalOpen(false)}
                />
            </div>
        </div>
    );
}

export default Home;
