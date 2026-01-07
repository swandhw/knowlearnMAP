import { createContext, useContext, useState, useCallback } from 'react';

const AlertContext = createContext();

export const useAlert = () => useContext(AlertContext);

export const AlertProvider = ({ children }) => {
    const [alertState, setAlertState] = useState({
        isOpen: false,
        message: '',
        title: '알림',
        type: 'alert', // 'alert' or 'confirm'
        onConfirm: null,
        onCancel: null
    });

    const showAlert = useCallback((message, options = {}) => {
        setAlertState({
            isOpen: true,
            message,
            title: options.title || '알림',
            type: 'alert',
            onConfirm: options.onConfirm || null,
            onCancel: null
        });
    }, []);

    const showConfirm = useCallback((message, options = {}) => {
        return new Promise((resolve) => {
            setAlertState({
                isOpen: true,
                message,
                title: options.title || '확인',
                type: 'confirm',
                onConfirm: () => {
                    setAlertState(prev => ({ ...prev, isOpen: false }));
                    resolve(true);
                },
                onCancel: () => {
                    setAlertState(prev => ({ ...prev, isOpen: false }));
                    resolve(false);
                }
            });
        });
    }, []);

    const closeAlert = useCallback(() => {
        if (alertState.onConfirm && alertState.type === 'alert') {
            alertState.onConfirm();
        }
        setAlertState(prev => ({ ...prev, isOpen: false }));
    }, [alertState]);

    const handleConfirm = useCallback(() => {
        if (alertState.onConfirm) {
            alertState.onConfirm();
        }
    }, [alertState]);

    const handleCancel = useCallback(() => {
        if (alertState.onCancel) {
            alertState.onCancel();
        }
    }, [alertState]);

    return (
        <AlertContext.Provider value={{ showAlert, showConfirm, closeAlert, handleConfirm, handleCancel, alertState }}>
            {children}
        </AlertContext.Provider>
    );
};
