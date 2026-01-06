import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import axios from 'axios';
import { API_URL } from '../config/api';
import './DomainSelection.css';

function DomainSelection() {
    const navigate = useNavigate();
    const { user, isAdmin, logout } = useAuth();
    const [domains, setDomains] = useState([]);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);

    useEffect(() => {
        if (user && !isAdmin) {
            // Normal users have 1:1 domain mapping, auto-select (skip selection)
            navigate('/workspaces');
            return;
        }

        if (isAdmin) {
            fetchDomains();
        }
    }, [user, isAdmin, navigate]);

    const fetchDomains = async () => {
        setLoading(true);
        try {
            const response = await axios.get(`${API_URL}/api/domains`);
            setDomains(response.data);
            setLoading(false);
        } catch (err) {
            console.error('Failed to fetch domains', err);
            setError('도메인 목록을 불러오는데 실패했습니다.');
            setLoading(false);
        }
    };

    if (!user) return null;

    if (!isAdmin) {
        return <div style={{ display: 'flex', justifyContent: 'center', marginTop: '50px' }}>Redirecting...</div>;
    }

    const handleSelectDomain = (domainId) => {
        localStorage.setItem('admin_selected_domain_id', domainId);
        navigate('/workspaces');
    };

    return (
        <div className="domain-selection-container">
            <div className="domain-selection-content">
                <header className="domain-header">
                    <h1>도메인 선택</h1>
                    <p>작업할 도메인을 선택해주세요.</p>
                </header>

                <div className="user-info-bar">
                    <span>관리자 로그인 ({user.email})</span>
                    <button className="logout-text-btn" onClick={logout}>로그아웃</button>
                </div>

                {loading && <p>Loading domains...</p>}
                {error && <p className="error-message">{error}</p>}

                <div className="domain-grid">
                    {domains.map(domain => (
                        <div key={domain.id} className="domain-card" onClick={() => handleSelectDomain(domain.id)}>
                            <div className="domain-icon">{domain.icon || '🌐'}</div>
                            <h3>{domain.name}</h3>
                            <p>{domain.description || '설명 없음'}</p>
                            <button className="select-btn">선택</button>
                        </div>
                    ))}
                </div>
            </div>
        </div>
    );
}

export default DomainSelection;
