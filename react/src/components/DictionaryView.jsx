import { useState, useEffect, useCallback } from 'react';
import './DictionaryView.css';
import { dictionaryApi } from '../services/api';
import { useAlert } from '../context/AlertContext';
import { Menu, Search, Edit2, Trash2, ArrowRightCircle, ChevronLeft, ChevronRight } from 'lucide-react';

function DictionaryView({ workspaceId, initialSelectedDocIds = [], onUpdate, readOnly }) {
    const { showAlert } = useAlert();
    const [viewMode, setViewMode] = useState('concept'); // 'concept' or 'relation'
    const [selectedCategory, setSelectedCategory] = useState({ id: 'All', name: 'All', label: '전체' });
    const [data, setData] = useState([]);
    const [categories, setCategories] = useState(['All']);
    const [loading, setLoading] = useState(false);
    const [searchTerm, setSearchTerm] = useState('');

    // Pagination
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [totalElements, setTotalElements] = useState(0);
    const pageSize = 20;

    // Modals
    const [isEditModalOpen, setIsEditModalOpen] = useState(false);
    const [editingTerm, setEditingTerm] = useState(null);
    const [isMoveModalOpen, setIsMoveModalOpen] = useState(false);
    const [moveSourceItem, setMoveSourceItem] = useState(null);
    const [moveSearchTerm, setMoveSearchTerm] = useState('');
    const [moveTargetId, setMoveTargetId] = useState(null);

    // Fetch Categories
    const fetchCategories = useCallback(async () => {
        if (!workspaceId) return;
        try {
            const params = { type: viewMode, workspaceId };
            if (initialSelectedDocIds.length > 0) {
                // Pass as comma separated or repeat? URLSearchParams handles repeat if array passed to append, 
                // but api wrapper uses URLSearchParams constructor which handles object?
                // Need to ensure array is handled correctly. 
                // api.js uses new URLSearchParams(params).toString().
                // Standard URLSearchParams handles arrays by comma separating or repeating keys depending on implementation?
                // Actually JS URLSearchParams joining array with comma is standard behavior for string conversion?
                // Let's manually join for safety or check. 
                // Spring accepts repeating param `documentIds=1&documentIds=2` or comma `documentIds=1,2`.
                // Let's safely join.
                params.documentIds = initialSelectedDocIds.join(',');
            }
            const result = await dictionaryApi.getCategories(params);
            setCategories(['All', ...result]);
        } catch (error) {
            console.error("Failed to fetch categories", error);
        }
    }, [workspaceId, viewMode, initialSelectedDocIds]);

    // Fetch Data
    const fetchData = useCallback(async () => {
        if (!workspaceId) return;
        setLoading(true);
        try {
            const params = {
                workspaceId,
                page,
                size: pageSize,
                sort: 'id,desc' // Default sort
            };
            if (initialSelectedDocIds.length > 0) {
                params.documentIds = initialSelectedDocIds.join(',');
            }

            let result;
            if (viewMode === 'concept') {
                result = await dictionaryApi.getConcepts(params);
            } else {
                result = await dictionaryApi.getRelations(params);
            }

            setData(result.content);
            setTotalPages(result.totalPages);
            setTotalElements(result.totalElements);
        } catch (error) {
            console.error("Failed to fetch dictionary data", error);
        } finally {
            setLoading(false);
        }
    }, [workspaceId, viewMode, page, initialSelectedDocIds]);

    useEffect(() => {
        setPage(0); // Reset page on mode/filter change
        fetchCategories();
    }, [fetchCategories, viewMode, initialSelectedDocIds]);

    useEffect(() => {
        fetchData();
    }, [fetchData]);

    const handleEditClick = (item) => {
        setEditingTerm({ ...item });
        setIsEditModalOpen(true);
    };

    const handleMoveClick = (item) => {
        setMoveSourceItem(item);
        setMoveSearchTerm('');
        setMoveTargetId(null);
        setIsMoveModalOpen(true);
    };

    const handleSaveEdit = async () => {
        if (!editingTerm) return;
        try {
            if (viewMode === 'concept') {
                await dictionaryApi.updateConcept(editingTerm.id, editingTerm);
            } else {
                await dictionaryApi.updateRelation(editingTerm.id, editingTerm);
            }
            setIsEditModalOpen(false);
            fetchData();
            if (onUpdate) onUpdate();
        } catch (error) {
            showAlert("저장 실패: " + error.message);
        }
    };

    const handleDelete = async (id) => {
        if (!window.confirm("정말로 삭제하시겠습니까?")) return;
        try {
            if (viewMode === 'concept') {
                await dictionaryApi.deleteConcept(id);
            } else {
                await dictionaryApi.deleteRelation(id);
            }
            fetchData();
            if (onUpdate) onUpdate();
        } catch (error) {
            showAlert("삭제 실패: " + error.message);
        }
    };

    const handleConfirmMove = async () => {
        if (!moveSourceItem || !moveTargetId) return;
        try {
            const payload = {
                sourceId: moveSourceItem.id,
                targetId: moveTargetId,
                workspaceId: workspaceId,
                mode: 'move'
            };
            if (viewMode === 'concept') {
                await dictionaryApi.mergeConcepts(payload);
            } else {
                await dictionaryApi.mergeRelations(payload);
            }
            setIsMoveModalOpen(false);
            fetchData();
            if (onUpdate) onUpdate();
        } catch (error) {
            alert("이동 실패: " + error.message);
        }
    };

    const handleModalChange = (field, value) => {
        setEditingTerm(prev => ({ ...prev, [field]: value }));
    };

    // Client-side filtering for search/category on the CURRENT PAGE data
    // Note: Since backend pagination is used, this only filters the detailed 20 items. 
    // Ideally backend should support search/category filtering.
    // For now implementing as per previous behavior assumption.
    const filteredData = data.filter(item => {
        const matchesSearch = searchTerm === '' ||
            (item.label && item.label.toLowerCase().includes(searchTerm.toLowerCase())) ||
            (item.labelEn && item.labelEn.toLowerCase().includes(searchTerm.toLowerCase()));

        const matchesCategory = selectedCategory.id === 'All' || item.category === selectedCategory.id; // Corrected: category is string usually?
        // Wait, setCategories sets strings ['All', 'Cat1', ...]. selectedCategory state is object? 
        // Initial state: { id: 'All', ... }. 
        // Let's simplify category selection.

        return matchesSearch && (selectedCategory.id === 'All' || item.category === selectedCategory.id);
    });

    return (
        <div className="dictionary-view">
            <div className="view-header">
                <div className="tab-group">
                    <button
                        className={`tab-btn ${viewMode === 'concept' ? 'active' : ''}`}
                        onClick={() => setViewMode('concept')}
                    >
                        개념 (Concepts)
                    </button>
                    <button
                        className={`tab-btn ${viewMode === 'relation' ? 'active' : ''}`}
                        onClick={() => setViewMode('relation')}
                    >
                        관계 (Relations)
                    </button>
                </div>

                <div className="search-bar">
                    <Search size={18} color="#666" />
                    <input
                        type="text"
                        placeholder="검색..."
                        value={searchTerm}
                        onChange={(e) => setSearchTerm(e.target.value)}
                    />
                </div>
            </div>

            <div className="content-area">
                <div className="category-sidebar">
                    <h4>카테고리</h4>
                    <ul>
                        {categories.map((cat, idx) => (
                            <li
                                key={idx}
                                className={selectedCategory.id === (cat === 'All' ? 'All' : cat) ? 'active' : ''}
                                onClick={() => setSelectedCategory({ id: cat === 'All' ? 'All' : cat, name: cat, label: cat })}
                            >
                                {cat}
                            </li>
                        ))}
                    </ul>
                </div>

                <div className="data-table">
                    <div className="table-header">
                        <div className="header-cell" style={{ flex: 1.5 }}>카테고리</div>
                        <div className="header-cell" style={{ flex: 2 }}>용어(KR)</div>
                        <div className="header-cell" style={{ flex: 2 }}>용어(EN)</div>
                        <div className="header-cell" style={{ flex: 3 }}>설명</div>
                        <div className="header-cell" style={{ flex: 2 }}>유의어</div>
                        {!readOnly && <div className="header-cell action-column" style={{ justifyContent: 'center' }}>관리</div>}
                    </div>

                    <div className="term-list">
                        {loading ? (
                            <div className="loading-state">로딩 중...</div>
                        ) : filteredData.length === 0 ? (
                            <div className="empty-state">데이터가 없습니다.</div>
                        ) : (
                            filteredData.map(item => (
                                <div key={item.id} className="term-item">
                                    <div className="term-cell" style={{ flex: 1.5 }}>
                                        <span className="category-tag">{item.category}</span>
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
                                    {!readOnly && (
                                        <div className="term-cell action-column" style={{ justifyContent: 'center', gap: '8px' }}>
                                            <button className="icon-btn edit-btn" onClick={() => handleEditClick(item)} title="수정">
                                                <Edit2 size={16} />
                                            </button>
                                            <button className="icon-btn move-btn" onClick={() => handleMoveClick(item)} title="이동 (병합)">
                                                <ArrowRightCircle size={16} />
                                            </button>
                                            <button className="icon-btn delete-btn" onClick={() => handleDelete(item.id)} title="삭제">
                                                <Trash2 size={16} />
                                            </button>
                                        </div>
                                    )}
                                </div>
                            ))
                        )}
                    </div>

                    {/* Pagination Controls */}
                    <div className="pagination">
                        <button
                            disabled={page === 0}
                            onClick={() => setPage(p => Math.max(0, p - 1))}
                        >
                            <ChevronLeft size={16} />
                        </button>
                        <span>{page + 1} / {totalPages || 1}</span>
                        <button
                            disabled={page >= totalPages - 1}
                            onClick={() => setPage(p => p + 1)}
                        >
                            <ChevronRight size={16} />
                        </button>
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
                                    value={editingTerm.label || ''}
                                    onChange={(e) => handleModalChange('label', e.target.value)}
                                />
                            </div>
                            <div className="form-group">
                                <label>라벨(EN)</label>
                                <input
                                    type="text"
                                    value={editingTerm.labelEn || ''}
                                    onChange={(e) => handleModalChange('labelEn', e.target.value)}
                                />
                            </div>
                            <div className="form-group">
                                <label>설명</label>
                                <textarea
                                    value={editingTerm.description || ''}
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

            {/* Move Modal */}
            {isMoveModalOpen && moveSourceItem && (
                <div className="edit-modal-overlay">
                    <div className="edit-modal" style={{ width: '500px' }}>
                        <h3>{viewMode === 'concept' ? '개념' : '관계'} 이동 (병합)</h3>
                        <p style={{ color: '#666', fontSize: '13px', marginBottom: '16px' }}>
                            <b>'{moveSourceItem.label}'</b> {viewMode === 'concept' ? '개념' : '관계'}을 다른 {viewMode === 'concept' ? '개념' : '관계'}으로 이동(병합)합니다. <br />
                            이동 후 원본 {viewMode === 'concept' ? '개념' : '관계'}은 삭제되며, 모든 문서 출처와 관계가 대상 {viewMode === 'concept' ? '개념' : '관계'}으로 이전됩니다.
                        </p>

                        <div style={{ marginBottom: '16px' }}>
                            <label style={{ display: 'block', marginBottom: '8px', fontWeight: 'bold' }}>이동할 대상 검색</label>
                            <input
                                type="text"
                                placeholder="대상 검색..."
                                value={moveSearchTerm}
                                onChange={(e) => setMoveSearchTerm(e.target.value)}
                                style={{ width: '100%', padding: '8px', marginBottom: '8px', border: '1px solid #ddd', borderRadius: '4px' }}
                            />
                            <div style={{ maxHeight: '200px', overflowY: 'auto', border: '1px solid #ddd', borderRadius: '4px' }}>
                                {data
                                    .filter(t => t.id !== moveSourceItem.id && (
                                        (t.label && t.label.toLowerCase().includes(moveSearchTerm.toLowerCase())) ||
                                        (t.labelEn && t.labelEn.toLowerCase().includes(moveSearchTerm.toLowerCase()))
                                    ))
                                    .slice(0, 50)
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

export default DictionaryView;
