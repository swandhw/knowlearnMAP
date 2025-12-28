import React from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import './DomainSelection.css'; // We will create this simple CSS

function DomainSelection() {
    const navigate = useNavigate();
    const { user, isAdmin, logout } = useAuth();

    const domains = [
        { id: 'knowlearn', name: 'Knowlearn Map', description: '지식 온톨로지 및 학습 맵 관리', icon: '🧠' },
        // Add more mock domains if needed
    ];

    const handleSelectDomain = (domainId) => {
        // For now, regardless of selection, go to the main workspace list
        // In the future, this could set a global "currentDomain" context
        navigate('/workspaces');
    };

    return (
        <div className="domain-selection-container">
            <div className="domain-selection-content">
                <header className="domain-header">
                    <h1>도메인 선택</h1>
                    <p>작업할 도메인을 선택해주세요.</p>
                </header>

                <div className="domain-grid">
                    {domains.map(domain => (
                        <div key={domain.id} className="domain-card" onClick={() => handleSelectDomain(domain.id)}>
                            <div className="domain-icon">{domain.icon}</div>
                            <h3>{domain.name}</h3>
                            <p>{domain.description}</p>
                            <button className="select-btn">선택</button>
                        </div>
                    ))}
                </div>

                {/* Admin direct link if needed, though they can also go through a domain */}
                <div className="user-info">
                    <span>로그인: {user?.email || user?.username}</span>
                    <button className="logout-text-btn" onClick={logout}>로그아웃</button>
                </div>
            </div>
        </div>
    );
}

export default DomainSelection;
