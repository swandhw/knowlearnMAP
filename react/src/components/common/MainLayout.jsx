import { useRef, useState } from 'react';
import { Outlet, NavLink, useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { LogOut, Star, User, Layers, MessageSquare, LayoutGrid, Globe, ArrowUpCircle } from 'lucide-react';
import UpgradeModal from '../UpgradeModal';
import './MainLayout.css';

function MainLayout() {
  const { user, isAdmin, logout } = useAuth();
  const navigate = useNavigate();
  const [upgradeModalOpen, setUpgradeModalOpen] = useState(false);

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <div className="main-layout">
      {/* GNB Header */}
      <header className="gnb-header">
        <div className="gnb-left">
          <Link to="/workspaces" className="site-logo">
            <LayoutGrid size={24} color="#1a73e8" />
            <span>KNOWLEARN MAP</span>
          </Link>
        </div>

        <nav className="gnb-menu">
          <NavLink to="/" className={({ isActive }) => `menu-item ${isActive ? 'active' : ''}`} end>
            전체
          </NavLink>

          <NavLink to="/workspaces" className={({ isActive }) => `menu-item ${isActive ? 'active' : ''}`}>
            내 워크스페이스
          </NavLink>

          {isAdmin && (
            <>
              <NavLink to="/admin/domains" className={({ isActive }) => `menu-item ${isActive ? 'active' : ''}`}>
                <Globe size={16} style={{ marginRight: '4px', verticalAlign: 'text-bottom' }} />
                도메인 관리
              </NavLink>
              <NavLink to="/admin/prompts" className={({ isActive }) => `menu-item ${isActive ? 'active' : ''}`}>
                <MessageSquare size={16} style={{ marginRight: '4px', verticalAlign: 'text-bottom' }} />
                프롬프트 관리
              </NavLink>
              <NavLink to="/admin/upgrades" className={({ isActive }) => `menu-item ${isActive ? 'active' : ''}`}>
                <Star size={16} style={{ marginRight: '4px', verticalAlign: 'text-bottom' }} />
                승인 관리
              </NavLink>
            </>
          )}
        </nav>

        <div className="gnb-right">
          {(!user?.grade || user.grade !== 'MAX') && (
            <button className="upgrade-btn" onClick={() => setUpgradeModalOpen(true)} title="등급 업그레이드">
              <ArrowUpCircle size={20} color="#FFD700" />
            </button>
          )}

          <div className="user-info" style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            {user?.grade && user.grade !== 'FREE' && (
              <span className={`grade-badge ${user.grade.toLowerCase()}`}>
                {user.grade}
              </span>
            )}
            {user?.email || user?.username || 'User'}
          </div>

          <button className="logout-btn" onClick={handleLogout} title="로그아웃">
            <LogOut size={20} />
          </button>
        </div>
      </header>

      {/* Main Content */}
      <main className="main-content">
        <Outlet />
      </main>

      {/* Footer */}
      <footer className="site-footer">
        <p>© 2025 KNOWLEARN MAP. All rights reserved.</p>
      </footer>

      <UpgradeModal
        isOpen={upgradeModalOpen}
        onClose={() => setUpgradeModalOpen(false)}
      />
    </div>
  );
}

export default MainLayout;
