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

    const [currentPage, setCurrentPage] = useState(1);
    const ITEMS_PER_PAGE = 20;

    const [terms, setTerms] = useState([]);
    const [relations, setRelations] = useState([]);



    const API_URL = import.meta.env.VITE_APP_API_URL || 'http://localhost:8080';

    const [documents, setDocuments] = useState([]);
    const [selectedDocumentIds, setSelectedDocumentIds] = useState([]);
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
            if (Array.isArray(data)) {
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

    const handleMergeClick = () => {
        if (selectedItemIds.length !== 2) return;
        setMergeTargetId(selectedItemIds[0]);
        setIsMergeModalOpen(true);
    };

    const handleConfirmMerge = async () => {
        if (!mergeTargetId || selectedItemIds.length !== 2) return;

        const sourceId = selectedItemIds.find(id => id !== mergeTargetId);

        try {
            const response = await fetch(`${API_URL}/api/dictionary/concepts/merge`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ sourceId, targetId: mergeTargetId, workspaceId })
            });

            if (!response.ok) throw new Error('Merge failed');

            alert('병합이 완료되었습니다.');
            setIsMergeModalOpen(false);
            setSelectedItemIds([]);
            fetchData(); // Refresh list
        } catch (error) {
            console.error(error);
            alert('병합 중 오류가 발생했습니다.');
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

    const [isSidebarOpen, setIsSidebarOpen] = useState(true);

    return (
        <div className="dictionary-view">
            {/* ... */}
            {/* Main Content: Term Table */}
            <div className="dictionary-main">
                <div className="dictionary-search-bar" style={{ display: 'flex', gap: '10px' }}>
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
                        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                            {isSidebarOpen ? (
                                <polyline points="15 18 9 12 15 6"></polyline>
                            ) : (
                                <polyline points="9 18 15 12 9 6"></polyline>
                            )}
                        </svg>
                    </button>

                    {/* Document Filter Dropdown */}
                    <div style={{ position: 'relative' }}>
                        <button
                            style={{
                                height: '100%',
                                padding: '0 12px',
                                border: '1px solid #e0e0e0',
                                borderRadius: '4px',
                                background: '#fff',
                                cursor: 'pointer',
                                display: 'flex',
                                alignItems: 'center',
                                gap: '5px',
                                fontSize: '14px'
                            }}
                            onClick={() => setIsDocDropdownOpen(!isDocDropdownOpen)}
                        >
                            <span>문서 필터 ({selectedDocumentIds.length}/{documents.length})</span>
                            <span style={{ fontSize: '10px' }}>{isDocDropdownOpen ? '▲' : '▼'}</span>
                        </button>

                        {isDocDropdownOpen && (
                            <div style={{
                                position: 'absolute',
                                top: '100%',
                                left: 0,
                                zIndex: 1000,
                                backgroundColor: '#fff',
                                border: '1px solid #ccc',
                                borderRadius: '4px',
                                padding: '8px',
                                minWidth: '250px',
                                maxHeight: '300px',
                                overflowY: 'auto',
                                boxShadow: '0 4px 12px rgba(0,0,0,0.15)',
                                marginTop: '4px'
                            }}>
                                <div style={{ marginBottom: '8px', paddingBottom: '8px', borderBottom: '1px solid #eee' }}>
                                    <label style={{ display: 'flex', alignItems: 'center', cursor: 'pointer' }}>
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
                                        <label style={{ display: 'flex', alignItems: 'center', fontSize: '13px', cursor: 'pointer' }}>
                                            <input
                                                type="checkbox"
                                                checked={selectedDocumentIds.includes(doc.id)}
                                                onChange={() => handleDocumentToggle(doc.id)}
                                                style={{ marginRight: '8px' }}
                                            />
                                            <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', maxWidth: '200px' }} title={doc.filename}>
                                                {doc.filename}
                                            </span>
                                        </label>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>

                    <div style={{ position: 'relative', flex: 1 }}>
                        <input
                            type="text"
                            placeholder="텍스트 입력"
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                            style={{ width: '100%', padding: '10px 40px 10px 16px', border: '1px solid #e0e0e0', borderRadius: '4px', fontSize: '14px' }}
                        />
                        <svg className="search-icon" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                            <circle cx="11" cy="11" r="8"></circle>
                            <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                        </svg>
                    </div>
                    {/* Merge Button logic */}
                    {viewMode === 'concept' && (
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
                    )}
                </div>

                <div className="term-list-header">
                    <h3>{listTitle} (총 {filteredData.length}개)</h3>
                </div>

                <div className="term-table-container">
                    {loading ? (
                        <div style={{ padding: '20px', textAlign: 'center' }}>로딩 중...</div>
                    ) : (
                        <>
                            <table className="term-table">
                                <thead>
                                    <tr>
                                        {viewMode === 'concept' && <th style={{ width: '40px' }}>선택</th>}
                                        <th>라벨(EN)</th>
                                        <th>라벨(KR)</th>
                                        <th>유의어</th>
                                        <th>설명</th>
                                        <th>상태</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {paginatedData.map(term => (
                                        <tr key={term.id}>
                                            {viewMode === 'concept' && (
                                                <td>
                                                    <input
                                                        type="checkbox"
                                                        checked={selectedItemIds.includes(term.id)}
                                                        onChange={() => handleCheckboxChange(term.id)}
                                                        disabled={!selectedItemIds.includes(term.id) && selectedItemIds.length >= 2}
                                                    />
                                                </td>
                                            )}
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
                            {/* Pagination Controls */}
                            {totalPages > 0 && (
                                <div style={{ display: 'flex', justifyContent: 'center', marginTop: '16px', gap: '8px' }}>
                                    <button
                                        onClick={() => setCurrentPage(prev => Math.max(prev - 1, 1))}
                                        disabled={currentPage === 1}
                                        style={{
                                            border: '1px solid #ddd',
                                            background: currentPage === 1 ? '#f5f5f5' : '#fff',
                                            cursor: currentPage === 1 ? 'default' : 'pointer',
                                            padding: '4px 12px',
                                            borderRadius: '4px'
                                        }}
                                    >
                                        &lt;
                                    </button>
                                    {Array.from({ length: totalPages }, (_, i) => i + 1).map(page => (
                                        <button
                                            key={page}
                                            onClick={() => setCurrentPage(page)}
                                            style={{
                                                border: '1px solid #ddd',
                                                background: currentPage === page ? '#1976d2' : '#fff',
                                                color: currentPage === page ? '#fff' : '#333',
                                                cursor: 'pointer',
                                                padding: '4px 12px',
                                                borderRadius: '4px'
                                            }}
                                        >
                                            {page}
                                        </button>
                                    ))}
                                    <button
                                        onClick={() => setCurrentPage(prev => Math.min(prev + 1, totalPages))}
                                        disabled={currentPage === totalPages}
                                        style={{
                                            border: '1px solid #ddd',
                                            background: currentPage === totalPages ? '#f5f5f5' : '#fff',
                                            cursor: currentPage === totalPages ? 'default' : 'pointer',
                                            padding: '4px 12px',
                                            borderRadius: '4px'
                                        }}
                                    >
                                        &gt;
                                    </button>
                                </div>
                            )}
                        </>
                    )}
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
            {isMergeModalOpen && selectedItemIds.length === 2 && (
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
            )}
        </div >
    );
}

export default DictionaryView;
