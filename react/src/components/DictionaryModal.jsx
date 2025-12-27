import React from 'react';
import DictionaryView from './DictionaryView';
import './KnowledgeGraphModal.css'; // Reusing the same modal styles

export default function DictionaryModal({ isOpen, onClose, workspaceId }) {
    if (!isOpen) return null;

    return (
        <div className="kg-modal-overlay" onClick={onClose}>
            <div
                className="kg-modal-content"
                style={{ maxWidth: 'none', width: '98%', height: '98vh', display: 'flex', flexDirection: 'column' }}
                onClick={e => e.stopPropagation()}
            >
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
                    <h2 className="kg-modal-title" style={{ margin: 0 }}>사전</h2>
                    <button className="kg-close-btn" style={{ margin: 0 }} onClick={onClose}>닫기</button>
                </div>
                <div style={{ flex: 1, overflow: 'hidden' }}>
                    <DictionaryView workspaceId={workspaceId} />
                </div>
            </div>
        </div>
    );
}
