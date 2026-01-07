import React, { useState } from 'react';
import { useAlert } from '../../../context/AlertContext';
import {
  Box,
  Paper,
  Typography,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Chip,
  IconButton,
  Rating,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  CircularProgress,
  Tooltip,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  ToggleButtonGroup,
  ToggleButton,
} from '@mui/material';
import {
  Visibility as VisibilityIcon,
  Delete as DeleteIcon,
  ContentCopy as ContentCopyIcon,
  CheckCircle as SuccessIcon,
  Error as ErrorIcon,
} from '@mui/icons-material';
import { useSnapshots, useDeleteSnapshot } from '../../hooks/useSnapshots';
import { formatDistanceToNow } from 'date-fns';
import { ko } from 'date-fns/locale';

const HistoryTab = ({ promptCode, versions }) => {
  const [versionFilter, setVersionFilter] = useState('');
  const [modelFilter, setModelFilter] = useState('');
  const [selectedSnapshot, setSelectedSnapshot] = useState(null);
  const [detailDialogOpen, setDetailDialogOpen] = useState(false);
  const [responseViewMode, setResponseViewMode] = useState('text');
  const { showAlert } = useAlert();

  const { data: snapshotsData, isLoading, error } = useSnapshots(promptCode, {
    versionId: versionFilter || undefined,
    model: modelFilter || undefined,
  });

  const deleteSnapshot = useDeleteSnapshot();

  const snapshots = snapshotsData?.content || [];

  const handleDelete = async (snapshotId) => {
    const confirmed = await showConfirm('이 테스트 스냅샷을 삭제하시겠습니까?');
    if (confirmed) {
      try {
        await deleteSnapshot.mutateAsync(snapshotId);
      } catch (error) {
        console.error('Failed to delete snapshot:', error);
        showAlert('삭제에 실패했습니다.');
      }
    }
  };

  const handleViewDetail = (snapshot) => {
    setSelectedSnapshot(snapshot);
    setDetailDialogOpen(true);
  };

  const getModelColor = (model) => {
    switch (model) {
      case 'AISTUDIO':
        return 'primary';
      case 'OPENAI':
        return 'success';
      case 'ANTHROPIC':
        return 'secondary';
      default:
        return 'default';
    }
  };

  const formatTime = (datetime) => {
    if (!datetime) return '-';
    try {
      return formatDistanceToNow(new Date(datetime), { addSuffix: true, locale: ko });
    } catch {
      return datetime;
    }
  };

  const getResponseText = (response) => {
    if (!response) return '-';
    try {
      const responseData = typeof response === 'string' ? JSON.parse(response) : response;
      return responseData.text || '응답 없음';
    } catch {
      return '응답 파싱 실패';
    }
  };

  const getTokensUsed = (response) => {
    if (!response) return 0;
    try {
      const responseData = typeof response === 'string' ? JSON.parse(response) : response;
      return responseData.tokens_used || 0;
    } catch {
      return 0;
    }
  };

  const getLatency = (response) => {
    if (!response) return 0;
    try {
      const responseData = typeof response === 'string' ? JSON.parse(response) : response;
      return responseData.latency_ms || 0;
    } catch {
      return 0;
    }
  };

  if (isLoading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box sx={{ p: 3 }}>
      {/* 필터 영역 */}
      <Paper sx={{ p: 2, mb: 2 }}>
        <Box display="flex" gap={2} alignItems="center">
          <FormControl size="small" sx={{ minWidth: 150 }}>
            <InputLabel>버전</InputLabel>
            <Select
              value={versionFilter}
              onChange={(e) => setVersionFilter(e.target.value)}
              label="버전"
            >
              <MenuItem value="">전체</MenuItem>
              {versions.map((v) => (
                <MenuItem key={v.id} value={v.id}>
                  v{v.version}
                </MenuItem>
              ))}
            </Select>
          </FormControl>

          <FormControl size="small" sx={{ minWidth: 150 }}>
            <InputLabel>모델</InputLabel>
            <Select
              value={modelFilter}
              onChange={(e) => setModelFilter(e.target.value)}
              label="모델"
            >
              <MenuItem value="">전체</MenuItem>
              <MenuItem value="AISTUDIO">AISTUDIO</MenuItem>
              <MenuItem value="OPENAI">OPENAI</MenuItem>
              <MenuItem value="ANTHROPIC">ANTHROPIC</MenuItem>
            </Select>
          </FormControl>

          <Typography variant="body2" color="text.secondary" sx={{ ml: 'auto' }}>
            총 {snapshots.length}개의 테스트 이력
          </Typography>
        </Box>
      </Paper>

      {/* 테이블 */}
      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow sx={{ bgcolor: 'action.hover' }}>
              <TableCell align="center">ID</TableCell>
              <TableCell>테스트 이름</TableCell>
              <TableCell align="center">버전</TableCell>
              <TableCell align="center">모델</TableCell>
              <TableCell align="center">만족도</TableCell>
              <TableCell align="center">토큰</TableCell>
              <TableCell align="center">응답시간</TableCell>
              <TableCell align="center">상태</TableCell>
              <TableCell align="center">실행시간</TableCell>
              <TableCell align="center">액션</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {snapshots.length === 0 ? (
              <TableRow>
                <TableCell colSpan={10} align="center" sx={{ py: 4 }}>
                  <Typography color="text.secondary">테스트 이력이 없습니다.</Typography>
                </TableCell>
              </TableRow>
            ) : (
              snapshots.map((snapshot) => {
                const llmConfig = typeof snapshot.llmConfig === 'string'
                  ? JSON.parse(snapshot.llmConfig)
                  : snapshot.llmConfig;
                const model = llmConfig?.model || 'UNKNOWN';
                const version = versions.find((v) => v.id === snapshot.versionId);

                return (
                  <TableRow key={snapshot.id} hover>
                    <TableCell align="center">
                      <Typography variant="body2" sx={{ fontWeight: 500 }}>
                        {snapshot.id}
                      </Typography>
                    </TableCell>
                    <TableCell>
                      <Typography variant="body2" sx={{ fontWeight: 500 }}>
                        {snapshot.testName || '테스트'}
                      </Typography>
                    </TableCell>
                    <TableCell align="center">
                      <Chip label={`v${version?.version || '?'}`} size="small" />
                    </TableCell>
                    <TableCell align="center">
                      <Chip label={model} size="small" color={getModelColor(model)} />
                    </TableCell>
                    <TableCell align="center">
                      <Rating value={snapshot.satisfaction || 0} readOnly size="small" />
                    </TableCell>
                    <TableCell align="center">
                      <Typography variant="body2">
                        {snapshot.tokensUsed || getTokensUsed(snapshot.response) || 0}
                      </Typography>
                    </TableCell>
                    <TableCell align="center">
                      <Typography variant="body2">
                        {snapshot.latencyMs
                          ? (snapshot.latencyMs / 1000).toFixed(1)
                          : (getLatency(snapshot.response) / 1000).toFixed(1)}s
                      </Typography>
                    </TableCell>
                    <TableCell align="center">
                      <Chip
                        icon={snapshot.success !== false ? <SuccessIcon /> : <ErrorIcon />}
                        label={snapshot.success !== false ? '성공' : '실패'}
                        size="small"
                        color={snapshot.success !== false ? 'success' : 'error'}
                      />
                    </TableCell>
                    <TableCell align="center">
                      <Tooltip title={formatTime(snapshot.createdAt)}>
                        <Typography variant="body2" color="text.secondary">
                          {snapshot.createdAt
                            ? new Date(snapshot.createdAt).toLocaleString('ko-KR', {
                              year: 'numeric',
                              month: '2-digit',
                              day: '2-digit',
                              hour: '2-digit',
                              minute: '2-digit',
                              second: '2-digit',
                              hour12: false
                            })
                            : '-'}
                        </Typography>
                      </Tooltip>
                    </TableCell>
                    <TableCell align="center">
                      <Box display="flex" gap={0.5} justifyContent="center">
                        <Tooltip title="상세보기">
                          <IconButton
                            size="small"
                            onClick={() => handleViewDetail(snapshot)}
                          >
                            <VisibilityIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                        <Tooltip title="삭제">
                          <IconButton
                            size="small"
                            color="error"
                            onClick={() => handleDelete(snapshot.id)}
                          >
                            <DeleteIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                      </Box>
                    </TableCell>
                  </TableRow>
                );
              })
            )}
          </TableBody>
        </Table>
      </TableContainer>

      {/* 상세보기 다이얼로그 */}
      <Dialog
        open={detailDialogOpen}
        onClose={() => setDetailDialogOpen(false)}
        maxWidth="md"
        fullWidth
      >
        <DialogTitle>테스트 스냅샷 상세</DialogTitle>
        <DialogContent dividers>
          {selectedSnapshot && (
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
              <Box>
                <Typography variant="subtitle2" color="text.secondary">
                  테스트 이름
                </Typography>
                <Typography variant="body1">{selectedSnapshot.testName}</Typography>
              </Box>

              <Box>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
                  <Typography variant="subtitle2" color="text.secondary">
                    프롬프트 내용
                  </Typography>
                  <Tooltip title="복사">
                    <IconButton
                      size="small"
                      onClick={() => {
                        navigator.clipboard.writeText(selectedSnapshot.content);
                        showAlert('프롬프트 내용이 복사되었습니다.');
                      }}
                    >
                      <ContentCopyIcon fontSize="small" />
                    </IconButton>
                  </Tooltip>
                </Box>
                <Paper sx={{ p: 2, bgcolor: 'grey.50', maxHeight: 200, overflow: 'auto' }}>
                  <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>
                    {selectedSnapshot.content}
                  </Typography>
                </Paper>
              </Box>

              <Box>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
                  <Typography variant="subtitle2" color="text.secondary">
                    응답 결과
                  </Typography>
                  <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
                    <ToggleButtonGroup
                      value={responseViewMode}
                      exclusive
                      onChange={(e, newMode) => {
                        if (newMode !== null) {
                          setResponseViewMode(newMode);
                        }
                      }}
                      size="small"
                    >
                      <ToggleButton value="text">TEXT</ToggleButton>
                      <ToggleButton value="json">JSON</ToggleButton>
                    </ToggleButtonGroup>
                    <Tooltip title="복사">
                      <IconButton
                        size="small"
                        onClick={() => {
                          const textToCopy = responseViewMode === 'json'
                            ? JSON.stringify(selectedSnapshot.response, null, 2)
                            : getResponseText(selectedSnapshot.response);
                          navigator.clipboard.writeText(textToCopy);
                          showAlert('응답 결과가 복사되었습니다.');
                        }}
                      >
                        <ContentCopyIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                  </Box>
                </Box>
                <Paper sx={{ p: 2, bgcolor: 'grey.50', maxHeight: 300, overflow: 'auto' }}>
                  <Typography
                    variant="body2"
                    component="pre"
                    sx={{
                      whiteSpace: 'pre-wrap',
                      fontFamily: responseViewMode === 'json' ? 'monospace' : 'inherit',
                      m: 0,
                    }}
                  >
                    {responseViewMode === 'json'
                      ? JSON.stringify(selectedSnapshot.response, null, 2)
                      : getResponseText(selectedSnapshot.response)}
                  </Typography>
                </Paper>
              </Box>

              {selectedSnapshot.notes && (
                <Box>
                  <Typography variant="subtitle2" color="text.secondary">
                    노트
                  </Typography>
                  <Typography variant="body2">{selectedSnapshot.notes}</Typography>
                </Box>
              )}
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDetailDialogOpen(false)}>닫기</Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
};

export default HistoryTab;
