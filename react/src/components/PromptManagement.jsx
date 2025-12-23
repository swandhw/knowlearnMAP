// material-ui
import Grid from '@mui/material/Grid';
import Typography from '@mui/material/Typography';
import Box from '@mui/material/Box';
import Chip from '@mui/material/Chip';
import IconButton from '@mui/material/IconButton';
import UploadFileIcon from '@mui/icons-material/UploadFile';
import SettingsIcon from '@mui/icons-material/Settings';
import ImageIcon from '@mui/icons-material/Image';
import React, { useRef, useState, useEffect } from 'react';
import Table from '@mui/material/Table';
import TableBody from '@mui/material/TableBody';
import TableCell from '@mui/material/TableCell';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableRow from '@mui/material/TableRow';
import Paper from '@mui/material/Paper';
import InputBase from '@mui/material/InputBase';
import OutlinedInput from '@mui/material/OutlinedInput';
import InputAdornment from '@mui/material/InputAdornment';
import LinearProgress from '@mui/material/LinearProgress';
import Stack from '@mui/material/Stack';
import Fab from '@mui/material/Fab';
import Menu from '@mui/material/Menu';
import MenuItem from '@mui/material/MenuItem';
import EditIcon from '@mui/icons-material/Edit';
import DeleteIcon from '@mui/icons-material/Delete';
import Dialog from '@mui/material/Dialog';
import DialogTitle from '@mui/material/DialogTitle';
import DialogContent from '@mui/material/DialogContent';
import DialogActions from '@mui/material/DialogActions';
import Button from '@mui/material/Button';
import TextField from '@mui/material/TextField';
import FormControlLabel from '@mui/material/FormControlLabel';
import Checkbox from '@mui/material/Checkbox';
import InputLabel from '@mui/material/InputLabel';
import Select from '@mui/material/Select';
import FormControl from '@mui/material/FormControl';
import CloudUploadOutlinedIcon from '@mui/icons-material/CloudUploadOutlined';
import Link from '@mui/material/Link';
import ChevronLeftIcon from '@mui/icons-material/ChevronLeft';
import ChevronRightIcon from '@mui/icons-material/ChevronRight';
import RefreshIcon from '@mui/icons-material/Refresh';
import SearchOutlined from '@ant-design/icons/SearchOutlined';
import MinusOutlined from '@ant-design/icons/MinusOutlined';
import AddIcon from '@mui/icons-material/Add';
import CloudUploadIcon from '@mui/icons-material/CloudUpload';
import EmailIcon from '@mui/icons-material/Email';
import StorageIcon from '@mui/icons-material/Storage';
import DownloadOutlined from '@ant-design/icons/DownloadOutlined';

import { v4 as uuidv4 } from 'uuid';

// API 호출을 위한 fetch 함수
const apiCall = async (endpoint, options = {}) => {
    try {
        const response = await fetch(`http://localhost:8080${endpoint}`, {
            headers: {
                'Content-Type': 'application/json',
                ...options.headers,
            },
            ...options,
        });

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const data = await response.json();
        return data;
    } catch (error) {
        console.error('API 호출 실패:', error);
        throw error;
    }
};

export default function PromptManagement() {
    const fileInputRef = React.useRef(null);
    const [searchTerm, setSearchTerm] = React.useState('');
    const [filters, setFilters] = React.useState({
        name: '',
        email: '',
        role: '',
        ageMin: '',
        ageMax: '',
        visitsMin: '',
        visitsMax: '',
        status: '',
        progressMin: '',
        progressMax: ''
    });
    const [menuAnchorEl, setMenuAnchorEl] = React.useState(null);
    const menuOpen = Boolean(menuAnchorEl);
    const [dropdownOpen, setDropdownOpen] = React.useState(false);
    const [selectedDropdownItem, setSelectedDropdownItem] = React.useState('');
    const dropdownRef = useRef(null);
    const [uploadDialogOpen, setUploadDialogOpen] = React.useState(false);
    const [page, setPage] = React.useState(1);
    const rowsPerPage = 50;
    const totalRows = 4484;
    const totalPages = Math.ceil(totalRows / rowsPerPage);
    const startIdx = (page - 1) * rowsPerPage + 1;
    const endIdx = Math.min(page * rowsPerPage, totalRows);
    const handlePrevPage = () => setPage((p) => Math.max(1, p - 1));
    const handleNextPage = () => setPage((p) => Math.min(totalPages, p + 1));
    const [dragActive, setDragActive] = React.useState(false);
    const [files, setFiles] = React.useState([]);
    const [llmModel, setLlmModel] = React.useState('AISTUDIO');
    const [processMode, setProcessMode] = React.useState('STEP_ONLY');
    const workspaceId = '1f84fb65-19eb-4b3c-b6f5-47124386b91a';
    const [knowledgeList, setKnowledgeList] = React.useState([]);
    const [stageDialogOpen, setStageDialogOpen] = useState(false);
    const [stageDetail, setStageDetail] = useState([]);
    const [stageLoading, setStageLoading] = useState(false);

    const fetchKnowledgeList = async () => {
        try {
            const res = await apiCall('/api/v1/knowlearn/knowledges', {
                method: 'GET',
            });
            // URL params 추가가 필요하면 여기에 추가
            setKnowledgeList(res || []);
        } catch (e) {
            setKnowledgeList([]);
        }
    };

    React.useEffect(() => {
        fetchKnowledgeList();
    }, []);

    const handleUploadClick = () => {
        if (fileInputRef.current) {
            fileInputRef.current.click();
        }
    };

    const handleMenuClick = (event) => {
        setMenuAnchorEl(event.currentTarget);
    };

    const handleMenuClose = () => {
        setMenuAnchorEl(null);
    };

    const handleDropdownToggle = () => {
        setDropdownOpen((prev) => !prev);
    };

    const handleDropdownItemClick = (item) => {
        setSelectedDropdownItem(item.text);
        setDropdownOpen(false);
        if (item.text === '업로드') {
            setUploadDialogOpen(true);
        }
    };

    useEffect(() => {
        const handleClickOutside = (event) => {
            if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
                setDropdownOpen(false);
            }
        };
        document.addEventListener('mousedown', handleClickOutside);
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, []);

    // 샘플 데이터 - 10개 항목으로 스크롤 테스트
    const menuItems = [
        { id: 1, icon: <CloudUploadIcon style={{ fontSize: 20, color: '#1976d2' }} />, text: '업로드', enabled: true },
        {
            id: 2, icon: (
                <svg width="20" height="20" viewBox="0 0 48 48" fill="none"><g><path d="M24 44C35.0457 44 44 35.0457 44 24C44 12.9543 35.0457 4 24 4C12.9543 4 4 12.9543 4 24C4 35.0457 12.9543 44 24 44Z" fill="#4285F4" /><path d="M34.5 24.5C34.5 20.0817 30.9183 16.5 26.5 16.5C22.0817 16.5 18.5 20.0817 18.5 24.5C18.5 28.9183 22.0817 32.5 26.5 32.5C30.9183 32.5 34.5 28.9183 34.5 24.5Z" fill="#34A853" /><path d="M24 44C35.0457 44 44 35.0457 44 24C44 12.9543 35.0457 4 24 4C12.9543 4 4 12.9543 4 24C4 35.0457 12.9543 44 24 44Z" fill="#4285F4" fillOpacity="0.2" /></g></svg>
            ), text: 'Google Drive', enabled: false
        },
        { id: 3, icon: <EmailIcon style={{ fontSize: 20, color: '#1976d2' }} />, text: 'Email', enabled: false },
        {
            id: 4, icon: (
                <svg width="20" height="20" viewBox="0 0 32 32"><g><rect fill="#0052CC" width="32" height="32" rx="6" /><path d="M23.6 8.6c-.3-.3-.7-.3-1 0l-7.7 7.7c-.3.3-.3.7 0 1l7.7 7.7c.3.3.7.3 1 0l7.7-7.7c.3-.3.3-.7 0-1l-7.7-7.7z" fill="#2684FF" /><path d="M8.6 8.6c-.3-.3-.7-.3-1 0l-7.7 7.7c-.3.3-.3.7 0 1l7.7 7.7c.3.3.7.3 1 0l7.7-7.7c.3-.3.3-.7 0-1l-7.7-7.7z" fill="#2684FF" /></g></svg>
            ), text: 'Confluence', enabled: false
        },
        { id: 5, icon: <StorageIcon style={{ fontSize: 20, color: '#1976d2' }} />, text: 'Database', enabled: false },
        {
            id: 6, icon: (
                <svg width="20" height="20" viewBox="0 0 32 32"><circle cx="16" cy="16" r="16" fill="#03363D" /><path d="M23.2 10.6c-.2-.2-.5-.2-.7 0l-6.5 6.5c-.2.2-.2.5 0 .7l6.5 6.5c.2.2.5.2.7 0l6.5-6.5c.2-.2.2-.5 0-.7l-6.5-6.5z" fill="#78A9FF" /></svg>
            ), text: 'Zendesk', enabled: false
        },
        {
            id: 7, icon: (
                <svg width="20" height="20" viewBox="0 0 122.8 122.8"><g><circle fill="#611f69" cx="61.4" cy="61.4" r="61.4" /><rect fill="#fff" x="35.6" y="56.1" width="51.6" height="10.6" rx="5.3" /><rect fill="#fff" x="56.1" y="35.6" width="10.6" height="51.6" rx="5.3" /></g></svg>
            ), text: 'Slack', enabled: false
        },
        {
            id: 8, icon: (
                <svg width="20" height="20" viewBox="0 0 32 32"><g><circle fill="#464EB8" cx="16" cy="16" r="16" /><path d="M10 22h12v-2H10v2zm0-4h12v-2H10v2zm0-4h12V8H10v6z" fill="#fff" /></g></svg>
            ), text: 'MS Teams', enabled: false
        },
        {
            id: 9, icon: (
                <svg width="20" height="20" viewBox="0 0 32 32"><g><rect fill="#0052CC" width="32" height="32" rx="6" /><path d="M16 8l-8 16h16L16 8z" fill="#2684FF" /><path d="M16 8l8 16H8l8-16z" fill="#fff" /></g></svg>
            ), text: 'Jira', enabled: false
        }
    ];

    // Dialog form state
    const [kbName, setKbName] = React.useState('');
    const [autoLang, setAutoLang] = React.useState(false);
    const [lang, setLang] = React.useState('ko');

    const handleFileChange = (e) => {
        if (e.target.files && e.target.files.length > 0) {
            if (e.target.files.length > 1) {
                alert('Not available in demo version');
                e.target.value = '';
                return;
            }
            setFiles([e.target.files[0]]);
        }
    };

    const handleDialogClose = () => {
        setUploadDialogOpen(false);
        setKbName('');
        setAutoLang(false);
        setLang('ko');
        setFiles([]);
    };

    // 파일 제거 핸들러
    const handleRemoveFile = (index) => {
        setFiles([]);
    };

    // 드래그 앤 드롭 핸들러
    const handleDragOver = (e) => {
        e.preventDefault();
        e.stopPropagation();
        setDragActive(true);
    };

    const handleDragLeave = (e) => {
        e.preventDefault();
        e.stopPropagation();
        setDragActive(false);
    };

    const handleDrop = (e) => {
        e.preventDefault();
        e.stopPropagation();
        setDragActive(false);
        if (e.dataTransfer.files && e.dataTransfer.files.length > 0) {
            if (e.dataTransfer.files.length > 1) {
                alert('Not available in demo version');
                return;
            }
            setFiles([e.dataTransfer.files[0]]);
            e.dataTransfer.clearData();
        }
    };

    // 등록 버튼 클릭 핸들러
    const handleRegister = async () => {
        if (!files.length) return;
        const knowledgeId = '369e1ccc-d2eb-4f98-8dc6-73bade9bd65d';
        const name = kbName || '지식 이름';
        const linkedService = 'UPLOAD';
        const llmModel = 'AISTUDIO';
        const processMode = 'STEP_ONLY';


        const formData = new FormData();
        formData.append('workspaceId', workspaceId);
        formData.append('knowledgeId', knowledgeId);
        formData.append('name', name);
        formData.append('linkedService', linkedService);
        formData.append('llmModel', llmModel);
        formData.append('processMode', processMode);
        formData.append('files', files[0]);

        try {
            await fetch('http://localhost:8080/api/v1/knowlearn/knowledges', {
                method: 'POST',
                body: formData,
            });
            setUploadDialogOpen(false);
            setKbName('');
            setAutoLang(false);
            setLang('ko');
            setFiles([]);
            fetchKnowledgeList();
        } catch (e) {
            alert('등록에 실패했습니다.');
        }
    };

    const handleRowRefresh = async (knowledgeId) => {
        try {
            const res = await apiCall(`/api/v1/knowlearn/knowledges/${knowledgeId}`);
            setKnowledgeList(prev => prev.map(row => (row.knowledgeId === knowledgeId ? res : row)));
        } catch (e) {
            alert('행 새로고침 실패');
        }
    };

    const fetchStageDetail = async (knowledgeId) => {
        setStageLoading(true);
        try {
            const res = await apiCall(`/api/v1/knowlearn/knowledges/${knowledgeId}/knowledgeStage`);
            setStageDetail(res || []);
        } catch (e) {
            setStageDetail([]);
        }
        setStageLoading(false);
    };

    return (
        <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
            <Paper elevation={0} sx={{ width: '100%', background: 'transparent', boxShadow: 'none', p: 0, flex: 1, display: 'flex', flexDirection: 'column' }}>
                <Stack spacing={2} sx={{ flex: 1, display: 'flex', flexDirection: 'column' }}>
                    <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                        <OutlinedInput
                            placeholder="Search 10 records..."
                            startAdornment={
                                <InputAdornment position="start">
                                    <SearchOutlined />
                                </InputAdornment>
                            }
                            value={searchTerm}
                            onChange={(e) => setSearchTerm(e.target.value)}
                            size="small"
                            sx={{ width: 300 }}
                        />
                        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'flex-end', gap: 2, position: 'relative' }} ref={dropdownRef}>
                            <Box sx={{ display: 'flex', alignItems: 'center', fontSize: 15, color: '#444', mr: 1, gap: 0.5 }}>
                                <span style={{ fontWeight: 400 }}>{totalRows.toLocaleString()}개 중 {startIdx}-{endIdx}</span>
                                <IconButton size="small" onClick={handlePrevPage} disabled={page === 1} sx={{ ml: 1 }}>
                                    <ChevronLeftIcon fontSize="small" />
                                </IconButton>
                                <IconButton size="small" onClick={handleNextPage} disabled={page === totalPages}>
                                    <ChevronRightIcon fontSize="small" />
                                </IconButton>
                            </Box>
                            <Fab color="primary" size="large" aria-label="add" sx={{ minWidth: '56px', minHeight: '56px' }} onClick={handleDropdownToggle}>
                                <AddIcon style={{ fontSize: '2rem' }} />
                            </Fab>
                            {/* 커스텀 드롭다운 메뉴 */}
                            {dropdownOpen && (
                                <div style={{
                                    position: 'absolute',
                                    top: 'calc(100% + 8px)',
                                    right: 0,
                                    minWidth: 200,
                                    maxWidth: 300,
                                    background: 'white',
                                    border: '1px solid #ddd',
                                    borderRadius: 8,
                                    boxShadow: '0 4px 12px rgba(0,0,0,0.12)',
                                    zIndex: 1000,
                                    marginTop: 4
                                }}>
                                    <ul style={{
                                        maxHeight: 240,
                                        overflowY: 'auto',
                                        margin: 0,
                                        padding: 0,
                                        listStyle: 'none'
                                    }}>
                                        {menuItems.map((item) => (
                                            <li
                                                key={item.id}
                                                style={{
                                                    display: 'flex',
                                                    alignItems: 'center',
                                                    padding: '12px 16px',
                                                    cursor: item.enabled ? 'pointer' : 'not-allowed',
                                                    borderBottom: '1px solid #f0f0f0',
                                                    background: selectedDropdownItem === item.text ? '#e3f2fd' : 'white',
                                                    transition: 'background 0.2s',
                                                    opacity: item.enabled ? 1 : 0.6,
                                                    pointerEvents: item.enabled ? 'auto' : 'none',
                                                    position: 'relative',
                                                    minHeight: 56
                                                }}
                                                onClick={item.enabled ? () => handleDropdownItemClick(item) : undefined}
                                                onMouseEnter={e => { if (item.enabled && selectedDropdownItem !== item.text) e.currentTarget.style.background = '#f5f5f5'; }}
                                                onMouseLeave={e => { if (item.enabled && selectedDropdownItem !== item.text) e.currentTarget.style.background = 'white'; }}
                                            >
                                                {/* 붉은 도장 스탬프 - 배경 없이 글씨만 빨간색 */}
                                                {!item.enabled && (
                                                    <span style={{
                                                        position: 'absolute',
                                                        top: 2,
                                                        left: '50%',
                                                        transform: 'translateX(-50%) rotate(-8deg)',
                                                        color: '#e53935',
                                                        fontWeight: 400,
                                                        fontSize: 12,
                                                        zIndex: 2,
                                                        userSelect: 'none',
                                                        pointerEvents: 'none',
                                                        letterSpacing: 0.5,
                                                        fontFamily: 'inherit',
                                                        whiteSpace: 'nowrap'
                                                    }}>
                                                        Not available in demo version
                                                    </span>
                                                )}
                                                <span style={{ marginRight: 12, fontSize: 20, minWidth: 20 }}>{item.icon}</span>
                                                <span style={{ fontSize: 14, color: '#333' }}>{item.text}</span>
                                            </li>
                                        ))}
                                    </ul>
                                </div>
                            )}
                            <IconButton
                                size="small"
                                title="CSV Export"
                                sx={{
                                    background: 'none',
                                    border: 'none',
                                    boxShadow: 'none',
                                    padding: 0,
                                    minWidth: 0,
                                    minHeight: 0,
                                    color: 'primary.main',
                                    '&:hover': { background: 'none' },
                                    '&:focus': { background: 'none' }
                                }}
                            >
                                <DownloadOutlined style={{ fontSize: '2rem', color: '#1976d2' }} />
                            </IconButton>
                        </Box>
                    </Box>
                    <TableContainer component={Paper} elevation={1} sx={{ flex: 1, height: '100%' }}>
                        <Table stickyHeader>
                            <TableHead>
                                <TableRow>
                                    <TableCell sx={{ width: 120, minWidth: 120, maxWidth: 120 }}>연결된서비스</TableCell>
                                    <TableCell sx={{ width: 120, minWidth: 120, maxWidth: 120 }}>이름</TableCell>
                                    <TableCell sx={{ width: 120, minWidth: 120, maxWidth: 120 }}>파일명</TableCell>
                                    <TableCell align="right" sx={{ width: 60, minWidth: 40, maxWidth: 80 }}>파일크기</TableCell>
                                    <TableCell>생성일시</TableCell>
                                    <TableCell sx={{ width: 120, minWidth: 120, maxWidth: 120 }}>진행단계</TableCell>
                                    <TableCell>진행률</TableCell>
                                    <TableCell align="center">작업상태</TableCell>
                                    <TableCell align="center">관리</TableCell>
                                </TableRow>
                                <TableRow>
                                    <TableCell>
                                        <OutlinedInput
                                            placeholder="Search Source"
                                            value={filters.name}
                                            onChange={(e) => setFilters({ ...filters, name: e.target.value })}
                                            size="small"
                                            fullWidth
                                        />
                                    </TableCell>
                                    <TableCell>
                                        <OutlinedInput
                                            placeholder="Search Name"
                                            value={filters.email}
                                            onChange={(e) => setFilters({ ...filters, email: e.target.value })}
                                            size="small"
                                            fullWidth
                                        />
                                    </TableCell>
                                    <TableCell>
                                        <OutlinedInput
                                            placeholder="Search Type"
                                            value={filters.role}
                                            onChange={(e) => setFilters({ ...filters, role: e.target.value })}
                                            size="small"
                                            fullWidth
                                        />
                                    </TableCell>
                                    <TableCell align="right" sx={{ width: 60, minWidth: 40, maxWidth: 80 }}>
                                        <OutlinedInput
                                            placeholder="Size"
                                            type="number"
                                            value={filters.ageMin}
                                            onChange={(e) => setFilters({ ...filters, ageMin: e.target.value })}
                                            size="small"
                                            fullWidth
                                        />
                                    </TableCell>
                                    <TableCell>
                                        {/* Create Date 필터(옵션) */}
                                    </TableCell>
                                    <TableCell>
                                        <OutlinedInput
                                            placeholder="Search Status"
                                            value={filters.status}
                                            onChange={(e) => setFilters({ ...filters, status: e.target.value })}
                                            size="small"
                                            fullWidth
                                        />
                                    </TableCell>
                                    <TableCell>
                                        {/* Progress 필터 삭제 */}
                                    </TableCell>
                                    <TableCell align="center"></TableCell>
                                    <TableCell align="center"></TableCell>
                                </TableRow>
                            </TableHead>
                            <TableBody>
                                {knowledgeList.map((row, index) => (
                                    <TableRow key={row.id || index}>
                                        <TableCell>{row.source || row.linkedService || '-'}</TableCell>
                                        <TableCell>{row.email || row.name || '-'}</TableCell>
                                        <TableCell>{row.type || row.docName || '-'}</TableCell>
                                        <TableCell align="right">{row.fileSize || '-'}</TableCell>
                                        <TableCell>{(row.createDate || row.createdDatetime || '-').slice(0, 10)}</TableCell>
                                        <TableCell sx={{ cursor: 'pointer', color: 'primary.main', textDecoration: 'underline' }}
                                            onClick={() => {
                                                if (row.knowledgeId) {
                                                    fetchStageDetail(row.knowledgeId);
                                                    setStageDialogOpen(true);
                                                }
                                            }}
                                        >
                                            {row.stage || '-'}
                                        </TableCell>
                                        <TableCell>
                                            <Stack direction="row" spacing={1} alignItems="center">
                                                <Box sx={{ flexGrow: 1 }}>
                                                    <LinearProgress variant="determinate" value={row.progress || 0} color="primary" />
                                                </Box>
                                                <Typography variant="body2">{row.progress ? `${row.progress}%` : '-'}</Typography>
                                            </Stack>
                                        </TableCell>
                                        <TableCell>{row.type || row.status || '-'}</TableCell>
                                        <TableCell align="center">
                                            <Stack direction="row" spacing={0.5} justifyContent="center">
                                                <IconButton color="primary" size="large">
                                                    <EditIcon />
                                                </IconButton>
                                                <IconButton color="inherit" size="large">
                                                    <DeleteIcon />
                                                </IconButton>
                                                <IconButton color="info" size="large" onClick={() => handleRowRefresh(row.knowledgeId)}>
                                                    <RefreshIcon />
                                                </IconButton>
                                            </Stack>
                                        </TableCell>
                                    </TableRow>
                                ))}
                            </TableBody>
                        </Table>
                    </TableContainer>
                </Stack>
            </Paper>

            {/* 업로드 다이얼로그 */}
            <Dialog open={uploadDialogOpen} onClose={handleDialogClose} maxWidth="xs" fullWidth>
                <DialogTitle sx={{ fontWeight: 700, fontSize: 22, textAlign: 'center', pt: 3 }}>지식 등록</DialogTitle>
                <DialogContent sx={{ pt: 1 }}>
                    <Typography
                        variant="body1"
                        sx={{
                            color: 'text.secondary',
                            fontSize: 15,
                            textAlign: 'center',
                            mb: 2
                        }}
                    >
                        새 지식의 이름을 정의하세요. 원하시면 지금 자료를 업로드하거나 나중에 추가할 수 있습니다.
                    </Typography>
                    <TextField
                        label="지식 이름"
                        required
                        fullWidth
                        size="small"
                        value={kbName}
                        onChange={e => setKbName(e.target.value)}
                        sx={{ mb: 2 }}
                    />
                    <FormControlLabel
                        control={<Checkbox checked={autoLang} onChange={e => setAutoLang(e.target.checked)} />}
                        label="언어를 자동으로 감지합니다"
                        sx={{ mb: 1 }}
                    />
                    <FormControl fullWidth required size="small" sx={{ mb: 2 }}>
                        <InputLabel>언어</InputLabel>
                        <Select
                            value={lang}
                            label="언어"
                            onChange={e => setLang(e.target.value)}
                        >
                            <MenuItem value="ko">Korean (Korea)</MenuItem>
                            <MenuItem value="en">English (US)</MenuItem>
                            <MenuItem value="ja">Japanese</MenuItem>
                            <MenuItem value="zh">Chinese</MenuItem>
                        </Select>
                    </FormControl>
                    <Typography
                        variant="subtitle2"
                        sx={{
                            fontWeight: 500,
                            mb: 1
                        }}
                    >
                        File upload
                    </Typography>
                    <Typography
                        variant="caption"
                        sx={{
                            color: 'text.secondary',
                            fontSize: 13,
                            mb: 1,
                            display: 'block'
                        }}
                    >
                        지원되는 파일 형식은 CSV, DOCX/DOC, TXT 또는 PDF입니다.
                    </Typography>
                    <Box
                        sx={{
                            border: '2px dashed',
                            borderColor: dragActive ? 'primary.main' : 'divider',
                            borderRadius: 1,
                            p: 3,
                            textAlign: 'center',
                            bgcolor: 'background.paper',
                            cursor: 'pointer',
                            mb: 1,
                            transition: 'all 0.2s',
                            '&:hover': {
                                borderColor: 'primary.main',
                                bgcolor: 'action.hover'
                            }
                        }}
                        onDragOver={handleDragOver}
                        onDragLeave={handleDragLeave}
                        onDrop={handleDrop}
                    >
                        <CloudUploadOutlinedIcon sx={{ fontSize: 48, color: 'text.disabled', mb: 1 }} />
                        <Typography
                            variant="body2"
                            sx={{
                                color: 'text.secondary',
                                fontSize: 15,
                                my: 1
                            }}
                        >
                            파일을 끌어서 놓거나
                        </Typography>
                        <label htmlFor="kb-file-upload">
                            <Button variant="outlined" component="span" size="small">파일 선택</Button>
                            <input
                                id="kb-file-upload"
                                type="file"
                                multiple
                                accept=".csv,.doc,.docx,.txt,.pdf"
                                style={{ display: 'none' }}
                                onChange={handleFileChange}
                            />
                        </label>
                        {files.length > 0 && (
                            <Box sx={{ mt: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 1 }}>
                                <Typography
                                    variant="caption"
                                    sx={{ fontSize: 13, color: 'primary.main' }}
                                >
                                    {files[0].name}
                                </Typography>
                                <Button size="small" color="error" onClick={handleRemoveFile} sx={{ minWidth: 0, p: 0, ml: 1 }}>
                                    ×
                                </Button>
                            </Box>
                        )}
                    </Box>

                </DialogContent>
                <DialogActions sx={{ justifyContent: 'space-between', px: 3, pb: 2 }}>
                    <Button onClick={handleDialogClose} color="inherit" variant="outlined">취소</Button>
                    <Button variant="contained" color="primary" onClick={handleRegister} disabled={files.length === 0}>등록</Button>
                </DialogActions>
            </Dialog>

            {/* 진행단계 상세 Dialog */}
            <Dialog open={stageDialogOpen} onClose={() => setStageDialogOpen(false)} maxWidth="md" fullWidth>
                <DialogTitle>진행단계 상세</DialogTitle>
                <DialogContent>
                    {stageLoading ? (
                        <Typography sx={{ p: 3, textAlign: 'center' }}>로딩중...</Typography>
                    ) : (
                        <Table size="small">
                            <TableHead>
                                <TableRow>
                                    <TableCell>NO</TableCell>
                                    <TableCell>Stage</TableCell>
                                    <TableCell>Status</TableCell>
                                    <TableCell>Total</TableCell>
                                    <TableCell>Success</TableCell>
                                    <TableCell>Fail</TableCell>
                                    <TableCell>Start</TableCell>
                                    <TableCell>End</TableCell>
                                    <TableCell>Duration</TableCell>
                                </TableRow>
                            </TableHead>
                            <TableBody>
                                {stageDetail.length === 0 ? (
                                    <TableRow><TableCell colSpan={9} align="center">데이터 없음</TableCell></TableRow>
                                ) : (
                                    stageDetail.map((item, idx) => {
                                        // Duration 계산 (초 단위 차이, hh:mm:ss)
                                        let duration = '-';
                                        let startStr = '-';
                                        let endStr = '-';
                                        if (item.startTime) {
                                            const d = new Date(item.startTime);
                                            startStr = d.toTimeString().slice(0, 8);
                                        }
                                        if (item.endTime) {
                                            const d = new Date(item.endTime);
                                            endStr = d.toTimeString().slice(0, 8);
                                        }
                                        if (item.startTime && item.endTime) {
                                            const start = new Date(item.startTime);
                                            const end = new Date(item.endTime);
                                            const diff = Math.floor((end - start) / 1000);
                                            if (!isNaN(diff) && diff >= 0) {
                                                const h = String(Math.floor(diff / 3600)).padStart(2, '0');
                                                const m = String(Math.floor((diff % 3600) / 60)).padStart(2, '0');
                                                const s = String(diff % 60).padStart(2, '0');
                                                duration = `${h}:${m}:${s}`;
                                            }
                                        }
                                        return (
                                            <TableRow key={item.knowledgeStageId || idx}>
                                                <TableCell>{item.stageIndex}</TableCell>
                                                <TableCell>{item.stage}</TableCell>
                                                <TableCell>{item.status}</TableCell>
                                                <TableCell>{item.totalCount}</TableCell>
                                                <TableCell>{item.successCount}</TableCell>
                                                <TableCell>{item.failCount}</TableCell>
                                                <TableCell>{startStr}</TableCell>
                                                <TableCell>{endStr}</TableCell>
                                                <TableCell>{duration}</TableCell>
                                            </TableRow>
                                        );
                                    })
                                )}
                            </TableBody>
                        </Table>
                    )}
                </DialogContent>
                <DialogActions>
                    <Button onClick={() => setStageDialogOpen(false)}>닫기</Button>
                </DialogActions>
            </Dialog>
        </div>
    );
}
