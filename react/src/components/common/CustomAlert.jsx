import React from 'react';
import { useAlert } from '../../context/AlertContext';
import './CustomAlert.css';

const CustomAlert = () => {
    const { alertState, closeAlert, handleConfirm, handleCancel } = useAlert();

    if (!alertState.isOpen) return null;

    const isConfirm = alertState.type === 'confirm';

    return (
        <div className="custom-alert-overlay">
            <div className="custom-alert-container">
                <div className="custom-alert-header">
                    <h2 className="custom-alert-title">{alertState.title}</h2>
                </div>
                <div className="custom-alert-body">
                    <p>{alertState.message}</p>
                </div>
                <div className="custom-alert-footer">
                    {isConfirm ? (
                        <>
                            <button className="custom-alert-btn custom-alert-btn-cancel" onClick={handleCancel}>
                                취소
                            </button>
                            <button className="custom-alert-btn custom-alert-btn-confirm" onClick={handleConfirm}>
                                확인
                            </button>
                        </>
                    ) : (
                        <button className="custom-alert-btn" onClick={closeAlert}>
                            확인
                        </button>
                    )}
                </div>
            </div>
        </div>
    );
};

export default CustomAlert;
