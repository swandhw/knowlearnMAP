import { useState, useEffect, useCallback } from 'react';
import './DictionaryView.css';
import { dictionaryApi } from '../services/api';
import { useAlert } from '../context/AlertContext';
import { Search, Edit2, ArrowRightCircle, ChevronLeft, ChevronRight } from 'lucide-react';

function DictionaryView({ workspaceId, initialSelectedDocIds = [], onUpdate, readOnly }) {
    const { showAlert, showConfirm } = useAlert();
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
    const [allCandidates, setAllCandidates] = useState([]);
    const [loadingCandidates, setLoadingCandidates] = useState(false);
    const [candidatePage, setCandidatePage] = useState(0);
    const [hasMoreCandidates, setHasMoreCandidates] = useState(true);
    const [keepSourceAsSynonym, setKeepSourceAsSynonym] = useState(true);

    // Fetch Categories
    const fetchCategories = useCallback(async () => {
        if (!workspaceId) return;
        try {
            const params = { type: viewMode, workspaceId };
            if (initialSelectedDocIds.length > 0) {
                params.documentIds = initialSelectedDocIds.join(',');
            }
            const result = await dictionaryApi.getCategories(params);
            // Add null safety - result might be undefined or not an array
            if (Array.isArray(result)) {
                setCategories(['All', ...result]);
            } else {
                console.warn('Categories result is not an array:', result);
                setCategories(['All']);
            }
        } catch (error) {
            console.error("Failed to fetch categories", error);
            setCategories(['All']); // Fallback to default
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

            // Add null safety - result might be undefined
            if (result && result.content) {
                setData(result.content);
                setTotalPages(result.totalPages || 0);
                setTotalElements(result.totalElements || 0);
            } else {
                console.warn('Dictionary result is invalid:', result);
                setData([]);
                setTotalPages(0);
                setTotalElements(0);
            }
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

    // Search with debounce
    useEffect(() => {
        if (!isMoveModalOpen) return;
        const timer = setTimeout(() => {
            setCandidatePage(0);
            setAllCandidates([]);
            setHasMoreCandidates(true);
            fetchAllCandidates(viewMode, 0, moveSearchTerm);
        }, 500);
        return () => clearTimeout(timer);
    }, [moveSearchTerm, isMoveModalOpen]);

    const fetchAllCandidates = async (currentViewMode, pageNum, searchTerm) => {
        setLoadingCandidates(true);
        try {
            const params = {
                workspaceId,
                page: pageNum,
                size: 20,
                sort: 'id,desc'
            };
            if (searchTerm) {
                params.search = searchTerm;
            }

            // Note: We deliberately DO NOT filter by documentIds here 
            // because merge target can be any concept in the workspace.

            let result;
            if (currentViewMode === 'concept') {
                result = await dictionaryApi.getConcepts(params);
            } else {
                result = await dictionaryApi.getRelations(params);
            }

            if (result && result.content) {
                if (pageNum === 0) {
                    setAllCandidates(result.content);
                } else {
                    setAllCandidates(prev => [...prev, ...result.content]);
                }
                setHasMoreCandidates(!result.last);
            } else {
                if (pageNum === 0) setAllCandidates([]);
                setHasMoreCandidates(false);
            }
        } catch (error) {
            console.error("Failed to fetch candidates", error);
            if (pageNum === 0) setAllCandidates([]);
        } finally {
            setLoadingCandidates(false);
        }
    };

    const handleLoadMoreCandidates = () => {
        if (!loadingCandidates && hasMoreCandidates) {
            const nextPage = candidatePage + 1;
            setCandidatePage(nextPage);
            fetchAllCandidates(viewMode, nextPage, moveSearchTerm);
        }
    };

    const handleMoveClick = (item) => {
        setMoveSourceItem(item);
        setMoveSearchTerm('');
        setMoveTargetId(null);
        setAllCandidates([]); // Clear previous
        setCandidatePage(0);
        setHasMoreCandidates(true);
        setIsMoveModalOpen(true);
        setKeepSourceAsSynonym(true); // Reset to default true
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
        const confirmed = await showConfirm("정말로 삭제하시겠습니까?");
        if (!confirmed) return;
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
                mode: 'move',
                keepSourceAsSynonym: keepSourceAsSynonym
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
            showAlert("이동 실패: " + error.message);
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

        const matchesCategory = selectedCategory.id === 'All' || item.category === selectedCategory.id;

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
                    <div className="item-count-info">
                        <span>총 {totalElements}건 | 검색 결과: {filteredData.length}건</span>
                    </div>
                </div>

                <div className="filter-search-group">
                    <div className="category-filter-inline">
                        <label htmlFor="category-select">카테고리:</label>
                        <select
                            id="category-select"
                            value={selectedCategory.id}
                            onChange={(e) => {
                                const catId = e.target.value;
                                setSelectedCategory({ id: catId, name: catId, label: catId });
                            }}
                        >
                            {categories.map((cat, idx) => (
                                <option key={idx} value={cat === 'All' ? 'All' : cat}>
                                    {cat === 'All' ? '전체' : cat}
                                </option>
                            ))}
                        </select>
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
            </div>

            <div className="content-area">

                <div className="data-table">
                    <div className="table-header">
                        <div className="header-cell" style={{ flex: 1.5 }}>카테고리</div>
                        <div className="header-cell" style={{ flex: 2 }}>용어(KR)</div>
                        <div className="header-cell" style={{ flex: 2 }}>용어(EN)</div>
                        <div className="header-cell" style={{ flex: 3 }}>설명</div>
                        <div className="header-cell" style={{ flex: 2 }}>유의어</div>
                        {!readOnly && <div className="header-cell action-column" style={{ justifyContent: 'center', flex: 0.8 }}>관리</div>}
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
                                        <div className="term-cell action-column" style={{ justifyContent: 'center', gap: '4px', flex: 0.8 }}>
                                            <button className="icon-btn edit-btn" onClick={() => handleEditClick(item)} title="수정">
                                                <Edit2 size={16} />
                                            </button>
                                            <button className="icon-btn move-btn" onClick={() => handleMoveClick(item)} title="이동 (병합)">
                                                <ArrowRightCircle size={16} />
                                            </button>
                                        </div>
                                    )}
                                </div>
                            ))
                        )}
                    </div>

                    {/* Pagination Controls */}
                    <div className="pagination-container">
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
                            <div
                                style={{ maxHeight: '200px', overflowY: 'auto', border: '1px solid #ddd', borderRadius: '4px' }}
                                onScroll={(e) => {
                                    const { scrollTop, scrollHeight, clientHeight } = e.target;
                                    if (scrollHeight - scrollTop <= clientHeight + 50) {
                                        handleLoadMoreCandidates();
                                    }
                                }}
                            >
                                {allCandidates
                                    .filter(t => t.id !== moveSourceItem.id) // Still filter out self
                                    .map(t => (
                                        <div
                                            key={t.id}
                                            onClick={() => setMoveTargetId(t.id)}
                                            style={{
                                                padding: '8px', cursor: 'pointer', borderBottom: '1px solid #f0f0f0',
                                                backgroundColor: moveTargetId === t.id ? '#e3f2fd' : 'white',
                                                display: 'flex', alignItems: 'center', gap: '8px'
                                            }}
                                        >
                                            <span className="category-tag small" style={{ fontSize: '10px', padding: '2px 6px' }}>{t.category}</span>
                                            <div style={{ flex: 1 }}>
                                                <div style={{ fontWeight: 'bold', fontSize: '13px' }}>{t.label}</div>
                                                <div style={{ fontSize: '11px', color: '#666' }}>{t.labelEn}</div>
                                            </div>
                                        </div>
                                    ))}
                                {loadingCandidates && (
                                    <div style={{ padding: '8px', color: '#666', textAlign: 'center' }}>목록 불러오는 중...</div>
                                )}
                                {!loadingCandidates && allCandidates.length === 0 && (
                                    <div style={{ padding: '8px', color: '#666', textAlign: 'center' }}>검색 결과가 없습니다.</div>
                                )}
                            </div>
                        </div>

                        <div style={{ marginBottom: '16px', display: 'flex', alignItems: 'center' }}>
                            <input
                                type="checkbox"
                                id="keepSourceAsSynonym"
                                checked={keepSourceAsSynonym}
                                onChange={(e) => setKeepSourceAsSynonym(e.target.checked)}
                                style={{ marginRight: '8px' }}
                            />
                            <label htmlFor="keepSourceAsSynonym" style={{ fontSize: '13px', color: '#333', cursor: 'pointer' }}>
                                원본 용어를 유의어로 추가 (병합 후 검색 가능)
                            </label>
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
