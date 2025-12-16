import { useState } from 'react';
import './DictionaryView.css';

function DictionaryView() {
    const [viewMode, setViewMode] = useState('concept'); // 'concept' or 'relation'
    const [selectedCategory, setSelectedCategory] = useState({ id: 'Abbreviation', name: 'Abbreviation', label: '약어' });
    const [searchTerm, setSearchTerm] = useState('');

    // Modal & Edit State
    const [isEditModalOpen, setIsEditModalOpen] = useState(false);
    const [editingTerm, setEditingTerm] = useState(null);

    // Initial Data
    const initialTerms = [
        { id: 1, label: '땀', labelEn: 'Sweat', synonym: '', desc: '피부의 땀샘에서 분비되는 체액', status: 'active' },
        { id: 2, label: '샘 분비물', labelEn: 'Gland Secretion', synonym: '샘분비물', desc: '샘(gland)에서 분비되는 물질', status: 'active' },
        { id: 3, label: '아포크린 분비물', labelEn: 'Apocrine Secretion', synonym: '아포크린분비물', desc: '아포크린 샘에서 분비되는 물질, 주로 겨드랑이에서 발견됨', status: 'active' },
        { id: 4, label: '피지 분비물', labelEn: 'Sebum Secretion', synonym: '피지분비물', desc: '피지샘에서 분비되는 기름진 물질', status: 'active' },
    ];

    const initialRelations = [
        { id: 1, label: '포함한다', labelEn: 'Includes', synonym: '', desc: '', status: 'active' },
        { id: 2, label: '위험 가진다', labelEn: 'Has Risk', synonym: '위험가진다', desc: '', status: 'active' },
        { id: 3, label: '향상시킨다', labelEn: 'Improves', synonym: '', desc: '', status: 'active' },
        { id: 4, label: '준수한다', labelEn: 'Complies With', synonym: '', desc: '', status: 'active' },
        { id: 5, label: '의 파생물임', labelEn: 'Is Derivative Of', synonym: '의파생물임', desc: '주제 객체가 대상 객체의 파생물이거나 관련 그룹에 속함을 나타냄', status: 'active' },
        { id: 6, label: '준수하지 않는다', labelEn: 'Does Not Comply With', synonym: '준수하지않는다', desc: '', status: 'active' },
        { id: 7, label: '아래에 발행된다', labelEn: 'Issued Under', synonym: '아래에발행된다', desc: '', status: 'active' },
        { id: 8, label: '위반한다', labelEn: 'Violates', synonym: '', desc: '', status: 'active' },
        { id: 9, label: '요구한다', labelEn: 'Requires', synonym: '', desc: '', status: 'active' },
        { id: 10, label: '간주된다', labelEn: 'Is Considered', synonym: '', desc: '', status: 'active' },
    ];

    const [terms, setTerms] = useState(initialTerms);
    const [relations, setRelations] = useState(initialRelations);

    const categories = [
        { id: 'Abbreviation', name: 'Abbreviation', label: '약어' },
        { id: 'Benefit', name: 'Benefit', label: '이익' },
        { id: 'BiologicalProcess', name: 'BiologicalProcess', label: '생물학적 과정' },
        { id: 'BiologicalSample', name: 'BiologicalSample', label: '생물학적 샘플' },
        { id: 'BiologicalSecretion', name: 'BiologicalSecretion', label: '생물학적 분비물' },
        { id: 'BiologicalStructure', name: 'BiologicalStructure', label: '생물학적 구조' },
        { id: 'BiologicalSystem', name: 'BiologicalSystem', label: '생물학적 시스템' },
        { id: 'Brand', name: 'Brand', label: '브랜드' },
        { id: 'CombiningRule', name: 'CombiningRule', label: '결합 규칙' },
        { id: 'Formulation', name: 'Formulation', label: '제형' },
        { id: 'General', name: 'General', label: '일반' }
    ];

    const currentData = viewMode === 'concept' ? terms : relations;
    const listTitle = viewMode === 'concept' ? '사전 항목 목록' : '관계 항목 목록';

    // Handlers
    const handleDelete = (id) => {
        if (window.confirm('정말 삭제하시겠습니까?')) {
            if (viewMode === 'concept') {
                setTerms(prev => prev.filter(t => t.id !== id));
            } else {
                setRelations(prev => prev.filter(r => r.id !== id));
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

    const handleSaveEdit = () => {
        if (viewMode === 'concept') {
            setTerms(prev => prev.map(t => t.id === editingTerm.id ? editingTerm : t));
        } else {
            setRelations(prev => prev.map(r => r.id === editingTerm.id ? editingTerm : r));
        }
        setIsEditModalOpen(false);
        setEditingTerm(null);
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
                                        <td className="desc-cell">{term.desc}</td>
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
                            <div className="form-group">
                                <label>유의어</label>
                                <input
                                    type="text"
                                    value={editingTerm.synonym}
                                    onChange={(e) => handleModalChange('synonym', e.target.value)}
                                />
                            </div>
                            <div className="form-group">
                                <label>설명</label>
                                <textarea
                                    value={editingTerm.desc}
                                    onChange={(e) => handleModalChange('desc', e.target.value)}
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
