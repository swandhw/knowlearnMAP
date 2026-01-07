import React from 'react';
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
  Radio,
  IconButton,
} from '@mui/material';
import {
  Delete as DeleteIcon,
  ContentCopy as CopyIcon,
} from '@mui/icons-material';

const VersionHistoryPanel = ({
  versions = [],
  selectedVersion,
  onVersionChange,
  onCopyVersion,
  onDeleteVersion,
  compareVersions,
}) => {
  const { showAlert, showConfirm } = useAlert();
  const handleVersionClick = async (newVersionId) => {
    const currentVersion = versions.find(v => v.id === selectedVersion);
    const newVersion = versions.find(v => v.id === newVersionId);

    if (!currentVersion || !newVersion) {
      onVersionChange?.(newVersionId, newVersion?.content || '');
      return;
    }

    const comparison = compareVersions(currentVersion.version, newVersion.version);

    // 하위 버전으로 변경 시 확인
    if (comparison > 0) {
      const confirmed = await showConfirm(`버전을 ${currentVersion.version}에서 ${newVersion.version}으로 변경하시겠습니까?`);
      if (confirmed) {
        onVersionChange?.(newVersionId, newVersion.content);
      }
    } else {
      // 상위 버전으로 변경 시 바로 적용
      onVersionChange?.(newVersionId, newVersion.content);
    }
  };

  const handleDelete = async (e, version) => {
    e.stopPropagation();

    // 활성화된 버전인지 확인
    if (version.isActive) {
      // 버전이 1개만 있으면 전체 삭제
      if (versions.length === 1) {
        const confirmed = await showConfirm(`활성화된 버전을 삭제하면 프롬프트 전체가 삭제됩니다. 계속하시겠습니까?`);
        if (confirmed) {
          onDeleteVersion?.(version.id);
        }
      } else {
        // 버전이 2개 이상이면 활성화 버전 삭제 불가
        showAlert('활성화된 버전은 삭제할 수 없습니다. 다른 버전을 활성화한 후 삭제해주세요.');
      }
      return;
    }

    // 비활성화 버전 삭제
    // 버전을 정렬하여 최상위 5개 확인
    const sortedVersions = [...versions].sort((a, b) =>
      compareVersions(b.version, a.version)
    );
    const top5 = sortedVersions.slice(0, 5);
    const isTop5 = top5.some(v => v.id === version.id);

    if (isTop5) {
      const confirmed = await showConfirm(`버전 ${version.version}을(를) 삭제하시겠습니까?`);
      if (confirmed) {
        onDeleteVersion?.(version.id);
      }
    } else {
      onDeleteVersion?.(version.id);
    }
  };

  return (
    <Paper sx={{ pt: 2, px: 2, pb: 0, height: '560px', display: 'flex', flexDirection: 'column', bgcolor: '#f5f5f5' }} id="version-history-paper">
      <Typography variant="h6" gutterBottom sx={{ bgcolor: '#e3f2fd', p: 1, borderRadius: 1 }}>
        버전 히스토리 ({versions.length}개)
      </Typography>

      <TableContainer sx={{ maxHeight: '470px', overflow: 'auto', bgcolor: '#fff' }}>
        <Table size="small" stickyHeader>
          <TableHead>
            <TableRow sx={{ bgcolor: '#f5f5f5' }}>
              <TableCell sx={{ width: '60px', whiteSpace: 'nowrap', bgcolor: '#fff3e0' }}>활성</TableCell>
              <TableCell sx={{ bgcolor: '#fff3e0' }}>버전</TableCell>
              <TableCell sx={{ bgcolor: '#fff3e0' }}>수정일자</TableCell>
              <TableCell align="center" sx={{ bgcolor: '#fff3e0' }}>액션</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {versions.length === 0 ? (
              <TableRow>
                <TableCell colSpan={4} align="center">
                  <Typography variant="body2" color="text.secondary">
                    버전이 없습니다.
                  </Typography>
                </TableCell>
              </TableRow>
            ) : (
              versions.map((version) => (
                <TableRow
                  key={version.id}
                  hover
                  onClick={() => handleVersionClick(version.id)}
                  sx={{
                    bgcolor: selectedVersion === version.id ? 'action.selected' : 'inherit',
                    cursor: 'pointer'
                  }}
                >
                  <TableCell padding="checkbox">
                    <Radio
                      checked={version.isActive}
                      disabled
                      size="small"
                    />
                  </TableCell>
                  <TableCell>
                    <Typography sx={{ fontWeight: version.isActive ? 700 : 400 }}>
                      {version.version}
                    </Typography>
                  </TableCell>
                  <TableCell>
                    {new Date(version.createdAt).toLocaleDateString('ko-KR')}
                  </TableCell>
                  <TableCell align="center">
                    <Box display="flex" justifyContent="center" gap={0.5}>
                      <IconButton
                        size="small"
                        onClick={(e) => {
                          e.stopPropagation();
                          onCopyVersion?.(version);
                        }}
                      >
                        <CopyIcon fontSize="small" />
                      </IconButton>
                      <IconButton
                        size="small"
                        onClick={(e) => handleDelete(e, version)}
                      >
                        <DeleteIcon fontSize="small" />
                      </IconButton>
                    </Box>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </TableContainer>
    </Paper>
  );
};

export default VersionHistoryPanel;
