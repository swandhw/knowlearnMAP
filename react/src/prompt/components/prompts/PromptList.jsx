import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Box,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Chip,
  Button,
  Add as AddIcon,
  Home as HomeIcon,
  Delete as DeleteIcon,
} from '@mui/icons-material';
import {
  Box,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Chip,
  Button,
  TextField,
  Select,
  MenuItem,
  Typography,
  CircularProgress,
  IconButton
} from '@mui/material';
import { usePrompts } from '../../hooks/usePrompts';
import PromptFormDialog from './PromptFormDialog';

const PromptListContent = () => {
  const navigate = useNavigate();
  const [filters, setFilters] = useState({
    search: '',
    tags: [],
    isActive: true
  });
  const [openDialog, setOpenDialog] = useState(false);

  const { data, isLoading, error } = usePrompts(filters);

  const handleRowClick = (code) => {
    navigate(`/prompts/${code}`);
  };

  // 백엔드가 없는 경우를 대비하여 에러 상태에서도 빈 배열로 처리
  const prompts = Array.isArray(data?.data?.content) ? data.data.content : [];

  if (isLoading) {
    return (
      <Box display="flex" justifyContent="center" mt={4}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box>
      <Box sx={{ mb: 3, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          <Button
            variant="outlined"
            startIcon={<HomeIcon />}
            onClick={() => navigate('/workspaces')}
          >
            홈으로
          </Button>
          <Box>
            <Typography variant="h4" sx={{ fontWeight: 700, mb: 0.5 }}>
              프롬프트 관리
            </Typography>
            <Typography variant="body2" color="text.secondary">
              {prompts.length}개의 프롬프트
            </Typography>
          </Box>
        </Box>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => setOpenDialog(true)}
          sx={{ height: 40 }}
        >
          새 프롬프트 생성
        </Button>
      </Box>

      <Paper sx={{ mb: 2, p: 2.5, borderRadius: 2 }}>
        <Box display="flex" gap={2}>
          <TextField
            label="검색"
            variant="outlined"
            size="small"
            value={filters.search}
            onChange={(e) => setFilters({ ...filters, search: e.target.value })}
            placeholder="코드 또는 이름으로 검색"
            sx={{ flexGrow: 1 }}
          />
          <Select
            value={filters.isActive}
            onChange={(e) => setFilters({ ...filters, isActive: e.target.value })}
            size="small"
            sx={{ minWidth: 150 }}
          >
            <MenuItem value={true}>활성</MenuItem>
            <MenuItem value={false}>비활성</MenuItem>
            <MenuItem value={undefined}>전체</MenuItem>
          </Select>
        </Box>
      </Paper>

      <TableContainer component={Paper} sx={{ borderRadius: 2 }}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>코드</TableCell>
              <TableCell>이름</TableCell>
              <TableCell>설명</TableCell>
              <TableCell align="center">활성화 버전</TableCell>
              <TableCell align="center">버전별 갯수</TableCell>
              <TableCell align="center">만족도</TableCell>
              <TableCell>수정 일시</TableCell>
              <TableCell align="center">삭제</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {prompts.length === 0 ? (
              <TableRow>
                <TableCell colSpan={8} align="center" sx={{ py: 4 }}>
                  <Typography color="text.secondary">
                    데이터가 없습니다.
                  </Typography>
                </TableCell>
              </TableRow>
            ) : (
              prompts.map((prompt) => (
                <TableRow
                  key={prompt.id}
                  hover
                  sx={{ cursor: 'pointer' }}
                >
                  <TableCell onClick={() => handleRowClick(prompt.code)}>
                    {prompt.code}
                  </TableCell>
                  <TableCell onClick={() => handleRowClick(prompt.code)}>
                    {prompt.name}
                  </TableCell>
                  <TableCell onClick={() => handleRowClick(prompt.code)}>
                    {prompt.description?.length > 20
                      ? `${prompt.description.substring(0, 20)}...`
                      : prompt.description || '-'}
                  </TableCell>
                  <TableCell align="center" onClick={() => handleRowClick(prompt.code)}>
                    {prompt.activeVersion || 'N/A'}
                  </TableCell>
                  <TableCell align="center" onClick={() => handleRowClick(prompt.code)}>
                    {prompt.versionCount || 0}
                  </TableCell>
                  <TableCell align="center" onClick={() => handleRowClick(prompt.code)}>
                    {prompt.satisfaction || '-'}
                  </TableCell>
                  <TableCell onClick={() => handleRowClick(prompt.code)}>
                    {new Date(prompt.updatedAt).toLocaleString('ko-KR', {
                      year: 'numeric',
                      month: '2-digit',
                      day: '2-digit',
                      hour: '2-digit',
                      minute: '2-digit',
                      second: '2-digit',
                      hour12: false
                    })}
                  </TableCell>
                  <TableCell align="center">
                    <IconButton
                      onClick={(e) => {
                        e.stopPropagation();
                        if (window.confirm('정말로 이 프롬프트와 관련된 모든 버전 및 스냅샷을 삭제하시겠습니까?')) {
                          // usePrompts 훅이 리패치 기능을 제공하지 않는다면 window.location.reload()를 임시로 사용하거나,
                          // 훅 내부에서 refetch 메서드를 노출하도록 수정해야 합니다. 
                          // 여기서는 간단히 fetch 로직 구현.
                          fetch(`/api/v1/prompts/${prompt.code}`, { method: 'DELETE' })
                            .then(res => {
                              if (res.ok) {
                                window.location.reload();
                              } else {
                                alert('삭제 실패');
                              }
                            });
                        }
                      }}
                      color="error"
                      size="small"
                    >
                      <DeleteIcon />
                    </IconButton>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </TableContainer>

      <PromptFormDialog
        open={openDialog}
        onClose={() => setOpenDialog(false)}
      />
    </Box>
  );
};

export default PromptListContent;
