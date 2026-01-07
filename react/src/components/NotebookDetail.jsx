import { useState, useRef, useEffect, useCallback } from 'react';
import { useParams, useNavigate, useLocation } from 'react-router-dom';
import { workspaceApi, ontologyApi } from '../services/api';

import { documentApi } from '../services/documentApi';
import { useAuth } from '../context/AuthContext';
import { useAlert } from '../context/AlertContext';
import { PanelLeftClose, PanelLeftOpen, PanelRightClose, PanelRightOpen, MessageSquare, Network, Book, FileText, Presentation, FileBarChart, Plus, Upload, Trash2, Search, RefreshCw } from 'lucide-react';

import DocumentSourceItem from './DocumentSourceItem';
import AddSourceModal from './AddSourceModal';
import KnowledgeMapView from './KnowledgeMapView';
import DictionaryView from './DictionaryView';
import RenameDialog from './RenameDialog';
import SlideCreationModal from './SlideCreationModal';
import ReportGenerationModal from './ReportGenerationModal';

import './NotebookDetail.css';

function NotebookDetail() {
    const { id } = useParams();
    const navigate = useNavigate();
    const location = useLocation();
    const { user } = useAuth();
    const { showAlert } = useAlert();

    // --- State: Layout & Tabs ---
    const [leftSidebarOpen, setLeftSidebarOpen] = useState(true);
    const [rightSidebarOpen, setRightSidebarOpen] = useState(true);
    const [activeTab, setActiveTab] = useState('chat'); // chat, graph, dictionary

    // --- State: Data ---
    const [notebook, setNotebook] = useState(null);
    const [documents, setDocuments] = useState([]);
    const [loading, setLoading] = useState(true);

    // --- State: Chat ---
    const [messages, setMessages] = useState([]);
    const [inputMessage, setInputMessage] = useState('');
    const [isProcessing, setIsProcessing] = useState(false);
    const messagesEndRef = useRef(null);

    // --- State: Source Management ---
    const [uploadModalOpen, setUploadModalOpen] = useState(false);
    const [uploading, setUploading] = useState(false);
    const [isSyncing, setIsSyncing] = useState(false);
    const [progressMap, setProgressMap] = useState({}); // documentId -> { status, progress, stage }
    const pollingRef = useRef({});

    // --- State: Document Selection ---
    const [selectedDocumentIds, setSelectedDocumentIds] = useState([]);

    // --- State: Rename ---
    const [renameDialogOpen, setRenameDialogOpen] = useState(false);
    const [documentToRename, setDocumentToRename] = useState(null);

    // --- State: Studio Modals ---
    const [slideModalOpen, setSlideModalOpen] = useState(false);
    const [reportModalOpen, setReportModalOpen] = useState(false);

    // --- Helper Functions ---
    const fetchNotebook = async () => {
        try {
            const data = await workspaceApi.getById(id);
            setNotebook(data);
            if (data && data.needsArangoSync !== undefined) {
                setIsSyncNeeded(data.needsArangoSync);
            }
        } catch (error) {
            console.error('Error fetching notebook:', error);
            // navigate('/workspaces'); // Optional: redirect on error
        }
    };

    const handleContentUpdate = () => {
        fetchNotebook();
    };

    // ... Polling Logic ... (Existing code)


    const startProgressPolling = (documentId) => {
        if (pollingRef.current[documentId]) return;

        const poll = async () => {
            try {
                const status = await documentApi.getPipelineStatus(documentId);
                setProgressMap(prev => ({
                    ...prev,
                    [documentId]: status
                }));

                if (status.status === 'COMPLETED' || status.status === 'FAILED') {
                    clearInterval(pollingRef.current[documentId]);
                    delete pollingRef.current[documentId];
                    // Update local state instead of refetching to prevent infinite loops caused by stale backend data
                    setDocuments(prev => prev.map(doc =>
                        doc.id === documentId
                            ? { ...doc, status: status.status, pipelineStatus: status.status }
                            : doc
                    ));
                }
            } catch (error) {
                console.error(`Polling error for ${documentId}:`, error);
                clearInterval(pollingRef.current[documentId]);
                delete pollingRef.current[documentId];
            }
        };

        // Initial call
        poll();
        // Set interval
        pollingRef.current[documentId] = setInterval(poll, 2000);
    };

    const fetchDocuments = useCallback(async () => {
        if (!id) return;
        try {
            const data = await documentApi.getByWorkspace(id);
            setDocuments(data || []);

            // Check for processing documents to start polling
            data?.forEach(doc => {
                if (doc.pipelineStatus === 'PROCESSING' || doc.pipelineStatus === 'PENDING') {
                    startProgressPolling(doc.id);
                }
            });
        } catch (error) {
            console.error('Error fetching documents:', error);
        } finally {
            setLoading(false);
        }
    }, [id]);

    // --- Initial Load ---
    useEffect(() => {
        fetchNotebook();
        fetchDocuments();

        // Check for state from navigation (e.g. create new workspace -> open add source)
        if (location.state?.openAddSource) {
            setUploadModalOpen(true);
            // Clear state to prevent reopening on refresh
            window.history.replaceState({}, document.title);
        }

        return () => {
            // Cleanup polling
            Object.values(pollingRef.current).forEach(intervalId => clearInterval(intervalId));
        };
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [id, fetchDocuments]);

    // --- Event Handlers: Chat ---
    const handleSendMessage = async (e) => {
        e?.preventDefault();
        if (!inputMessage.trim() || isProcessing) return;

        const userMsg = { role: 'user', content: inputMessage, timestamp: new Date() };
        setMessages(prev => [...prev, userMsg]);
        setInputMessage('');
        setIsProcessing(true);

        try {
            // Mock response for now, replace with actual API call
            // const response = await chatApi.send(id, inputMessage, selectedDocumentIds);
            setTimeout(() => {
                setMessages(prev => [...prev, {
                    role: 'assistant',
                    content: `Echo: ${userMsg.content} (Backend integration pending)`,
                    timestamp: new Date()
                }]);
                setIsProcessing(false);
            }, 1000);
        } catch (error) {
            console.error('Chat error:', error);
            setIsProcessing(false);
        }
    };

    const handleKeyPress = (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            handleSendMessage(e);
        }
    };

    useEffect(() => {
        messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
    }, [messages]);

    // --- Event Handlers: Source ---
    const handleCheckDocument = (docId) => {
        setSelectedDocumentIds(prev =>
            prev.includes(docId)
                ? prev.filter(id => id !== docId)
                : [...prev, docId]
        );
    };

    // --- Read Only Check ---
    const isReadOnly = notebook?.role !== 'Owner';

    // --- State: Sync Status ---
    const [isSyncNeeded, setIsSyncNeeded] = useState(false);

    // ... existing handlers ...

    const handleDeleteDocument = async (doc) => {
        if (isReadOnly) return;
        if (!window.confirm(`'${doc.filename}' 문서를 삭제하시겠습니까?`)) return;
        try {
            await documentApi.delete(doc.id);
            setDocuments(prev => prev.filter(d => d.id !== doc.id));
            setSelectedDocumentIds(prev => prev.filter(id => id !== doc.id));
            setIsSyncNeeded(true); // Mark sync needed on delete
        } catch (err) {
            console.error('삭제 실패:', err);
            showAlert('문서 삭제 실패');
        }
    };

    const handleRenameDocument = (doc) => {
        if (isReadOnly) return;
        setDocumentToRename(doc);
        setRenameDialogOpen(true);
    };

    const handleRenameConfirm = async (newTitle) => {
        try {
            await documentApi.rename(documentToRename.id, newTitle);
            console.log("Renaming to", newTitle);
            setDocuments(prev => prev.map(d => d.id === documentToRename.id ? { ...d, filename: newTitle } : d));
            setRenameDialogOpen(false);
            setDocumentToRename(null);
        } catch (err) {
            console.error('이름 변경 실패:', err);
        }
    };

    const handleUploadComplete = () => {
        fetchDocuments();
        setUploadModalOpen(false);
        setIsSyncNeeded(true); // Mark sync needed on upload
    };

    const handleSync = async () => {
        if (isReadOnly) return;
        if (!isSyncNeeded && !window.confirm('변경된 사항이 없습니다. 그래도 동기화를 진행하시겠습니까?')) return;
        if (isSyncNeeded && !window.confirm('ArangoDB와 동기화를 진행하시겠습니까?')) return;

        setIsSyncing(true);
        try {
            await ontologyApi.sync(id, true);
            showAlert('동기화가 완료되었습니다.');
            fetchDocuments();
            setIsSyncNeeded(false); // Reset sync needed
        } catch (error) {
            console.error('동기화 실패:', error);
            showAlert('동기화 실패: ' + (error.message || '알 수 없는 오류'));
        } finally {
            setIsSyncing(false);
        }
    };

    const handleSelectAll = (e) => {
        if (e.target.checked) {
            setSelectedDocumentIds(documents.map(d => d.id));
        } else {
            setSelectedDocumentIds([]);
        }
    };

    const handleTabChange = (tab) => {
        if ((tab === 'graph' || tab === 'dictionary') && selectedDocumentIds.length === 0 && documents.length > 0) {
            setSelectedDocumentIds(documents.map(d => d.id));
        }
        setActiveTab(tab);
    };

    // --- Render ---
    if (loading) return <div className="loading-screen">Loading Workspace...</div>;

    return (
        <div className="notebook-layout">
            {/* Left Panel: Sources */}
            <div className={`panel panel-left ${leftSidebarOpen ? '' : 'collapsed'}`}>
                <div className="panel-header">
                    {leftSidebarOpen && <div className="panel-title"><FileText size={18} /> 소스 자료</div>}
                    <button className="panel-toggle-btn" onClick={() => setLeftSidebarOpen(!leftSidebarOpen)}>
                        {leftSidebarOpen ? <PanelLeftClose size={18} /> : <PanelLeftOpen size={18} />}
                    </button>
                </div>
                <div className="panel-body">
                    {leftSidebarOpen ? (
                        <>
                            <div className="source-actions" style={{ marginBottom: '10px', display: 'flex', flexDirection: 'column', gap: '10px' }}>
                                {!isReadOnly && (
                                    <button className="add-source-btn" onClick={() => setUploadModalOpen(true)} style={{
                                        width: '100%', padding: '10px', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '6px',
                                        backgroundColor: '#1a73e8', color: 'white', border: 'none', borderRadius: '20px', cursor: 'pointer', fontSize: '14px', fontWeight: '500'
                                    }}>
                                        <Plus size={18} /> 소스 추가
                                    </button>
                                )}

                                <div style={{ position: 'relative' }}>
                                    <Search size={16} style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: '#666' }} />
                                    <input
                                        type="text"
                                        placeholder="찾아내 새 소스 검색하세요"
                                        style={{ width: '100%', padding: '8px 8px 8px 36px', borderRadius: '8px', border: 'none', backgroundColor: '#f1f3f4', fontSize: '13px' }}
                                    />
                                </div>

                                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '0 4px' }}>
                                    <label style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '12px', color: '#666', cursor: 'pointer' }}>
                                        <input
                                            type="checkbox"
                                            checked={documents.length > 0 && selectedDocumentIds.length === documents.length}
                                            onChange={handleSelectAll}
                                        />
                                        모두 선택
                                    </label>
                                    <div style={{ display: 'flex', gap: '8px' }}>
                                        {!isReadOnly && (
                                            <button
                                                onClick={handleSync}
                                                title="동기화 (ArangoDB)"
                                                disabled={isSyncing}
                                                style={{
                                                    background: 'none',
                                                    border: 'none',
                                                    cursor: isSyncing ? 'wait' : 'pointer',
                                                    color: isSyncNeeded ? '#d93025' : '#5f6368', // Red if needed, else Grey
                                                    padding: '4px',
                                                    opacity: isSyncing ? 0.6 : 1
                                                }}
                                            >
                                                <RefreshCw size={16} className={isSyncing ? "spin-animation" : ""} />
                                            </button>
                                        )}
                                        <button
                                            onClick={() => handleTabChange('graph')}
                                            disabled={selectedDocumentIds.length === 0}
                                            title="지식 그래프"
                                            style={{
                                                background: 'none',
                                                border: 'none',
                                                cursor: selectedDocumentIds.length > 0 ? 'pointer' : 'not-allowed',
                                                color: selectedDocumentIds.length > 0 ? '#d93025' : '#ccc',
                                                padding: '4px'
                                            }}
                                        >
                                            <Network size={16} />
                                        </button>
                                        <button
                                            onClick={() => handleTabChange('dictionary')}
                                            disabled={selectedDocumentIds.length === 0}
                                            title="사전"
                                            style={{
                                                background: 'none',
                                                border: 'none',
                                                cursor: selectedDocumentIds.length > 0 ? 'pointer' : 'not-allowed',
                                                color: selectedDocumentIds.length > 0 ? '#d93025' : '#ccc',
                                                padding: '4px'
                                            }}
                                        >
                                            <Book size={16} />
                                        </button>
                                    </div>
                                </div>
                            </div>
                            <div className="source-list">
                                {documents.length === 0 ? (
                                    <div style={{ padding: '20px', textAlign: 'center', color: '#999', fontSize: '13px' }}>
                                        등록된 문서가 없습니다.<br />자료를 추가해보세요.
                                    </div>
                                ) : (
                                    documents.map(doc => (
                                        <DocumentSourceItem
                                            key={doc.id}
                                            document={doc}
                                            progress={progressMap[doc.id]} // Pass polling status
                                            isChecked={selectedDocumentIds.includes(doc.id)}
                                            onCheckChange={handleCheckDocument}
                                            onSelect={() => {/* Maybe preview? */ }}
                                            onRename={handleRenameDocument}
                                            onDelete={handleDeleteDocument}
                                            readOnly={isReadOnly}
                                        />
                                    ))
                                )}
                            </div>
                        </>
                    ) : (
                        <div className="vertical-text">SOURCE</div>
                    )}
                </div>
            </div>

            {/* Center Panel: Content */}
            <div className="panel panel-center">
                <div className="tabs-header">
                    <button
                        className={`tab-btn ${activeTab === 'chat' ? 'active' : ''}`}
                        onClick={() => handleTabChange('chat')}
                    >
                        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '6px' }}>
                            <MessageSquare size={16} /> 채팅
                        </div>
                    </button>
                    <button
                        className={`tab-btn ${activeTab === 'graph' ? 'active' : ''}`}
                        onClick={() => handleTabChange('graph')}
                    >
                        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '6px' }}>
                            <Network size={16} /> 지식 그래프
                        </div>
                    </button>
                    <button
                        className={`tab-btn ${activeTab === 'dictionary' ? 'active' : ''}`}
                        onClick={() => handleTabChange('dictionary')}
                    >
                        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '6px' }}>
                            <Book size={16} /> 사전
                        </div>
                    </button>
                </div>

                <div className="tab-content">
                    {activeTab === 'chat' && (
                        <div className="chat-container">
                            <div className="chat-messages">
                                {messages.length === 0 && notebook && (
                                    <div className="notebook-welcome-header">
                                        <div className="workspace-icon-large" style={{ fontSize: '48px', marginBottom: '16px' }}>
                                            {notebook.icon || '📄'}
                                        </div>
                                        <h1 className="workspace-title" style={{ fontSize: '32px', fontWeight: '400', color: '#202124', marginBottom: '8px', lineHeight: '1.2' }}>
                                            {notebook.name || 'Untitled notebook'}
                                        </h1>
                                        <div className="workspace-meta" style={{ fontSize: '13px', color: '#5f6368', marginBottom: '24px' }}>
                                            소스 {documents.length}개
                                        </div>
                                        {notebook.description && (
                                            <div className="workspace-description" style={{ fontSize: '16px', color: '#3c4043', lineHeight: '1.6', maxWidth: '800px' }}>
                                                {notebook.description}
                                            </div>
                                        )}
                                    </div>
                                )}
                                {messages.map((msg, idx) => (
                                    <div key={idx} className={`message ${msg.role}`} style={{
                                        display: 'flex', flexDirection: 'column',
                                        alignItems: msg.role === 'user' ? 'flex-end' : 'flex-start',
                                        marginBottom: '16px'
                                    }}>
                                        <div style={{
                                            maxWidth: '80%', padding: '12px 16px', borderRadius: '12px',
                                            backgroundColor: msg.role === 'user' ? '#e3f2fd' : '#f5f5f5',
                                            color: '#333', fontSize: '14px', lineHeight: '1.5'
                                        }}>
                                            {msg.content}
                                        </div>
                                        <span style={{ fontSize: '11px', color: '#aaa', marginTop: '4px' }}>
                                            {msg.timestamp.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                                        </span>
                                    </div>
                                ))}
                                <div ref={messagesEndRef} />
                            </div>
                            <div className="message-input-area">
                                <textarea
                                    value={inputMessage}
                                    onChange={(e) => setInputMessage(e.target.value)}
                                    onKeyPress={handleKeyPress}
                                    placeholder="질문을 입력하세요..."
                                    style={{
                                        width: '100%', height: '80px', padding: '12px', borderRadius: '8px',
                                        border: '1px solid #ddd', resize: 'none', fontFamily: 'inherit'
                                    }}
                                    disabled={isProcessing}
                                />
                                <div style={{ textAlign: 'right', marginTop: '8px' }}>
                                    <button
                                        onClick={handleSendMessage}
                                        disabled={!inputMessage.trim() || isProcessing}
                                        style={{
                                            padding: '8px 24px', backgroundColor: '#1a73e8', color: 'white',
                                            border: 'none', borderRadius: '4px', cursor: 'pointer', opacity: isProcessing ? 0.7 : 1
                                        }}
                                    >
                                        전송
                                    </button>
                                </div>
                            </div>
                        </div>
                    )}

                    {activeTab === 'graph' && (
                        <KnowledgeMapView
                            workspaceId={id}
                            documents={documents}
                            initialSelectedDocIds={selectedDocumentIds}
                            readOnly={isReadOnly}
                        />
                    )}

                    {activeTab === 'dictionary' && (
                        <DictionaryView
                            workspaceId={id}
                            initialSelectedDocIds={selectedDocumentIds}
                            onUpdate={handleContentUpdate}
                            readOnly={isReadOnly}
                        />
                    )}
                </div>
            </div>

            {/* Right Panel: Studio */}
            <div className={`panel panel-right ${rightSidebarOpen ? '' : 'collapsed'}`}>
                <div className="panel-header">
                    <button className="panel-toggle-btn" onClick={() => setRightSidebarOpen(!rightSidebarOpen)} style={{ marginRight: 'auto' }}>
                        {rightSidebarOpen ? <PanelRightClose size={18} /> : <PanelRightOpen size={18} />}
                    </button>
                    {rightSidebarOpen && <div className="panel-title">스튜디오</div>}
                </div>
                <div className="panel-body">
                    {rightSidebarOpen ? (
                        <>
                            <div className="studio-item">
                                <div className="studio-icon"><Presentation size={20} /></div>
                                <div className="studio-info">
                                    <h4>슬라이드 생성</h4>
                                    <p>문서를 기반으로 발표 자료 생성</p>
                                </div>
                                <button className="studio-action-btn" onClick={() => setSlideModalOpen(true)}><Plus size={16} /></button>
                            </div>

                            <div className="studio-item">
                                <div className="studio-icon"><FileBarChart size={20} /></div>
                                <div className="studio-info">
                                    <h4>보고서 작성</h4>
                                    <p>데이터 분석 및 리포트 작성</p>
                                </div>
                                <button className="studio-action-btn" onClick={() => setReportModalOpen(true)}><Plus size={16} /></button>
                            </div>

                            <div style={{ marginTop: '20px', padding: '12px', backgroundColor: '#f9f9f9', borderRadius: '8px' }}>
                                <h4 style={{ margin: '0 0 8px 0', fontSize: '13px', color: '#555' }}>최근 생성물</h4>
                                <div style={{ fontSize: '12px', color: '#999', textAlign: 'center', padding: '10px' }}>
                                    생성된 항목이 없습니다.
                                </div>
                            </div>
                        </>
                    ) : (
                        <div className="vertical-text">STUDIO</div>
                    )}
                </div>
            </div>

            {/* Modals */}
            <AddSourceModal
                isOpen={uploadModalOpen}
                onClose={() => setUploadModalOpen(false)}
                onUploadComplete={handleUploadComplete}
                workspaceId={id}
            />

            <RenameDialog
                isOpen={renameDialogOpen}
                onClose={() => setRenameDialogOpen(false)}
                title="문서 이름 변경"
                currentName={documentToRename?.filename || ''}
                onConfirm={handleRenameConfirm}
            />

            <SlideCreationModal
                isOpen={slideModalOpen}
                onClose={() => setSlideModalOpen(false)}
                workspaceId={id}
            />

            <ReportGenerationModal
                isOpen={reportModalOpen}
                onClose={() => setReportModalOpen(false)}
                workspaceId={id}
            />

        </div>
    );
}

export default NotebookDetail;
