import { useState } from 'react';

function DomainManagement() {
    const [domains] = useState([]);

    return (
        <div className="admin-section">
            <div className="admin-section-header">
                <h2 className="admin-section-title">도메인 관리</h2>
                <div className="admin-toolbar">
                    <input
                        type="text"
                        className="admin-search"
                        placeholder="도메인 검색..."
                    />
                    <button className="admin-btn admin-btn-primary">
                        ➕ 새 도메인
                    </button>
                </div>
            </div>

            <table className="admin-table">
                <thead>
                    <tr>
                        <th>도메인명</th>
                        <th>설명</th>
                        <th>생성일</th>
                        <th>작업</th>
                    </tr>
                </thead>
                <tbody>
                    {domains.length === 0 ? (
                        <tr>
                            <td colSpan="4" style={{ textAlign: 'center', padding: '40px' }}>
                                도메인이 없습니다.
                            </td>
                        </tr>
                    ) : (
                        domains.map((domain) => (
                            <tr key={domain.id}>
                                <td><strong>{domain.name}</strong></td>
                                <td>{domain.description}</td>
                                <td>{new Date(domain.createdAt).toLocaleDateString()}</td>
                                <td>
                                    <div className="admin-actions">
                                        <button className="admin-btn admin-btn-secondary">
                                            ✏️ 수정
                                        </button>
                                        <button className="admin-btn admin-btn-danger">
                                            🗑️ 삭제
                                        </button>
                                    </div>
                                </td>
                            </tr>
                        ))
                    )}
                </tbody>
            </table>

            <div style={{ marginTop: '20px', padding: '20px', backgroundColor: '#f5f5f5', borderRadius: '8px' }}>
                <p style={{ color: '#666', textAlign: 'center' }}>
                    💡 도메인 관리 기능은 추후 API 연동 후 활성화됩니다.
                </p>
            </div>
        </div>
    );
}

export default DomainManagement;
