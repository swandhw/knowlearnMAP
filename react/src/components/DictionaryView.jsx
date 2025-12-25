import { useState, useEffect } from 'react';
import './DictionaryView.css';

function DictionaryView({ workspaceId }) {
    const [viewMode, setViewMode] = useState('concept'); // 'concept' or 'relation'
    const [selectedCategory, setSelectedCategory] = useState({ id: 'All', name: 'All', label: '전체' });
    const [searchTerm, setSearchTerm] = useState('');
    const [loading, setLoading] = useState(false);

    const [categories, setCategories] = useState([{ id: 'All', name: 'All', label: '전체' }]);

    // Modal & Edit State
    const [isEditModalOpen, setIsEditModalOpen] = useState(false);
    const [editingTerm, setEditingTerm] = useState(null);

    const [terms, setTerms] = useState([]);
    const [relations, setRelations] = useState([]);



    const API_URL = import.meta.env.VITE_APP_API_URL || 'http://localhost:8080';

    useEffect(() => {
        if (workspaceId) {
            fetchData();
            fetchCategories();
        }
    }, [workspaceId, viewMode]);

    const fetchCategories = async () => {
        try {
            const endpoint = viewMode === 'concept' ? 'concepts/categories' : 'relations/categories';
            const response = await fetch(`${API_URL}/api/dictionary/${endpoint}?workspaceId=${workspaceId}`);
            if (!response.ok) throw new Error('Failed to fetch categories');
            const data = await response.json();

            const dynamicCategories = data.map(cat => ({
                id: cat,
                name: cat,
                label: cat
            }));

            setCategories([{ id: 'All', name: 'All', label: '전체' }, ...dynamicCategories]);
        } catch (error) {
            console.error("Category fetch error:", error);
        }
    };



    const fetchData = async () => {
        setLoading(true);
        try {
            const endpoint = viewMode === 'concept' ? 'concepts' : 'relations';
            const response = await fetch(`${API_URL}/api/dictionary/${endpoint}?workspaceId=${workspaceId}`);
            if (!response.ok) throw new Error('Failed to fetch dictionary data');
            const data = await response.json();

            if (viewMode === 'concept') {
                setTerms(data);
            } else {
                setRelations(data);
            }
        } catch (error) {
            console.error("Dictionary fetch error:", error);
            alert("데이터를 불러오는데 실패했습니다.");
        } finally {
            setLoading(false);
        }
    };

    const currentData = (viewMode === 'concept' ? terms : relations).filter(item => {
        // Category Filter
        if (selectedCategory.id !== 'All' && item.category !== selectedCategory.id) return false;

        // Search Filter
        if (searchTerm) {
            const lowerTerm = searchTerm.toLowerCase();
            return (
                (item.label && item.label.toLowerCase().includes(lowerTerm)) ||
                (item.labelEn && item.labelEn.toLowerCase().includes(lowerTerm)) ||
                (item.synonym && item.synonym.toLowerCase().includes(lowerTerm)) ||
                (item.description && item.description.toLowerCase().includes(lowerTerm))
            );
        }
        return true;
    });

    const listTitle = viewMode === 'concept' ? '사전 항목 목록' : '관계 항목 목록';

    // Handlers
    const handleDelete = async (id) => {
        if (window.confirm('정말 삭제하시겠습니까?')) {
            try {
                const endpoint = viewMode === 'concept' ? 'concepts' : 'relations';
                const response = await fetch(`${API_URL}/api/dictionary/${endpoint}/${id}`, {
                    method: 'DELETE'
                });

                if (!response.ok) throw new Error('Failed to delete item');

                if (viewMode === 'concept') {
                    setTerms(prev => prev.filter(t => t.id !== id));
                } else {
                    setRelations(prev => prev.filter(r => r.id !== id));
                }
            } catch (error) {
                console.error("Delete error:", error);
                alert("삭제에 실패했습니다.");
            }
        }
    };

    const handleEditClick = (term) => {
        setEditingTerm({ ...term });
        setIsEditModalOpen(true);
    };

    const handleModalChange = (field, value) => {
        setEditingTerm(prev => ({ ...prev, [field]: value }));
    };

    const handleSaveEdit = async () => {
        try {
            const endpoint = viewMode === 'concept' ? 'concepts' : 'relations';
            const response = await fetch(`${API_URL}/api/dictionary/${endpoint}/${editingTerm.id}`, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(editingTerm)
            });

            if (!response.ok) throw new Error('Failed to update item');

            const updatedItem = await response.json();

            if (viewMode === 'concept') {
                setTerms(prev => prev.map(t => t.id === updatedItem.id ? updatedItem : t));
            } else {
                setRelations(prev => prev.map(r => r.id === updatedItem.id ? updatedItem : r));
            }
            setIsEditModalOpen(false);
            setEditingTerm(null);
            alert("저장되었습니다.");
        } catch (error) {
            console.error("Update error:", error);
            alert("수정에 실패했습니다.");
        }
    };

    return (
        <div className="dictionary-view">
            <div className="dictionary-content-wrapper">
                {/* Left Sidebar: Categories */}
                <div className="dictionary-sidebar">
                    <div className="sidebar-tabs">
                        <button
                            className={`sidebar-tab ${viewMode === 'concept' ? 'active' : ''}`}
                            onClick={() => setViewMode('concept')}
                        >
                            개념
                        </button>
                        <button
                            className={`sidebar-tab ${viewMode === 'relation' ? 'active' : ''}`}
                            onClick={() => setViewMode('relation')}
                        >
                            관계
                        </button>
                    </div>

                    <div className="category-list-container">
                        <h3 className="category-header">카테고리 목록</h3>
                        <div className="category-list">
                            {categories.map(cat => (
                                <button
                                    key={cat.id}
                                    className={`category-item ${selectedCategory.id === cat.id ? 'active' : ''}`}
                                    onClick={() => setSelectedCategory(cat)}
                                >
                                    {cat.name} ({cat.label})
                                </button>
                            ))}
                        </div>
                    </div>
                </div>

                {/* Main Content: Term Table */}
                <div className="dictionary-main">
                    <div className="dictionary-search-bar">
                        <input
                            type="text"
                            placeholder="텍스트 입력"
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                        />
                        <svg className="search-icon" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                            <circle cx="11" cy="11" r="8"></circle>
                            <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                        </svg>
                    </div>

                    <div className="term-list-header">
                        <h3>{listTitle} ({currentData.length}개)</h3>
                    </div>

                    <div className="term-table-container">
                        {loading ? (
                            <div style={{ padding: '20px', textAlign: 'center' }}>로딩 중...</div>
                        ) : (
                            <table className="term-table">
                                <thead>
                                    <tr>
                                        <th>라벨(EN)</th>
                                        <th>라벨(KR)</th>
                                        <th>유의어</th>
                                        <th>설명</th>
                                        <th>상태</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {currentData.map(term => (
                                        <tr key={term.id}>
                                            <td>{term.labelEn}</td>
                                            <td>{term.label}</td>
                                            <td>{term.synonym}</td>
                                            <td className="desc-cell">{term.description || term.desc}</td>
                                            <td className="status-cell">
                                                <div className="action-buttons">
                                                    <button
                                                        className="icon-btn edit"
                                                        title="수정"
                                                        onClick={() => handleEditClick(term)}
                                                    >
                                                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                                            <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path>
                                                            <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path>
                                                        </svg>
                                                    </button>
                                                    <button
                                                        className="icon-btn delete"
                                                        title="삭제"
                                                        onClick={() => handleDelete(term.id)}
                                                    >
                                                        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                                                            <polyline points="3 6 5 6 21 6"></polyline>
                                                            <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                                                        </svg>
                                                    </button>
                                                </div>
                                                <span className={`status-tag ${term.status}`}>{term.status}</span>
                                            </td>
                                        </tr>
                                    ))}
                                    {currentData.length === 0 && (
                                        <tr>
                                            <td colSpan="5" className="empty-table">데이터가 없습니다.</td>
                                        </tr>
                                    )}
                                </tbody>
                            </table>
                        )}
                    </div>
                </div>
            </div>

            {/* Edit Modal */}
            {isEditModalOpen && editingTerm && (
                <div className="edit-modal-overlay">
                    <div className="edit-modal">
                        <h3>항목 수정</h3>
                        <div className="edit-form">
                            <div className="form-group">
                                <label>라벨(KR)</label>
                                <input
                                    type="text"
                                    value={editingTerm.label}
                                    onChange={(e) => handleModalChange('label', e.target.value)}
                                />
                            </div>
                            <div className="form-group">
                                <label>라벨(EN)</label>
                                <input
                                    type="text"
                                    value={editingTerm.labelEn}
                                    onChange={(e) => handleModalChange('labelEn', e.target.value)}
                                />
                            </div>
                            {/* Synonym edit disabled for MVP complexity reduced, or just show readonly */}
                            <div className="form-group">
                                <label>유의어 (수정 불가)</label>
                                <input
                                    type="text"
                                    value={editingTerm.synonym || ''}
                                    readOnly
                                    style={{ backgroundColor: '#f5f5f5' }}
                                />
                            </div>
                            <div className="form-group">
                                <label>설명</label>
                                <textarea
                                    value={editingTerm.description || editingTerm.desc || ''}
                                    onChange={(e) => handleModalChange('description', e.target.value)}
                                    rows="3"
                                />
                            </div>
                        </div>
                        <div className="modal-buttons">
                            <button className="cancel-btn" onClick={() => setIsEditModalOpen(false)}>취소</button>
                            <button className="save-btn" onClick={handleSaveEdit}>저장</button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}

export default DictionaryView;
