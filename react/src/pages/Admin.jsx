import AdminUpgradeRequests from './admin/AdminUpgradeRequests';
import { useState } from 'react';
import { useNavigate, Routes, Route, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import PromptList from '../prompt/components/prompts/PromptList';
import DomainManagement from '../components/DomainManagement';
import PromptDetail from '../prompt/components/prompts/PromptDetail';

function Admin() {
    const [activeTab, setActiveTab] = useState('prompts');
    const { isAdmin } = useAuth();
    const navigate = useNavigate();
    const location = useLocation();

    // Sync tab with URL if needed, or just let routing handle it. 
    // The current Admin.jsx uses internal state 'activeTab' for the root route, which is confusing if getting there via direct link.
    // However, MainLayout links to /admin/upgrades, which hits the Router *inside* Admin.jsx

    // Let's support the direct route `/admin/upgrades`

    if (!isAdmin) {
        navigate('/');
        return null;
    }

    return (
        <div className="admin-container">
            {/* Header */}
            <header className="app-header">
                <div className="header-left">
                    <button className="icon-btn" onClick={() => navigate('/')}>
                        <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
                            <path d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z" />
                        </svg>
                    </button>
                    <div className="logo">
                        <span className="logo-text">KNOWLEARN MAP - 관리</span>
                    </div>
                </div>
            </header>

            <Routes>
                <Route path="/" element={
                    <div className="admin-content">
                        {/* Admin Tab Navigation */}
                        <nav className="admin-tab-navigation">
                            <button
                                className={`admin-tab ${activeTab === 'prompts' ? 'active' : ''}`}
                                onClick={() => setActiveTab('prompts')}
                            >
                                📝 프롬프트 관리
                            </button>
                            <button
                                className={`admin-tab ${activeTab === 'domains' ? 'active' : ''}`}
                                onClick={() => setActiveTab('domains')}
                            >
                                🌐 도메인 관리
                            </button>
                            <button
                                className={`admin-tab ${activeTab === 'upgrades' ? 'active' : ''}`}
                                onClick={() => setActiveTab('upgrades')}
                            >
                                ⭐ 승인 관리
                            </button>
                        </nav>

                        {/* Tab Content */}
                        <div className="admin-tab-content">
                            {activeTab === 'prompts' && <PromptList />}
                            {activeTab === 'domains' && <DomainManagement />}
                            {activeTab === 'upgrades' && <AdminUpgradeRequests />}
                        </div>
                    </div>
                } />
                <Route path="/prompts" element={<PromptList />} />
                <Route path="/prompts/:code" element={<PromptDetail />} />
                <Route path="/upgrades" element={<AdminUpgradeRequests />} />
                <Route path="/domains" element={<DomainManagement />} />
            </Routes>
        </div>
    );
}

export default Admin;
