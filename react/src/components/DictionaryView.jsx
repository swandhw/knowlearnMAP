import { useState, useEffect } from 'react';
import './DictionaryView.css';
import { API_URL } from '../config/api';
import { ChevronLeft, Menu, Search, Edit2, Trash2, ArrowRightCircle } from 'lucide-react';

function DictionaryView({ workspaceId, initialSelectedDocIds = [], onUpdate }) {
    const [viewMode, setViewMode] = useState('concept'); // 'concept' or 'relation'
    const [selectedCategory, setSelectedCategory] = useState({ id: 'All', name: 'All', label: '전체' });
    const [searchTerm, setSearchTerm] = useState('');
    const [loading, setLoading] = useState(false);

    const [categories, setCategories] = useState([{ id: 'All', name: 'All', label: '전체' }]);

    // Modal & Edit State
    const [isEditModalOpen, setIsEditModalOpen] = useState(false);
    const [editingTerm, setEditingTerm] = useState(null);

    // [New] Move/Merge State
    const [isMoveModalOpen, setIsMoveModalOpen] = useState(false);
    const [moveSourceItem, setMoveSourceItem] = useState(null);
    const [moveTargetId, setMoveTargetId] = useState(null);
    const [moveSearchTerm, setMoveSearchTerm] = useState('');

    const [currentPage, setCurrentPage] = useState(1);
    const ITEMS_PER_PAGE = 20;

    const [terms, setTerms] = useState([]);
    const [relations, setRelations] = useState([]);

    const [documents, setDocuments] = useState([]);
    const [selectedDocumentIds, setSelectedDocumentIds] = useState(initialSelectedDocIds);
    const [isDocDropdownOpen, setIsDocDropdownOpen] = useState(false);

    // Sync with parent's selection
    useEffect(() => {
        if (initialSelectedDocIds) {
            setSelectedDocumentIds(initialSelectedDocIds);
        }
    }, [initialSelectedDocIds]);

    useEffect(() => {
        if (workspaceId) {
            fetchDocuments();
            fetchData();
            fetchCategories();
        }
    }, [workspaceId, viewMode]); // fetchData called here initially

    // Re-fetch when filter changes
    useEffect(() => {
        if (workspaceId) {
            fetchData();
        }
    }, [selectedDocumentIds]);

    const fetchDocuments = async () => {
        try {
            const response = await fetch(`${API_URL}/api/documents/workspace/${workspaceId}`);
            if (!response.ok) throw new Error('Failed to fetch documents');
            const data = await response.json();
            if (data.success && Array.isArray(data.data)) {
                setDocuments(data.data);
            } else if (Array.isArray(data)) {
                setDocuments(data);
            } else {
                console.warn("Expected array for documents but got:", data);
                setDocuments([]);
            }
        } catch (error) {
            console.error("Error fetching documents:", error);
        }
    };

    const fetchCategories = async () => {
        try {
            const response = await fetch(`${API_URL}/api/dictionary/categories?type=${viewMode}&workspaceId=${workspaceId}`);
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
        // [User Requirement] If no document is selected, show nothing.
        if (selectedDocumentIds.length === 0) {
            setTerms([]);
            setRelations([]);
            setLoading(false);
            return;
        }

        setLoading(true);
        try {
            const endpoint = viewMode === 'concept' ? 'concepts' : 'relations';
            // Request large size to support client-side filtering for now
            let url = `${API_URL}/api/dictionary/${endpoint}?workspaceId=${workspaceId}&page=0&size=2000`;

            if (selectedDocumentIds.length > 0) {
                const queryParams = selectedDocumentIds.map(id => `documentIds=${id}`).join('&');
                url += `&${queryParams}`;
            }

            const response = await fetch(url);
            if (!response.ok) throw new Error('Failed to fetch dictionary data');
            const data = await response.json();

            // Handle Page<Dto> response or List<Dto> (backward compatibility)
            const listData = data.content || data;

            if (viewMode === 'concept') {
                setTerms(listData);
            } else {
                setRelations(listData);
            }
            setCurrentPage(1); // Reset to page 1 on fetch
        } catch (error) {
            console.error("Dictionary fetch error:", error);
            alert("데이터를 불러오는데 실패했습니다.");
        } finally {
            setLoading(false);
        }
    };

    const handleDocumentToggle = (docId) => {
        setSelectedDocumentIds(prev =>
            prev.includes(docId) ? prev.filter(id => id !== docId) : [...prev, docId]
        );
    };

    const handleSelectAllDocs = () => {
        if (selectedDocumentIds.length === documents.length) {
            setSelectedDocumentIds([]);
        } else {
            setSelectedDocumentIds(documents.map(d => d.id));
        }
    };

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
                if (onUpdate) onUpdate();
            } catch (error) {
                console.error("Delete error:", error);
                alert("삭제에 실패했습니다.");
            }
        }
    };

    // --- Move (Merge) Handlers ---
    const handleMoveClick = (item) => {
        setMoveSourceItem(item);
        setMoveTargetId(null);
        setMoveSearchTerm('');
        setIsMoveModalOpen(true);
    };

    const handleConfirmMove = async () => {
        if (!moveSourceItem || !moveTargetId) return;
        try {
            const response = await fetch(`${API_URL}/api/dictionary/concepts/merge`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    sourceId: moveSourceItem.id,
                    targetId: moveTargetId,
                    workspaceId,
                    mode: 'move' // Force move mode
                })
            });

            if (!response.ok) {
                const errorMsg = await response.text();
                throw new Error(errorMsg || 'Move failed');
            }

            alert('이동되었습니다.');
            setIsMoveModalOpen(false);
            setMoveSourceItem(null);
            fetchData(); // Refresh list to reflect merge (source gone)
            if (onUpdate) onUpdate();
        } catch (error) {
            console.error("Move error:", error);
            alert(`이동 실패: ${error.message}`);
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

            if (!response.ok) {
                const errorMsg = await response.text();
                throw new Error(errorMsg || 'Failed to update item');
            }

            const updatedItem = await response.json();

            if (viewMode === 'concept') {
                setTerms(prev => prev.map(t => t.id === updatedItem.id ? updatedItem : t));
            } else {
                setRelations(prev => prev.map(r => r.id === updatedItem.id ? updatedItem : r));
            }
            setIsEditModalOpen(false);
            setEditingTerm(null);
            alert("저장되었습니다.");
            if (onUpdate) onUpdate();
        } catch (error) {
            console.error("Update error:", error);
            alert(`수정 실패: ${error.message}`);
        }
    };

    // ... existing filter logic ...

    // UI Render Helper
    // ...

    // ...

    // New State for Category Header Filter
    const [categoryFilter, setCategoryFilter] = useState('');
    const [showCategorySuggestions, setShowCategorySuggestions] = useState(false);

    // Filter Logic
    const filteredData = (viewMode === 'concept' ? terms : relations).filter(item => {
        // Category Filter (Header Input)
        if (categoryFilter && item.category) {
            if (!item.category.toLowerCase().includes(categoryFilter.toLowerCase())) {
                return false;
            }
        }

        // Search Filter (Global)
        if (searchTerm) {
            const lowerTerm = searchTerm.toLowerCase();
            return (
                (item.label && item.label.toLowerCase().includes(lowerTerm)) ||
                (item.labelEn && item.labelEn.toLowerCase().includes(lowerTerm)) ||
                (item.synonym && item.synonym.toLowerCase().includes(lowerTerm))
            );
        }
        return true;
    });

    // Pagination Logic
    const totalPages = Math.ceil(filteredData.length / ITEMS_PER_PAGE);
    const paginatedData = filteredData.slice(
        (currentPage - 1) * ITEMS_PER_PAGE,
        currentPage * ITEMS_PER_PAGE
    );

    // Render
    return (
        <div className="dictionary-view">
            <div className="dictionary-content-wrapper">
                {/* Remove Sidebar */}

                <div className="dictionary-main" style={{ width: '100%' }}>
                    {/* Header Area */}
                    <div style={{ display: 'flex', alignItems: 'center', marginBottom: '24px', flexShrink: 0 }}>
                        <div style={{ display: 'flex', alignItems: 'center', marginRight: '16px', gap: '16px' }}>
                            <h2 style={{ margin: '0', fontSize: '1.5rem', color: '#202124' }}>사전</h2>

                            {/* View Mode Tabs */}
                            <div style={{ display: 'flex', backgroundColor: '#f1f3f4', borderRadius: '8px', padding: '4px' }}>
                                <button
                                    onClick={() => setViewMode('concept')}
                                    style={{
                                        border: 'none', background: viewMode === 'concept' ? 'white' : 'transparent',
                                        padding: '6px 12px', borderRadius: '6px', fontSize: '13px', fontWeight: '500',
                                        color: viewMode === 'concept' ? '#1a73e8' : '#5f6368', cursor: 'pointer',
                                        boxShadow: viewMode === 'concept' ? '0 1px 2px rgba(0,0,0,0.1)' : 'none'
                                    }}
                                >
                                    개념 (Object)
                                </button>
                                <button
                                    onClick={() => setViewMode('relation')}
                                    style={{
                                        border: 'none', background: viewMode === 'relation' ? 'white' : 'transparent',
                                        padding: '6px 12px', borderRadius: '6px', fontSize: '13px', fontWeight: '500',
                                        color: viewMode === 'relation' ? '#1a73e8' : '#5f6368', cursor: 'pointer',
                                        boxShadow: viewMode === 'relation' ? '0 1px 2px rgba(0,0,0,0.1)' : 'none'
                                    }}
                                >
                                    관계 (Relation)
                                </button>
                            </div>
                        </div>

                        {/* Search Controls */}
                        <div className="kg-search-controls">
                            <div className="kg-input-group">
                                <Search size={16} style={{ position: 'absolute', left: '10px', color: '#999' }} />
                                <input
                                    type="text"
                                    placeholder={viewMode === 'concept' ? "용어 검색..." : "관계 검색..."}
                                    value={searchTerm}
                                    onChange={(e) => setSearchTerm(e.target.value)}
                                    className="kg-search-input"
                                    style={{ paddingLeft: '32px' }}
                                />
                            </div>

                            {/* Document Filter */}
                            <div style={{ position: 'relative' }}>
                                <button
                                    className="kg-btn secondary"
                                    onClick={() => setIsDocDropdownOpen(!isDocDropdownOpen)}
                                    style={{ display: 'flex', alignItems: 'center', gap: '5px' }}
                                >
                                    <span>문서 필터 ({selectedDocumentIds.length}/{documents.length})</span>
                                    <span style={{ fontSize: '10px' }}>{isDocDropdownOpen ? '▲' : '▼'}</span>
                                </button>
                                {/* ... Dropdown Content (kept same) ... */}
                                {isDocDropdownOpen && (
                                    <div className="kg-dropdown-menu" style={{
                                        position: 'absolute', top: '100%', right: 0, zIndex: 1000,
                                        backgroundColor: '#2d2d2d', border: '1px solid #444',
                                        padding: '8px', minWidth: '250px', maxHeight: '300px', overflowY: 'auto'
                                    }}>
                                        <div style={{ marginBottom: '8px', paddingBottom: '8px', borderBottom: '1px solid #444' }}>
                                            <label style={{ display: 'flex', alignItems: 'center', color: '#fff', cursor: 'pointer' }}>
                                                <input type="checkbox" checked={documents.length > 0 && selectedDocumentIds.length === documents.length} onChange={handleSelectAllDocs} style={{ marginRight: '8px' }} />
                                                전체 선택
                                            </label>
                                        </div>
                                        {documents.map(doc => (
                                            <div key={doc.id} style={{ marginBottom: '4px' }}>
                                                <label style={{ display: 'flex', alignItems: 'center', color: '#eee', fontSize: '13px', cursor: 'pointer' }}>
                                                    <input type="checkbox" checked={selectedDocumentIds.includes(doc.id)} onChange={() => handleDocumentToggle(doc.id)} style={{ marginRight: '8px' }} />
                                                    <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', maxWidth: '200px' }} title={doc.filename}>{doc.filename}</span>
                                                </label>
                                            </div>
                                        ))}
                                    </div>
                                )}
                            </div>
                        </div>
                        <div style={{ flex: '1 1 0%' }}></div>
                    </div>

                    <div className="term-list-header">
                        {/* 1. Category Column with Search */}
                        <div className="header-cell" style={{ flex: 1.5, position: 'relative' }}>
                            <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                                <span>카테고리</span>
                                <input
                                    type="text"
                                    value={categoryFilter}
                                    placeholder="🔍"
                                    onChange={(e) => {
                                        setCategoryFilter(e.target.value);
                                        setShowCategorySuggestions(true);
                                    }}
                                    onFocus={() => setShowCategorySuggestions(true)}
                                    onBlur={() => setTimeout(() => setShowCategorySuggestions(false), 200)}
                                    style={{
                                        width: '80px', fontSize: '12px', padding: '2px 4px',
                                        border: '1px solid #ddd', borderRadius: '4px', marginLeft: '6px'
                                    }}
                                />
                            </div>
                            {/* Autocomplete Suggestions */}
                            {showCategorySuggestions && (
                                <div style={{
                                    position: 'absolute', top: '100%', left: 0, zIndex: 10,
                                    backgroundColor: 'white', border: '1px solid #ddd',
                                    boxShadow: '0 2px 8px rgba(0,0,0,0.1)', borderRadius: '4px',
                                    maxHeight: '200px', overflowY: 'auto', width: '150px'
                                }}>
                                    {categories
                                        .filter(c => c.id !== 'All' && c.label.toLowerCase().includes(categoryFilter.toLowerCase()))
                                        .map(c => (
                                            <div
                                                key={c.id}
                                                style={{ padding: '6px 12px', fontSize: '12px', cursor: 'pointer', borderBottom: '1px solid #f0f0f0' }}
                                                onClick={() => setCategoryFilter(c.label)}
                                                className="suggestion-item"
                                            >
                                                {c.label}
                                            </div>
                                        ))}
                                </div>
                            )}
                        </div>

                        <div className="header-cell" style={{ flex: 2 }}>{viewMode === 'concept' ? '용어(KR)' : '관계(KR)'}</div>
                        <div className="header-cell" style={{ flex: 2 }}>{viewMode === 'concept' ? '용어(EN)' : '관계(EN)'}</div>
                        <div className="header-cell" style={{ flex: 3 }}>설명</div>
                        <div className="header-cell" style={{ flex: 2 }}>유의어</div>
                        <div className="header-cell action-column" style={{ justifyContent: 'center' }}>관리</div>
                    </div>

                    <div className="term-list">
                        {loading ? (
                            <div style={{ padding: '40px', textAlign: 'center', color: '#666' }}>로딩 중...</div>
                        ) : paginatedData.length === 0 ? (
                            <div style={{ padding: '40px', textAlign: 'center', color: '#666' }}>
                                {searchTerm || categoryFilter ? '검색 결과가 없습니다.' : '문서를 선택하여 사전 항목을 조회하세요.'}
                            </div>
                        ) : (
                            paginatedData.map(item => (
                                <div key={item.id} className="term-item">
                                    {/* Category Cell */}
                                    <div className="term-cell" style={{ flex: 1.5, color: '#555', fontSize: '13px' }}>
                                        <span style={{ backgroundColor: '#e9ecef', padding: '2px 8px', borderRadius: '12px' }}>
                                            {item.category}
                                        </span>
                                    </div>
                                    <div className="term-cell" style={{ flex: 2, fontWeight: 'bold' }}>{item.label}</div>
                                    <div className="term-cell" style={{ flex: 2, color: '#555' }}>{item.labelEn}</div>
                                    <div className="term-cell" style={{ flex: 3, fontSize: '0.9em', color: '#666' }}>
                                        {item.description && item.description.length > 50
                                            ? item.description.substring(0, 50) + '...'
                                            : item.description}
                                    </div>
                                    <div className="term-cell" style={{ flex: 2, color: '#888', fontStyle: 'italic' }}>
                                        {item.synonym || '-'}
                                    </div>
                                    <div className="term-cell action-column" style={{ justifyContent: 'center', gap: '8px' }}>
                                        <button
                                            className="icon-btn edit-btn"
                                            onClick={() => handleEditClick(item)}
                                            title="수정"
                                            style={{ background: 'none', border: 'none', cursor: 'pointer', padding: '4px', color: '#1976d2' }}
                                        >
                                            <Edit2 size={16} />
                                        </button>

                                        {/* If Concept: Show Move. If Relation: Show Delete (or nothing if strict) */}
                                        {viewMode === 'concept' ? (
                                            <button
                                                className="icon-btn move-btn"
                                                onClick={() => handleMoveClick(item)}
                                                title="이동 (병합)"
                                                style={{ background: 'none', border: 'none', cursor: 'pointer', padding: '4px', color: '#00796b' }}
                                            >
                                                <ArrowRightCircle size={16} />
                                            </button>
                                        ) : (
                                            <button
                                                className="icon-btn delete-btn"
                                                onClick={() => handleDelete(item.id)}
                                                title="삭제"
                                                style={{ background: 'none', border: 'none', cursor: 'pointer', padding: '4px', color: '#d32f2f' }}
                                            >
                                                <Trash2 size={16} />
                                            </button>
                                        )}
                                    </div>
                                </div>
                            ))
                        )}
                    </div>
                </div>
            </div>

            {/* Edit Modal (kept same) */}
            {isEditModalOpen && editingTerm && (
                /* ... existing modal code ... */
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

            {/* Move (Merge) Modal */}
            {isMoveModalOpen && moveSourceItem && (
                <div className="edit-modal-overlay">
                    <div className="edit-modal" style={{ width: '500px' }}>
                        <h3>개념 이동 (병합)</h3>
                        <p style={{ color: '#666', fontSize: '13px', marginBottom: '16px' }}>
                            <b>'{moveSourceItem.label}'</b> 개념을 다른 개념으로 이동(병합)합니다. <br />
                            이동 후 원본 개념은 삭제되며, 모든 문서 출처와 관계가 대상 개념으로 이전됩니다.
                        </p>

                        <div style={{ marginBottom: '16px' }}>
                            <label style={{ display: 'block', marginBottom: '8px', fontWeight: 'bold' }}>이동할 대상 개념 검색</label>
                            <input
                                type="text"
                                placeholder="대상 개념 검색..."
                                value={moveSearchTerm}
                                onChange={(e) => setMoveSearchTerm(e.target.value)}
                                style={{ width: '100%', padding: '8px', marginBottom: '8px', border: '1px solid #ddd', borderRadius: '4px' }}
                            />
                            <div style={{
                                maxHeight: '200px', overflowY: 'auto', border: '1px solid #ddd', borderRadius: '4px'
                            }}>
                                {terms
                                    .filter(t => t.id !== moveSourceItem.id && (
                                        t.label.toLowerCase().includes(moveSearchTerm.toLowerCase()) ||
                                        (t.labelEn && t.labelEn.toLowerCase().includes(moveSearchTerm.toLowerCase()))
                                    ))
                                    .slice(0, 50) // Limit displayed results
                                    .map(t => (
                                        <div
                                            key={t.id}
                                            onClick={() => setMoveTargetId(t.id)}
                                            style={{
                                                padding: '8px', cursor: 'pointer', borderBottom: '1px solid #f0f0f0',
                                                backgroundColor: moveTargetId === t.id ? '#e3f2fd' : 'white'
                                            }}
                                        >
                                            <div style={{ fontWeight: 'bold' }}>{t.label}</div>
                                            <div style={{ fontSize: '12px', color: '#666' }}>{t.labelEn}</div>
                                        </div>
                                    ))}
                                {terms.filter(t => t.id !== moveSourceItem.id && (
                                    t.label.toLowerCase().includes(moveSearchTerm.toLowerCase()) ||
                                    (t.labelEn && t.labelEn.toLowerCase().includes(moveSearchTerm.toLowerCase()))
                                )).length === 0 && (
                                        <div style={{ padding: '12px', textAlign: 'center', color: '#999', fontSize: '13px' }}>검색 결과 없음</div>
                                    )}
                            </div>
                        </div>

                        <div className="modal-buttons">
                            <button className="cancel-btn" onClick={() => setIsMoveModalOpen(false)}>취소</button>
                            <button
                                className="save-btn"
                                onClick={handleConfirmMove}
                                disabled={!moveTargetId}
                                style={{ backgroundColor: moveTargetId ? '#1976d2' : '#ccc', cursor: moveTargetId ? 'pointer' : 'not-allowed' }}
                            >
                                이동
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}

// ... export default ...

export default DictionaryView;
