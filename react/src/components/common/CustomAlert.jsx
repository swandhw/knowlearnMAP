import React from 'react';
import { useAlert } from '../../context/AlertContext';
import './CustomAlert.css';

const CustomAlert = () => {
    const { alertState, closeAlert } = useAlert();

    if (!alertState.isOpen) return null;

    return (
        <div className="custom-alert-overlay">
            <div className="custom-alert-container">
                {/* No Close Button (X) as per request */}
                <div className="custom-alert-header">
                    <h2 className="custom-alert-title">{alertState.title}</h2>
                </div>
                <div className="custom-alert-body">
                    <p>{alertState.message}</p>
                </div>
                <div className="custom-alert-footer">
                    <button className="custom-alert-btn" onClick={closeAlert}>
                        확인
                    </button>
                </div>
            </div>
        </div>
    );
};

export default CustomAlert;
