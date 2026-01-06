import React, { useEffect, useState } from 'react';
import { API_BASE_URL } from '../../config/api';

const AdminUpgradeRequests = () => {
    const [requests, setRequests] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    const fetchRequests = async () => {
        try {
            const token = localStorage.getItem('token');
            const response = await fetch(`${API_BASE_URL}/admin/upgrades`, {
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            });

            if (!response.ok) throw new Error('Failed to fetch requests');

            const data = await response.json();
            setRequests(data);
        } catch (err) {
            setError(err.message);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchRequests();
    }, []);

    const handleApprove = async (id) => {
        if (!window.confirm('승인하시겠습니까? 해당 회원의 등급이 변경됩니다.')) return;

        try {
            const token = localStorage.getItem('token');
            const response = await fetch(`${API_BASE_URL}/admin/upgrades/${id}/approve`, {
                method: 'PUT',
                headers: {
                    'Authorization': `Bearer ${token}`
                }
            });

            if (!response.ok) {
                const errText = await response.text();
                throw new Error(errText || '승인 실패');
            }

            alert('승인되었습니다.');
            fetchRequests(); // Refresh list
        } catch (err) {
            alert(err.message);
        }
    };

    if (loading) return <div>Loading...</div>;
    if (error) return <div>Error: {error}</div>;

    return (
        <div className="admin-container" style={{ padding: '20px', maxWidth: '1200px', margin: '0 auto' }}>
            <h1 style={{ marginBottom: '20px', fontSize: '1.5rem', fontWeight: 'bold' }}>승인 대기 요청 목록</h1>

            <div className="table-responsive" style={{ background: 'white', borderRadius: '8px', boxShadow: '0 1px 3px rgba(0,0,0,0.1)', overflow: 'hidden' }}>
                <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                    <thead style={{ background: '#f8f9fa', borderBottom: '2px solid #e9ecef' }}>
                        <tr>
                            <th style={{ padding: '12px', textAlign: 'left', fontWeight: '600', color: '#495057' }}>신청일</th>
                            <th style={{ padding: '12px', textAlign: 'left', fontWeight: '600', color: '#495057' }}>이메일</th>
                            <th style={{ padding: '12px', textAlign: 'left', fontWeight: '600', color: '#495057' }}>회사/소속</th>
                            <th style={{ padding: '12px', textAlign: 'left', fontWeight: '600', color: '#495057' }}>담당자</th>
                            <th style={{ padding: '12px', textAlign: 'left', fontWeight: '600', color: '#495057' }}>유형</th>
                            <th style={{ padding: '12px', textAlign: 'left', fontWeight: '600', color: '#495057' }}>상태</th>
                            <th style={{ padding: '12px', textAlign: 'left', fontWeight: '600', color: '#495057' }}>액션</th>
                        </tr>
                    </thead>
                    <tbody>
                        {requests.length === 0 ? (
                            <tr>
                                <td colSpan="7" style={{ padding: '20px', textAlign: 'center', color: '#868e96' }}>
                                    대기 중인 요청이 없습니다.
                                </td>
                            </tr>
                        ) : (
                            requests.map(req => (
                                <tr key={req.id} style={{ borderBottom: '1px solid #f1f3f5' }}>
                                    <td style={{ padding: '12px' }}>{new Date(req.createdAt).toLocaleDateString()}</td>
                                    <td style={{ padding: '12px' }}>{req.email}</td>
                                    <td style={{ padding: '12px' }}>{req.company}</td>
                                    <td style={{ padding: '12px' }}>
                                        {req.name}<br />
                                        <span style={{ fontSize: '0.8rem', color: '#868e96' }}>{req.phone}</span>
                                    </td>
                                    <td style={{ padding: '12px' }}>
                                        <span style={{
                                            padding: '4px 8px',
                                            borderRadius: '4px',
                                            fontSize: '0.8rem',
                                            fontWeight: 'bold',
                                            background: req.type === 'MAX_CONSULTATION' ? '#e9ecef' : '#e7f5ff',
                                            color: req.type === 'MAX_CONSULTATION' ? '#495057' : '#228be6'
                                        }}>
                                            {req.type === 'PRO_UPGRADE' ? 'Pro 신청' : 'Max 상담'}
                                        </span>
                                    </td>
                                    <td style={{ padding: '12px' }}>
                                        <span style={{
                                            fontWeight: 'bold',
                                            color: req.status === 'APPROVED' ? '#2b8a3e' : '#f08c00'
                                        }}>
                                            {req.status}
                                        </span>
                                    </td>
                                    <td style={{ padding: '12px' }}>
                                        {req.status === 'PENDING' && (
                                            <button
                                                onClick={() => handleApprove(req.id)}
                                                style={{
                                                    padding: '6px 12px',
                                                    background: '#20c997',
                                                    color: 'white',
                                                    border: 'none',
                                                    borderRadius: '4px',
                                                    cursor: 'pointer',
                                                    fontSize: '0.9rem'
                                                }}
                                            >
                                                승인
                                            </button>
                                        )}
                                    </td>
                                </tr>
                            ))
                        )}
                    </tbody>
                </table>
            </div>
        </div>
    );
};

export default AdminUpgradeRequests;
