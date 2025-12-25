import { useState, useEffect } from 'react';

function DomainManagement() {
    const [domains, setDomains] = useState([]);
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [formData, setFormData] = useState({
        name: '',
        description: '',
        arangoDbName: ''
    });
    const [isArangoDbChecked, setIsArangoDbChecked] = useState(false);
    const [checkMessage, setCheckMessage] = useState('');
    const [error, setError] = useState('');

    useEffect(() => {
        fetchDomains();
    }, []);

    const fetchDomains = async () => {
        try {
            const response = await fetch('/api/domains');
            if (response.ok) {
                const data = await response.json();
                setDomains(data);
            }
        } catch (err) {
            console.error('Failed to fetch domains', err);
        }
    };

    const handleInputChange = (e) => {
        const { name, value } = e.target;

        if (name === 'arangoDbName') {
            // Validation: only lowercase letters
            if (value && !/^[a-z]*$/.test(value)) {
                return;
            }
            // Reset check status on change
            setIsArangoDbChecked(false);
            setCheckMessage('');
        }

        setFormData(prev => ({
            ...prev,
            [name]: value
        }));
    };

    const handleDuplicateCheck = () => {
        if (!formData.arangoDbName) {
            setCheckMessage('ArangoDB 이름을 입력해주세요.');
            return;
        }

        const exists = domains.some(d => d.arangoDbName === formData.arangoDbName);
        if (exists) {
            setCheckMessage('이미 사용 중인 이름입니다.');
            setIsArangoDbChecked(false);
        } else {
            setCheckMessage('사용 가능한 이름입니다.');
            setIsArangoDbChecked(true);
        }
    };

    const handleSubmit = async () => {
        // Validation
        if (!formData.name || !formData.arangoDbName) {
            setError('도메인명과 ArangoDB명은 필수입니다.');
            return;
        }

        if (!isArangoDbChecked) {
            setError('ArangoDB명 중복 확인을 해주세요.');
            return;
        }

        // Check Duplicates (Frontend Check again for safety)
        const nameExists = domains.some(d => d.name === formData.name);
        if (nameExists) {
            setError('이미 존재하는 도메인 이름입니다.');
            return;
        }

        try {
            const response = await fetch('/api/domains', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(formData),
            });

            if (response.ok) {
                await fetchDomains();
                setIsModalOpen(false);
                setFormData({ name: '', description: '', arangoDbName: '' });
                setIsArangoDbChecked(false);
                setCheckMessage('');
                setError('');
            } else {
                const errorMsg = await response.text();
                setError(errorMsg || '도메인 생성 실패');
            }
        } catch (err) {
            setError('서버 오류가 발생했습니다.');
        }
    };

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
                    <button
                        className="admin-btn admin-btn-primary"
                        onClick={() => setIsModalOpen(true)}
                    >
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
                        <th>ArangoDB명</th>
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
                                <td>{new Date(domain.createdDatetime || domain.createdAt).toLocaleDateString()}</td>
                                <td>{domain.arangoDbName}</td>
                            </tr>
                        ))
                    )}
                </tbody>
            </table>

            {/* Modal */}
            {isModalOpen && (
                <div style={{
                    position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
                    backgroundColor: 'rgba(0,0,0,0.5)', display: 'flex', justifyContent: 'center', alignItems: 'center',
                    zIndex: 1000
                }}>
                    <div style={{
                        backgroundColor: 'white', padding: '24px', borderRadius: '8px', width: '400px',
                        boxShadow: '0 4px 6px rgba(0,0,0,0.1)'
                    }}>
                        <h3>새 도메인 추가</h3>

                        <div style={{ marginBottom: '16px' }}>
                            <label style={{ display: 'block', marginBottom: '8px' }}>도메인명</label>
                            <input
                                type="text"
                                name="name"
                                value={formData.name}
                                onChange={handleInputChange}
                                style={{ width: '100%', padding: '8px', border: '1px solid #ddd', borderRadius: '4px' }}
                            />
                        </div>

                        <div style={{ marginBottom: '16px' }}>
                            <label style={{ display: 'block', marginBottom: '8px' }}>설명</label>
                            <input
                                type="text"
                                name="description"
                                value={formData.description}
                                onChange={handleInputChange}
                                style={{ width: '100%', padding: '8px', border: '1px solid #ddd', borderRadius: '4px' }}
                            />
                        </div>

                        <div style={{ marginBottom: '16px' }}>
                            <label style={{ display: 'block', marginBottom: '8px' }}>ArangoDB명 (소문자만 가능)</label>
                            <div style={{ display: 'flex', gap: '8px' }}>
                                <input
                                    type="text"
                                    name="arangoDbName"
                                    value={formData.arangoDbName}
                                    onChange={handleInputChange}
                                    placeholder="예: mydomaindb"
                                    style={{ flex: 1, padding: '8px', border: '1px solid #ddd', borderRadius: '4px' }}
                                />
                                <button
                                    onClick={handleDuplicateCheck}
                                    style={{
                                        padding: '8px 12px',
                                        backgroundColor: '#e0e0e0',
                                        border: 'none',
                                        borderRadius: '4px',
                                        cursor: 'pointer',
                                        fontSize: '13px'
                                    }}
                                >
                                    중복확인
                                </button>
                            </div>
                            {checkMessage && (
                                <div style={{
                                    fontSize: '12px',
                                    marginTop: '4px',
                                    color: isArangoDbChecked ? 'green' : 'red'
                                }}>
                                    {checkMessage}
                                </div>
                            )}
                        </div>

                        {error && <div style={{ color: 'red', marginBottom: '16px', fontSize: '14px' }}>{error}</div>}

                        <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px' }}>
                            <button
                                onClick={() => { setIsModalOpen(false); setError(''); setCheckMessage(''); setIsArangoDbChecked(false); }}
                                style={{ padding: '8px 16px', border: '1px solid #ddd', borderRadius: '4px', background: 'white', cursor: 'pointer' }}
                            >
                                취소
                            </button>
                            <button
                                onClick={handleSubmit}
                                disabled={!isArangoDbChecked}
                                className="admin-btn admin-btn-primary"
                                style={{
                                    padding: '8px 16px',
                                    border: 'none',
                                    borderRadius: '4px',
                                    backgroundColor: !isArangoDbChecked ? '#ccc' : undefined,
                                    cursor: !isArangoDbChecked ? 'not-allowed' : 'pointer'
                                }}
                            >
                                생성
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}

export default DomainManagement;
