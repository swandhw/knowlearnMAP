import { useState, useRef, useEffect } from 'react'
import './App.css'
import NotebookDetail from './components/NotebookDetail'

// Mock data
const mockNotebooks = [
  {
    id: 1,
    title: 'Untitled notebook',
    icon: '📄',
    source: '소스 0개',
    date: '2025. 12. 18.',
    role: 'Owner',
    color: 'yellow'
  },
  {
    id: 2,
    title: 'Business Metadata and Human Resources Data',
    icon: '👥',
    source: '소스 5개',
    date: '2025. 12. 11.',
    role: 'Owner',
    color: 'default'
  },
  {
    id: 3,
    title: '주기도문: 떠나하읺 농 화하, 거도의 완핚',
    icon: '🙏',
    source: '소스 1개',
    date: '2025. 12. 12.',
    role: 'Owner',
    color: 'blue'
  },
  {
    id: 4,
    title: '주기도문: 떠나하읺 농 화하, 거도의 완핚',
    icon: '🙏',
    source: '소스 1개',
    date: '2025. 12. 12.',
    role: 'Reader',
    color: 'blue'
  },
  {
    id: 5,
    title: 'IT Project and Resource Metadata Catalog',
    icon: '📊',
    source: '소스 10개',
    date: '2025. 12. 9.',
    role: 'Owner',
    color: 'purple'
  }
];

function App() {
  const [activeTab, setActiveTab] = useState('내노트북');
  const [viewMode, setViewMode] = useState('grid'); // grid or list
  const [sortBy, setSortBy] = useState('최신순');
  const [openMenuId, setOpenMenuId] = useState(null);
  const [selectedNotebook, setSelectedNotebook] = useState(null);
  const menuRef = useRef(null);

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

  const handleDelete = (e, notebookId) => {
    e.stopPropagation();
    console.log('Delete notebook:', notebookId);
    setOpenMenuId(null);
    // TODO: Implement delete functionality
  };

  const handleRename = (e, notebookId) => {
    e.stopPropagation();
    console.log('Rename notebook:', notebookId);
    setOpenMenuId(null);
    // TODO: Implement rename functionality
  };

  // If a notebook is selected, show detail view
  if (selectedNotebook) {
    return (
      <NotebookDetail
        notebook={selectedNotebook}
        onBack={() => setSelectedNotebook(null)}
      />
    );
  }

  return (
    <div className="app">
      {/* Header */}
      <header className="header">
        <div className="header-left">
          <div className="logo">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
              <path d="M12 2L2 7v10c0 5.55 3.84 10.74 9 12 5.16-1.26 9-6.45 9-12V7l-10-5z" />
            </svg>
            <span>KNOWLEARN MAP</span>
          </div>
        </div>

        <div className="header-right">
          <button className="icon-btn">
            <svg width="20" height="20" viewBox="0 0 20 20" fill="currentColor">
              <path d="M15.95 10.78c.03-.25.05-.51.05-.78s-.02-.53-.06-.78l1.69-1.32c.15-.12.19-.34.1-.51l-1.6-2.77c-.1-.18-.31-.24-.49-.18l-1.99.8c-.42-.32-.86-.58-1.35-.78L12 2.34c-.03-.2-.2-.34-.4-.34H8.4c-.2 0-.36.14-.39.34l-.3 2.12c-.49.2-.94.47-1.35.78l-1.99-.8c-.18-.07-.39 0-.49.18l-1.6 2.77c-.1.18-.06.39.1.51l1.69 1.32c-.04.25-.07.52-.07.78s.02.53.06.78L2.37 12.1c-.15.12-.19.34-.1.51l1.6 2.77c.1.18.31.24.49.18l1.99-.8c.42.32.86.58 1.35.78l.3 2.12c.04.2.2.34.4.34h3.2c.2 0 .37-.14.39-.34l.3-2.12c.49-.2.94-.47 1.35-.78l1.99.8c.18.07.39 0 .49-.18l1.6-2.77c.1-.18.06-.39-.1-.51l-1.67-1.32zM10 13c-1.65 0-3-1.35-3-3s1.35-3 3-3 3 1.35 3 3-1.35 3-3 3z" />
            </svg>
          </button>

          <button className="icon-btn">
            <svg width="20" height="20" viewBox="0 0 20 20" fill="currentColor">
              <path d="M10 2C6 2 2.73 4.11 1 7.5 2.73 10.89 6 13 10 13s7.27-2.11 9-5.5C17.27 4.11 14 2 10 2zm0 9a3.5 3.5 0 110-7 3.5 3.5 0 010 7zm0-5.5c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z" />
            </svg>
          </button>

          <button className="icon-btn">
            <svg width="20" height="20" viewBox="0 0 20 20" fill="currentColor">
              <path d="M6 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm8 0c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm-4 4c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm4 0c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm-8 0c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm4 4c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2z" />
            </svg>
          </button>

          <div className="profile-avatar">
            <img src="https://api.dicebear.com/7.x/avataaars/svg?seed=Felix" alt="Profile" />
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="main-content">
        {/* Tab Navigation */}
        <nav className="tab-navigation">
          <button
            className={`tab ${activeTab === '전체' ? 'active' : ''}`}
            onClick={() => setActiveTab('전체')}
          >
            전체
          </button>
          <button
            className={`tab ${activeTab === '내노트북' ? 'active' : ''}`}
            onClick={() => setActiveTab('내노트북')}
          >
            내노트북
          </button>
          <button
            className={`tab ${activeTab === '추천노트북' ? 'active' : ''}`}
            onClick={() => setActiveTab('추천노트북')}
          >
            추천노트북
          </button>
        </nav>

        {/* Toolbar */}
        <div className="toolbar">
          <h1 className="page-title">내노트북</h1>

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

            <button className="new-note-btn">
              <svg width="20" height="20" viewBox="0 0 20 20" fill="currentColor">
                <path d="M10 4v12M4 10h12" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
              </svg>
              새 노트
            </button>
          </div>
        </div>

        {/* Notebooks Grid */}
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
          <div className="notebook-card create-card">
            <div className="create-card-content">
              <div className="create-icon">
                <svg width="48" height="48" viewBox="0 0 48 48" fill="none" stroke="currentColor" strokeWidth="2">
                  <circle cx="24" cy="24" r="20" />
                  <path d="M24 16v16M16 24h16" strokeLinecap="round" />
                </svg>
              </div>
              <span className="create-text">새 노트북 만들기</span>
            </div>
          </div>

          {/* Notebook Cards */}
          {mockNotebooks.map((notebook) => (
            <div
              key={notebook.id}
              className={`notebook-card ${notebook.color}`}
              onClick={() => setSelectedNotebook(notebook)}
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
      </main>
    </div>
  )
}

export default App
