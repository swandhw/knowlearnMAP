import { useState, useRef, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { Edit2, Trash2 } from 'lucide-react';
import { workspaceApi } from '../services/api';
import { useAuth } from '../context/AuthContext';
import { useAlert } from '../context/AlertContext';
// CSS is imported globally or via MainLayout, but we keep Home specific tweaks if any
// import './Home.css'; 

function Home() {
    const [viewMode, setViewMode] = useState('grid');
    const [sortBy, setSortBy] = useState('최신순');
    const [filter, setFilter] = useState('MY'); // 'MY' or 'ALL'
    const [openMenuId, setOpenMenuId] = useState(null);
    const [notebooks, setNotebooks] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    // Rename Modal State
    const [renameModalOpen, setRenameModalOpen] = useState(false);
    const [renamingNotebook, setRenamingNotebook] = useState(null);
    const [newName, setNewName] = useState('');

    const menuRef = useRef(null);
    const navigate = useNavigate();
    const { isAdmin, isAuthenticated } = useAuth();
    const { showAlert, showConfirm } = useAlert();

    // 워크스페이스 목록 불러오기
    useEffect(() => {
        // Wait for auth check to complete (handled by route protection usually, but useAuth helps)
        if (!isAuthenticated) return;

        const fetchWorkspaces = async () => {
            try {
                setLoading(true);
                let params = { filter };

                if (isAdmin) {
                    const selectedDomainId = localStorage.getItem('admin_selected_domain_id');
                    if (selectedDomainId) {
                        params.domainId = selectedDomainId;
                    }
                }

                const data = await workspaceApi.getAll(params);
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
    }, [isAuthenticated, isAdmin, navigate, filter]); // dependencies updated

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

        const confirmed = await showConfirm('정말 삭제하시겠습니까?');
        if (!confirmed) {
            return;
        }

        try {
            await workspaceApi.delete(notebookId);
            setNotebooks(prev => prev.filter(nb => nb.id !== notebookId));
            setOpenMenuId(null);
        } catch (err) {
            console.error('삭제 실패:', err);
            showAlert('삭제에 실패했습니다.');
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
            showAlert('워크스페이스 이름을 입력해주세요.');
            return;
        }

        try {
            const updated = await workspaceApi.update(renamingNotebook.id, {
                ...renamingNotebook,
                name: newName.trim()
            });

            setNotebooks(prev => prev.map(nb =>
                nb.id === updated.id ? { ...nb, name: updated.name, title: updated.name } : nb
            ));

            setRenameModalOpen(false);
            setRenamingNotebook(null);
            setNewName('');
        } catch (err) {
            console.error('이름 변경 실패:', err);
            showAlert('이름 변경에 실패했습니다.');
        }
    };

    const handleCreateNew = async () => {
        try {
            let selectedDomainId = null;
            if (isAdmin) {
                selectedDomainId = localStorage.getItem('admin_selected_domain_id');
                if (!selectedDomainId) {
                    showAlert("도메인을 선택해야 합니다."); // Should be redirected already but safety check
                    return;
                }
            }

            const newWorkspace = {
                name: 'Untitled notebook',
                description: '',
                icon: '📄',
                color: 'yellow',
                domainId: selectedDomainId ? parseInt(selectedDomainId) : null,
                isShared: filter === 'ALL' && isAdmin ? true : false // If creating in "All" view as Admin, make it shared? Optional logic.
            };

            const created = await workspaceApi.create(newWorkspace);
            setNotebooks(prev => [created, ...prev]);
            navigate(`/notebook/${created.id}`, { state: { openAddSource: true } });
        } catch (err) {
            console.error('워크스페이스 생성 실패:', err);
            showAlert('워크스페이스 생성에 실패했습니다.');
        }
    };

    const handleNotebookClick = (id) => {
        navigate(`/notebook/${id}`);
    };

    return (
        <div className="home-container">
            <div className="toolbar">
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                    <h1 className="page-title">워크스페이스</h1>
                </div>


                <div className="toolbar-actions">
                    <div className="view-toggle">
                        <button
                            className={`view-btn ${viewMode === 'grid' ? 'active' : ''}`}
                            onClick={() => setViewMode('grid')}
                            title="그리드 보기"
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
                            title="리스트 보기"
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
                        style={{ marginTop: '10px', padding: '8px 16px', cursor: 'pointer' }}
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
                            className={`notebook-card ${notebook.color || 'yellow'}`}
                            onClick={() => handleNotebookClick(notebook.id)}
                            style={{ position: 'relative' }}
                        >
                            <div className="card-header">
                                <div className="notebook-icon">{notebook.icon || '📄'}</div>
                                {notebook.isShared && (
                                    <span style={{
                                        position: 'absolute',
                                        top: '8px',
                                        right: viewMode === 'grid' ? '40px' : 'auto', // Adjust based on menu btn
                                        left: viewMode === 'list' ? '40px' : 'auto',
                                        fontSize: '10px',
                                        background: '#e0f2f1',
                                        color: '#00695c',
                                        padding: '2px 6px',
                                        borderRadius: '4px',
                                        border: '1px solid #b2dfdb'
                                    }}>
                                        Shared
                                    </span>
                                )}

                                {viewMode === 'grid' && (
                                    // Only show menu if Owner
                                    (notebook.role === 'Owner') && (
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
                                                        <Edit2 size={14} />
                                                        <span>제목 수정</span>
                                                    </button>
                                                    <button
                                                        className="menu-item delete"
                                                        onClick={(e) => handleDelete(e, notebook.id)}
                                                    >
                                                        <Trash2 size={14} />
                                                        <span>삭제</span>
                                                    </button>
                                                </div>
                                            )}
                                        </div>
                                    )
                                )}
                            </div>
                            <div className="card-body">
                                {viewMode === 'list' && <div className="notebook-icon">{notebook.icon || '📄'}</div>}
                                <h3 className="notebook-title">{notebook.name || notebook.title || 'Untitled'}</h3>
                                <p className="notebook-source">소스 {notebook.documentCount || 0}개</p>
                                <p className="notebook-date">{notebook.date || '2025. 12. 28.'}</p>
                                <p className="notebook-role">{notebook.role || 'Owner'}</p>

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
                                                    <Edit2 size={14} />
                                                    <span>제목 수정</span>
                                                </button>
                                                <button
                                                    className="menu-item delete"
                                                    onClick={(e) => handleDelete(e, notebook.id)}
                                                >
                                                    <Trash2 size={14} />
                                                    <span>삭제</span>
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
        </div>
    );
}

export default Home;
