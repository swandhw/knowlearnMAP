import { useState, useEffect } from 'react';
import './DictionaryView.css';
import { API_URL } from '../config/api';
import { ChevronLeft, Menu, Search } from 'lucide-react';

function DictionaryView({ workspaceId, initialSelectedDocIds = [], onUpdate }) {
    const [viewMode, setViewMode] = useState('concept'); // 'concept' or 'relation'
    const [selectedCategory, setSelectedCategory] = useState({ id: 'All', name: 'All', label: '전체' });
    const [searchTerm, setSearchTerm] = useState('');
    const [loading, setLoading] = useState(false);

    const [categories, setCategories] = useState([{ id: 'All', name: 'All', label: '전체' }]);

    // Modal & Edit State
    const [isEditModalOpen, setIsEditModalOpen] = useState(false);
    const [editingTerm, setEditingTerm] = useState(null);

    const [currentPage, setCurrentPage] = useState(1);
    const ITEMS_PER_PAGE = 20;

    const [terms, setTerms] = useState([]);
    const [relations, setRelations] = useState([]);

    const [documents, setDocuments] = useState([]);
    const [selectedDocumentIds, setSelectedDocumentIds] = useState(initialSelectedDocIds);
    const [isDocDropdownOpen, setIsDocDropdownOpen] = useState(false);

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

    // ... existing filter logic ...

    // UI Render Helper
    // ...

    // ...

    // Returning JSX
    // Need to insert Dropdown in the header area (near Search Bar)
    /* 
       Inside <div className="dictionary-search-bar" ...> 
    */

    // ... (context continue)

    // Filter Logic
    const filteredData = (viewMode === 'concept' ? terms : relations).filter(item => {
        // Category Filter
        if (selectedCategory.id !== 'All' && item.category !== selectedCategory.id) return false;

        // Search Filter
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

    // Merge functionality
    const [selectedItemIds, setSelectedItemIds] = useState([]);
    const [isMergeModalOpen, setIsMergeModalOpen] = useState(false);
    const [mergeTargetId, setMergeTargetId] = useState(null);

    const handleCheckboxChange = (id) => {
        setSelectedItemIds(prev => {
            if (prev.includes(id)) return prev.filter(itemId => itemId !== id);
            if (prev.length >= 2) return prev;
            return [...prev, id];
        });
    };

    const [mergeDirection, setMergeDirection] = useState('forward'); // 'forward' (0->1) or 'backward' (1->0)
    const [mergeMode, setMergeMode] = useState('move'); // 'move' (synonym) or 'merge' (delete)

    const handleMergeClick = () => {
        if (selectedItemIds.length !== 2) return;
        setMergeDirection('forward');
        setMergeMode('move');
        setIsMergeModalOpen(true);
    };

    const handleConfirmMerge = async () => {
        if (selectedItemIds.length !== 2) return;

        // Determine Source and Target based on direction
        // forward: source=0, target=1 (Items[0] moves to Items[1]) -> Wait, UI needs to show names
        // Let's rely on explicit Source/Target logic
        const item1 = terms.find(t => t.id === selectedItemIds[0]);
        const item2 = terms.find(t => t.id === selectedItemIds[1]);

        // If forward: Item 1 -> Item 2 (Item 1 is Source)
        // If backward: Item 2 -> Item 1 (Item 2 is Source)
        const sourceId = mergeDirection === 'forward' ? item1.id : item2.id;
        const targetId = mergeDirection === 'forward' ? item2.id : item1.id;

        try {
            const response = await fetch(`${API_URL}/api/dictionary/concepts/merge`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    sourceId,
                    targetId,
                    workspaceId,
                    mode: mergeMode
                })
            });

            if (!response.ok) {
                const errorMsg = await response.text();
                throw new Error(errorMsg || 'Merge failed');
            }

            alert('작업이 완료되었습니다.');
            setIsMergeModalOpen(false);
            setSelectedItemIds([]);
            fetchData(); // Refresh list
            if (onUpdate) onUpdate(); // Trigger sync warning
        } catch (error) {
            console.error(error);
            alert(`오류 발생: ${error.message}`);
        }
    };

    const getMergeCandidateName = (id) => {
        const item = terms.find(t => t.id === id);
        return item ? `${item.label} (${item.labelEn})` : 'Unknown';
    };

    const listTitle = viewMode === 'concept' ? '사전 항목 목록' : '관계 항목 목록';

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
                if (onUpdate) onUpdate(); // Trigger sync warning
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
            if (onUpdate) onUpdate(); // Trigger sync warning
        } catch (error) {
            console.error("Update error:", error);
            alert(`수정 실패: ${error.message}`);
        }
    };

    const [isSidebarOpen, setIsSidebarOpen] = useState(true);
    const [categorySearchTerm, setCategorySearchTerm] = useState('');

    const filteredCategories = categories.filter(cat => {
        if (!categorySearchTerm) return true;

        // Create regex from search term (support * wildcard, anchor start)
        // 1. Split by *
        // 2. Escape regex special chars in each part
        // 3. Join with .*
        // 4. Prepend ^ to ensure "Starts With" behavior by default
        const parts = categorySearchTerm.split('*').map(part => part.replace(/[.+?^${}()|[\]\\]/g, '\\$&'));
        const regexString = '^' + parts.join('.*');
        const regex = new RegExp(regexString, 'i');

        return (
            (cat.name && regex.test(cat.name)) ||
            (cat.label && regex.test(cat.label))
        );
    });

    return (
        <div className="dictionary-view">
            <div className="dictionary-content-wrapper">
                <div className={`dictionary-sidebar ${!isSidebarOpen ? 'collapsed' : ''}`}>
                    <div className="sidebar-tabs">
                        <button className="sidebar-tab active">
                            {viewMode === 'concept' ? '개념 카테고리' : '관계 카테고리'}
                        </button>
                    </div>
                    <div className="category-list-container">
                        <div style={{ padding: '0 0 12px 0' }}>
                            <div className="kg-input-group" style={{ width: '100%' }}>
                                <input
                                    type="text"
                                    placeholder="카테고리 검색..."
                                    value={categorySearchTerm}
                                    onChange={(e) => setCategorySearchTerm(e.target.value)}
                                    className="kg-search-input"
                                    style={{ width: '100%', padding: '8px 30px 8px 8px', fontSize: '13px' }}
                                />
                                <Search className="search-icon" size={14} style={{ position: 'absolute', right: '8px', color: '#9aa0a6' }} />
                            </div>
                        </div>
                        <h4 className="category-header">전체 목록</h4>
                        <div className="category-list">
                            {filteredCategories.map(cat => (
                                <button
                                    key={cat.id}
                                    className={`category-item ${selectedCategory.id === cat.id ? 'active' : ''}`}
                                    onClick={() => { setSelectedCategory(cat); setCurrentPage(1); }}
                                >
                                    {cat.label} ({cat.name})
                                </button>
                            ))}
                        </div>
                    </div>
                </div>

                <div className="dictionary-main">
                    {/* Header Area matching Knowledge Graph Layout */}
                    <div style={{ display: 'flex', alignItems: 'center', marginBottom: '24px', flexShrink: 0 }}>
                        {/* Title / Left Controls Area */}
                        <div style={{ display: 'flex', alignItems: 'center', marginRight: '16px' }}>
                            <button
                                className="sidebar-toggle-btn"
                                onClick={() => setIsSidebarOpen(!isSidebarOpen)}
                                title={isSidebarOpen ? "카테고리 접기" : "카테고리 펼치기"}
                                style={{
                                    padding: '8px 12px',
                                    border: '1px solid #e0e0e0',
                                    borderRadius: '4px',
                                    background: '#fff',
                                    cursor: 'pointer',
                                    display: 'flex',
                                    alignItems: 'center',
                                    justifyContent: 'center'
                                }}
                            >
                                {isSidebarOpen ? <ChevronLeft size={20} /> : <Menu size={20} />}
                            </button>
                            <h2 style={{ margin: '0 0 0 12px', fontSize: '1.5rem', color: '#202124', alignSelf: 'center' }}>사전</h2>
                        </div>

                        {/* KG-Style Search Controls Group */}
                        <div className="kg-search-controls">
                            <div className="kg-input-group">
                                <input
                                    type="text"
                                    placeholder="텍스트 입력"
                                    value={searchTerm}
                                    onChange={(e) => setSearchTerm(e.target.value)}
                                    className="kg-search-input"
                                />
                            </div>
                            {/* Search Button (Optional, mimicking KG) or just Filter next */}

                            {/* Document Filter Dropdown */}
                            <div style={{ position: 'relative' }}>
                                <button
                                    className="kg-btn secondary"
                                    onClick={() => setIsDocDropdownOpen(!isDocDropdownOpen)}
                                    style={{ display: 'flex', alignItems: 'center', gap: '5px' }}
                                >
                                    <span>문서 필터 ({selectedDocumentIds.length}/{documents.length})</span>
                                    <span style={{ fontSize: '10px' }}>{isDocDropdownOpen ? '▲' : '▼'}</span>
                                </button>

                                {isDocDropdownOpen && (
                                    <div className="kg-dropdown-menu" style={{
                                        position: 'absolute',
                                        top: '100%',
                                        right: 0,
                                        transform: 'none',
                                        zIndex: 1000,
                                        backgroundColor: 'rgb(45, 45, 45)',
                                        border: '1px solid rgb(68, 68, 68)',
                                        borderRadius: '4px',
                                        padding: '8px',
                                        minWidth: '250px',
                                        maxHeight: '300px',
                                        overflowY: 'auto',
                                        boxShadow: '0 4px 12px rgba(0, 0, 0, 0.5)'
                                    }}>
                                        <div style={{ marginBottom: '8px', paddingBottom: '8px', borderBottom: '1px solid rgb(68, 68, 68)' }}>
                                            <label style={{ display: 'flex', alignItems: 'center', color: 'rgb(255, 255, 255)', cursor: 'pointer' }}>
                                                <input
                                                    type="checkbox"
                                                    checked={documents.length > 0 && selectedDocumentIds.length === documents.length}
                                                    onChange={handleSelectAllDocs}
                                                    style={{ marginRight: '8px' }}
                                                />
                                                전체 선택
                                            </label>
                                        </div>
                                        {documents.map(doc => (
                                            <div key={doc.id} style={{ marginBottom: '4px' }}>
                                                <label style={{ display: 'flex', alignItems: 'center', color: 'rgb(238, 238, 238)', fontSize: '13px', cursor: 'pointer' }}>
                                                    <input
                                                        type="checkbox"
                                                        checked={selectedDocumentIds.includes(doc.id)}
                                                        onChange={() => handleDocumentToggle(doc.id)}
                                                        style={{ marginRight: '8px' }}
                                                    />
                                                    <span title={doc.filename} style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', maxWidth: '200px' }}>
                                                        {doc.filename}
                                                    </span>
                                                </label>
                                            </div>
                                        ))}
                                    </div>
                                )}
                            </div>
                        </div>

                        {/* Merge Button logic - kept outside search group but adjacent */}
                        {viewMode === 'concept' && (
                            <div style={{ marginLeft: '8px' }}>
                                <button
                                    onClick={handleMergeClick}
                                    disabled={selectedItemIds.length !== 2}
                                    style={{
                                        padding: '8px 16px',
                                        backgroundColor: selectedItemIds.length === 2 ? '#1976d2' : '#e0e0e0',
                                        color: selectedItemIds.length === 2 ? '#fff' : '#a0a0a0',
                                        border: 'none',
                                        borderRadius: '4px',
                                        cursor: selectedItemIds.length === 2 ? 'pointer' : 'default',
                                        fontWeight: 'bold'
                                    }}
                                >
                                    병합
                                </button>
                            </div>
                        )}

                        <div style={{ flex: '1 1 0%' }}></div>
                    </div>

                    <div className="term-list-header">
                        <div style={{ width: '40px', display: 'flex', justifyContent: 'center' }}>-</div>
                        <div className="header-cell" style={{ flex: 2 }}>용어(KR)</div>
                        <div className="header-cell" style={{ flex: 2 }}>용어(EN)</div>
                        <div className="header-cell" style={{ flex: 3 }}>설명</div>
                        <div className="header-cell" style={{ flex: 2 }}>유의어</div>
                        <div className="header-cell action-column">관리</div>
                    </div>

                    <div className="term-list">
                        {loading ? (
                            <div style={{ padding: '40px', textAlign: 'center', color: '#666' }}>로딩 중...</div>
                        ) : paginatedData.length === 0 ? (
                            <div style={{ padding: '40px', textAlign: 'center', color: '#666' }}>
                                {searchTerm ? '검색 결과가 없습니다.' : '문서를 선택하여 사전 항목을 조회하세요.'}
                            </div>
                        ) : (
                            paginatedData.map(item => (
                                <div key={item.id} className="term-item">
                                    <div style={{ width: '40px', display: 'flex', justifyContent: 'center', alignItems: 'center' }}>
                                        {viewMode === 'concept' && (
                                            <input
                                                type="checkbox"
                                                checked={selectedItemIds.includes(item.id)}
                                                onChange={() => handleCheckboxChange(item.id)}
                                            />
                                        )}
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
                                    <div className="term-cell action-column">
                                        <button className="edit-btn" onClick={() => handleEditClick(item)}>수정</button>
                                        <button className="delete-btn" onClick={() => handleDelete(item.id)}>삭제</button>
                                    </div>
                                </div>
                            ))
                        )}
                    </div>
                </div>
            </div>


            {/* Edit Modal */}
            {
                isEditModalOpen && editingTerm && (
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
                )
            }
            {/* Merge Confirmation Modal */}
            {
                isMergeModalOpen && selectedItemIds.length === 2 && (
                    <div className="edit-modal-overlay">
                        <div className="edit-modal" style={{ width: '600px' }}>
                            <h3>개념 병합 (Concept Merge)</h3>
                            <p style={{ fontSize: '14px', color: '#666', marginBottom: '20px' }}>
                                두 개념을 하나로 합칩니다. 선택하지 않은 개념은 <b>삭제</b>되며,
                                해당 개념의 문서 출처와 관계(Triple)는 선택한 '대표 개념'으로 이동됩니다.
                            </p>

                            <div style={{ display: 'flex', gap: '20px', marginBottom: '20px' }}>
                                {selectedItemIds.map(id => (
                                    <div
                                        key={id}
                                        onClick={() => setMergeTargetId(id)}
                                        style={{
                                            flex: 1,
                                            padding: '15px',
                                            border: mergeTargetId === id ? '2px solid #1976d2' : '1px solid #ddd',
                                            borderRadius: '8px',
                                            cursor: 'pointer',
                                            backgroundColor: mergeTargetId === id ? '#e3f2fd' : '#fff'
                                        }}
                                    >
                                        <div style={{ fontWeight: 'bold', marginBottom: '8px', color: mergeTargetId === id ? '#1976d2' : '#333' }}>
                                            {mergeTargetId === id ? '✅ 대표 개념 (유지)' : '❌ 삭제될 개념'}
                                        </div>
                                        <div style={{ fontSize: '16px', fontWeight: '500' }}>{getMergeCandidateName(id)}</div>
                                    </div>
                                ))}
                            </div>

                            <div className="modal-buttons">
                                <button className="cancel-btn" onClick={() => setIsMergeModalOpen(false)}>취소</button>
                                <button className="save-btn" onClick={handleConfirmMerge} style={{ backgroundColor: '#d32f2f' }}>병합 실행</button>
                            </div>
                        </div>
                    </div>
                )
            }
        </div >
    );
}

export default DictionaryView;
