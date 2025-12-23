import React, { useState } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Button,
  Box,
  Typography,
  Divider,
  IconButton,
} from '@mui/material';
import {
  Close as CloseIcon,
} from '@mui/icons-material';
import { useCreatePrompt, usePrompts } from '../../hooks/usePrompts';
import { useVersions } from '../../hooks/useVersions';
import PromptEditTabs from '../common/PromptEditTabs';

const PromptFormDialog = ({ open, onClose, initialData = null, mode = 'create' }) => {
  const [formData, setFormData] = useState({
    code: initialData?.code || '',
    name: initialData?.name || '',
    description: initialData?.description || '',
    version: '',
    promptContent: initialData?.promptContent || '',
  });
  const [variables, setVariables] = useState({});
  const [isExistingCode, setIsExistingCode] = useState(false);
  const [codeCheckStatus, setCodeCheckStatus] = useState(mode === 'edit' ? 'available' : null); // null | 'checking' | 'available' | 'duplicate'
  const [isCodeLocked, setIsCodeLocked] = useState(mode === 'edit');
  const [activeTab, setActiveTab] = useState(0);
  const [extractedVariables, setExtractedVariables] = useState([]); // 변수 추출 결과를 state로 관리
  const [isExpanded, setIsExpanded] = useState(false);
  const prevOpenRef = React.useRef(false);

  // activeTab이 범위를 벗어나면 0으로 리셋
  React.useEffect(() => {
    if (activeTab > extractedVariables.length) {
      setActiveTab(0);
    }
  }, [extractedVariables.length]);

  // Dialog가 열릴 때만 초기화 (false -> true)
  React.useEffect(() => {
    if (open && !prevOpenRef.current) {
      setFormData({
        code: initialData?.code || '',
        name: initialData?.name || '',
        description: initialData?.description || '',
        version: '',
        promptContent: initialData?.promptContent || '',
      });
      setVariables({});
      setExtractedVariables([]);
      setIsExistingCode(false);
      setCodeCheckStatus(mode === 'edit' ? 'available' : null);
      setIsCodeLocked(mode === 'edit');
      setActiveTab(0);
      setIsExpanded(false);
    }
    prevOpenRef.current = open;
  }, [open, mode, initialData]);

  const createPrompt = useCreatePrompt();
  const { data: promptsData } = usePrompts({});
  const { data: versionsData } = useVersions(formData.code);

  // 등록된 코드 목록 추출
  const existingCodes = Array.isArray(promptsData?.data?.content)
    ? promptsData.data.content.map(p => p.code)
    : [];

  // 선택된 코드가 기존 코드인지 확인하고, 기존 코드면 해당 프롬프트 정보 가져오기
  const selectedPrompt = Array.isArray(promptsData?.data?.content)
    ? promptsData.data.content.find(p => p.code === formData.code)
    : undefined;

  // 다음 버전 번호 계산
  const getNextVersion = (versions) => {
    if (!versions || versions.length === 0) return '1';

    // 버전에서 숫자 추출 (v1.2.0 -> [1, 2, 0])
    const versionNumbers = versions.map(v => {
      const match = v.version.match(/v?(\d+)\.?(\d+)?\.?(\d+)?/);
      if (match) {
        return parseInt(match[1]) || 0;
      }
      return 0;
    });

    const maxVersion = Math.max(...versionNumbers);
    return String(maxVersion + 1);
  };

  // 코드 중복 확인
  const handleCheckCode = () => {
    if (!formData.code.trim()) {
      return;
    }

    setCodeCheckStatus('checking');

    // 테스트용: "CODE"라고 입력하면 중복으로 처리
    setTimeout(() => {
      const isDuplicate = formData.code === 'CODE' || existingCodes.includes(formData.code);

      if (isDuplicate) {
        setCodeCheckStatus('duplicate');
        setIsCodeLocked(false);
      } else {
        setCodeCheckStatus('available');
        setIsCodeLocked(true);
      }
    }, 300);
  };

  // 변수 체크 (프롬프트에서 {{}} 패턴 추출)
  const handleCheckVariables = () => {
    const regex = /\{\{\s*([a-zA-Z0-9_]+)\s*\}\}/g;
    const matches = [];
    let match;

    while ((match = regex.exec(formData.promptContent)) !== null) {
      if (!matches.includes(match[1])) {
        matches.push(match[1]);
      }
    }

    setExtractedVariables(matches);
  };

  // 변수 업데이트
  const handleUpdateVariable = (key, field, value) => {
    setVariables(prev => ({
      ...prev,
      [key]: {
        ...prev[key],
        [field]: value,
      }
    }));
  };

  const handlePromptContentChange = (value) => {
    setFormData(prev => ({ ...prev, promptContent: value }));
  };

  // 프롬프트 내용에서 현재 변수 추출
  const getCurrentVariables = () => {
    const regex = /\{\{\s*([a-zA-Z0-9_]+)\s*\}\}/g;
    const matches = [];
    let match;

    while ((match = regex.exec(formData.promptContent)) !== null) {
      if (!matches.includes(match[1])) {
        matches.push(match[1]);
      }
    }

    return matches.sort();
  };

  // 변수 체크 필요 여부 확인
  const needsVariableCheck = () => {
    if (!formData.promptContent) return false;

    const currentVars = getCurrentVariables();
    const extractedVars = [...extractedVariables].sort();

    // 변수가 없으면 체크 불필요
    if (currentVars.length === 0) return false;

    // 변수 개수가 다르거나, 내용이 다르면 체크 필요
    if (currentVars.length !== extractedVars.length) return true;

    return !currentVars.every((v, i) => v === extractedVars[i]);
  };

  const variableCheckNeeded = needsVariableCheck();

  const handleSubmit = async () => {
    try {
      // 버전 자동 설정
      const version = isExistingCode && versionsData
        ? getNextVersion(versionsData)
        : '1';

      // 변수 데이터 정리
      const variablesArray = extractedVariables.map(key => ({
        key,
        type: variables[key]?.type || 'string',
        required: variables[key]?.required ?? true,
        defaultValue: variables[key]?.defaultValue || '',
        description: variables[key]?.description || '',
        content: variables[key]?.content || '',
      }));

      // 백엔드 CreatePromptRequest에는 version 필드가 없으므로 제외하고 전송
      // (백엔드에서 초기 버전을 1로 자동 설정함)
      const { version: _, ...submitData } = formData;

      await createPrompt.mutateAsync({
        ...submitData,
        variables: variablesArray,
      });
      onClose();
      // 폼 초기화
      setFormData({
        code: '',
        name: '',
        description: '',
        version: '',
        promptContent: '',
      });
      setVariables({});
      setIsExistingCode(false);
      setIsExpanded(false);
    } catch (error) {
      console.error('Failed to create prompt:', error);
      const errorMessage = error.response?.data?.message || error.message || '알 수 없는 오류가 발생했습니다.';
      alert(`프롬프트 생성 실패: ${errorMessage}`);
    }
  };

  const handleClose = () => {
    onClose();
    // 폼 초기화
    setFormData({
      code: '',
      name: '',
      description: '',
      version: '',
      promptContent: '',
    });
    setVariables({});
    setIsExistingCode(false);
    setCodeCheckStatus(null);
    setIsCodeLocked(false);
    setActiveTab(0);
    setIsExpanded(false);
  };

  return (
    <Dialog
      open={open}
      onClose={(event, reason) => {
        if (reason === 'backdropClick' || reason === 'escapeKeyDown') {
          return; // 외부 클릭 및 ESC 키 무시
        }
        handleClose();
      }}
      maxWidth="md"
      fullWidth
      disableEscapeKeyDown
      PaperProps={{
        sx: {
          borderRadius: 2,
        }
      }}
    >
      <DialogTitle sx={{ pb: 1 }}>
        <Box display="flex" alignItems="center" justifyContent="space-between">
          <Typography variant="h6" sx={{ fontWeight: 600 }}>
            {mode === 'edit' ? '프롬프트 버전 생성' : '새 프롬프트 생성'}
          </Typography>
          <IconButton
            onClick={handleClose}
            size="small"
            sx={{
              color: 'text.secondary',
              '&:hover': { bgcolor: 'action.hover' }
            }}
          >
            <CloseIcon />
          </IconButton>
        </Box>
      </DialogTitle>

      <Divider />

      <DialogContent sx={{ pt: 3, pb: 2, overflow: 'auto' }}>
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2.5 }}>
          {/* 코드, 버전, 이름 - 한 줄 배치 (45:10:45 비율) */}
          <Box display="flex" gap={2} alignItems="flex-start">
            {/* 코드 - 45% */}
            <Box flex="1 1 45%">
              <Typography
                variant="body2"
                sx={{ mb: 0.75, fontWeight: 500, color: 'text.primary' }}
              >
                코드 <Typography component="span" color="error.main">*</Typography>
              </Typography>
              <Box display="flex" gap={1}>
                <TextField
                  value={formData.code}
                  onChange={(e) => {
                    if (mode === 'edit') return;
                    // 영어, 숫자, _ 만 허용하고 영어는 대문자로 변환
                    const value = e.target.value
                      .replace(/[^a-zA-Z0-9_]/g, '')
                      .toUpperCase();
                    setFormData(prev => ({ ...prev, code: value }));
                    setCodeCheckStatus(null);
                  }}
                  fullWidth
                  placeholder="프롬프트 코드를 입력하세요"
                  size="small"
                  disabled={mode === 'edit' || isCodeLocked || !!initialData}
                  error={codeCheckStatus === 'duplicate'}
                  sx={{
                    '& .MuiOutlinedInput-root': {
                      bgcolor: (mode === 'edit' || isCodeLocked) ? 'action.hover' : 'background.paper',
                    }
                  }}
                />
                {mode !== 'edit' && (
                  <Button
                    variant="outlined"
                    onClick={handleCheckCode}
                    disabled={!formData.code.trim() || isCodeLocked || !!initialData}
                    sx={{
                      minWidth: 100,
                      borderColor: codeCheckStatus === 'available' ? 'success.main' : undefined,
                      color: codeCheckStatus === 'available' ? 'success.main' : undefined,
                    }}
                  >
                    {codeCheckStatus === 'checking' ? '...' :
                      codeCheckStatus === 'available' ? '✓' : '중복확인'}
                  </Button>
                )}
              </Box>
              {codeCheckStatus === 'duplicate' && mode !== 'edit' && (
                <Typography variant="caption" color="error.main" sx={{ mt: 0.5, display: 'block' }}>
                  ⚠️ 코드명이 중복되었습니다.
                </Typography>
              )}
              {codeCheckStatus === 'available' && mode !== 'edit' && (
                <Typography variant="caption" color="success.main" sx={{ mt: 0.5, display: 'block' }}>
                  ✓ 사용 가능
                </Typography>
              )}
            </Box>

            {/* 버전 - 10% */}
            {codeCheckStatus === 'available' && (
              <Box flex="0 0 10%">
                <Typography
                  variant="body2"
                  sx={{ mb: 0.75, fontWeight: 500, color: 'text.primary' }}
                >
                  버전 <Typography component="span" color="error.main">*</Typography>
                </Typography>
                <TextField
                  value="1"
                  size="small"
                  disabled
                  fullWidth
                  sx={{
                    '& .MuiOutlinedInput-root': {
                      bgcolor: 'action.hover',
                    }
                  }}
                />
                <Typography variant="caption" color="text.secondary" sx={{ mt: 0.5, display: 'block' }}>
                  자동 생성
                </Typography>
              </Box>
            )}

            {/* 이름 - 45% */}
            <Box flex="1 1 45%">
              <Typography
                variant="body2"
                sx={{ mb: 0.75, fontWeight: 500, color: 'text.primary' }}
              >
                이름 <Typography component="span" color="error.main">*</Typography>
              </Typography>
              <TextField
                value={formData.name}
                onChange={(e) => {
                  const value = e.target.value;
                  setFormData(prev => ({ ...prev, name: value }));
                }}
                fullWidth
                placeholder="프롬프트 명을 입력하세요"
                size="small"
                disabled={codeCheckStatus !== 'available'}
                sx={{
                  '& .MuiOutlinedInput-root': {
                    bgcolor: 'background.paper',
                  }
                }}
              />
              <Typography variant="caption" color="text.secondary" sx={{ mt: 0.5, display: 'block' }}>
                청크 생성 프롬프트
              </Typography>
            </Box>
          </Box>

          {/* 설명 */}
          <Box>
            <Typography
              variant="body2"
              sx={{ mb: 0.75, fontWeight: 500, color: 'text.primary' }}
            >
              설명
            </Typography>
            <TextField
              value={formData.description}
              onChange={(e) => {
                const value = e.target.value;
                setFormData(prev => ({ ...prev, description: value }));
              }}
              fullWidth
              multiline
              rows={3}
              placeholder="프롬프트에 대한 설명을 입력하세요"
              size="small"
              disabled={codeCheckStatus !== 'available'}
              sx={{
                '& .MuiOutlinedInput-root': {
                  bgcolor: 'background.paper',
                }
              }}
            />
          </Box>

          {/* 탭 영역 */}
          <Box sx={{ mt: 2 }}>
            <PromptEditTabs
              activeTab={activeTab}
              onTabChange={setActiveTab}
              extractedVariables={extractedVariables}
              promptContent={formData.promptContent}
              onPromptContentChange={handlePromptContentChange}
              variables={variables}
              onVariableUpdate={handleUpdateVariable}
              onCheckVariables={handleCheckVariables}
              disabled={codeCheckStatus !== 'available'}
              showToolbar={true}
              isExpanded={isExpanded}
              onToggleExpand={() => setIsExpanded(!isExpanded)}
              customHeight={isExpanded ? '500px' : '250px'}
            />
          </Box>

        </Box>
      </DialogContent>

      <Divider />

      <DialogActions sx={{ px: 3, py: 2, flexDirection: 'column', alignItems: 'stretch', gap: 1 }}>
        {codeCheckStatus !== 'available' && (
          <Typography variant="caption" color="error.main" sx={{ textAlign: 'center' }}>
            ⚠️ 코드 중복 확인을 완료해주세요
          </Typography>
        )}
        {variableCheckNeeded && (
          <Typography variant="caption" color="error.main" sx={{ textAlign: 'center' }}>
            ⚠️ 변수 체크를 완료해주세요
          </Typography>
        )}
        <Typography variant="caption" color="text.secondary" sx={{ textAlign: 'left', mb: 1 }}>
          💡 변수는 {`{{변수명}}`} 형태로 입력하세요. 변수 체크 버튼을 눌러 변수를 추출하세요.
        </Typography>
        <Box sx={{ display: 'flex', justifyContent: 'flex-end', gap: 1 }}>
          <Button
            onClick={handleClose}
            sx={{
              color: 'text.secondary',
              '&:hover': { bgcolor: 'action.hover' }
            }}
          >
            취소
          </Button>
          <Button
            variant="contained"
            onClick={handleSubmit}
            disabled={
              codeCheckStatus !== 'available' ||
              !formData.code ||
              !formData.name ||
              variableCheckNeeded ||
              createPrompt.isPending
            }
            sx={{
              minWidth: 100,
              boxShadow: 'none',
              '&:hover': {
                boxShadow: '0px 2px 8px rgba(0,0,0,0.15)',
              },
              '&.Mui-disabled': {
                bgcolor: 'action.disabledBackground',
                color: 'text.disabled',
              }
            }}
          >
            {createPrompt.isPending ? (mode === 'edit' ? '버전 생성 중...' : '생성 중...') :
              codeCheckStatus !== 'available' ? '코드 확인 필요' :
                !formData.name ? '이름 입력 필요' :
                  variableCheckNeeded ? '변수 체크 필요' :
                    (mode === 'edit' ? '버전 생성' : '생성')}
          </Button>
        </Box>
      </DialogActions>
    </Dialog>
  );
};

export default PromptFormDialog;
