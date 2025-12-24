import React, { useState, useEffect } from 'react';
import './RenameDialog.css';

function RenameDialog({ document, onClose, onConfirm }) {
    const [newName, setNewName] = useState(document?.filename || '');

    useEffect(() => {
        if (document) {
            setNewName(document.filename);
        }
    }, [document]);

    const handleSubmit = (e) => {
        e.preventDefault();
        if (newName.trim()) {
            onConfirm(newName.trim());
        }
    };

    if (!document) return null;

    return (
        <div className="rename-dialog-overlay" onClick={onClose}>
            <div className="rename-dialog" onClick={(e) => e.stopPropagation()}>
                <h3>제목 수정</h3>
                <form onSubmit={handleSubmit}>
                    <input
                        type="text"
                        value={newName}
                        onChange={(e) => setNewName(e.target.value)}
                        placeholder="새 제목 입력"
                        autoFocus
                    />
                    <div className="dialog-buttons">
                        <button type="button" onClick={onClose} className="cancel-btn">
                            취소
                        </button>
                        <button type="submit" className="confirm-btn">
                            저장
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}

export default RenameDialog;
