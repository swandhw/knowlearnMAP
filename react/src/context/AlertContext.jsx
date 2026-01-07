import { createContext, useContext, useState, useCallback } from 'react';

const AlertContext = createContext();

export const useAlert = () => useContext(AlertContext);

export const AlertProvider = ({ children }) => {
    const [alertState, setAlertState] = useState({
        isOpen: false,
        message: '',
        title: '알림', // Default title
        onConfirm: null
    });

    const showAlert = useCallback((message, options = {}) => {
        setAlertState({
            isOpen: true,
            message,
            title: options.title || '알림',
            onConfirm: options.onConfirm || null
        });
    }, []);

    const closeAlert = useCallback(() => {
        if (alertState.onConfirm) {
            alertState.onConfirm();
        }
        setAlertState(prev => ({ ...prev, isOpen: false }));
    }, [alertState]);

    return (
        <AlertContext.Provider value={{ showAlert, closeAlert, alertState }}>
            {children}
        </AlertContext.Provider>
    );
};
