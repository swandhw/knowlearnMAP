import React, { useState } from 'react';
import {
  Box,
  Grid,
  Paper,
  Typography,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  TextField,
  Button,
  Divider,
  CircularProgress,
  Tabs,
  Tab,
  IconButton,
  Tooltip,
  Rating
} from '@mui/material';
import {
  PlayArrow as PlayIcon,
  ContentCopy as CopyIcon,
  Save as SaveIcon
} from '@mui/icons-material';
import { resolveVariables } from '../../utils/variableParser';
import { testService } from '../../api/testService';
import axiosClient from '../../api/axiosClient';

const TestTab = ({
  promptCode,
  versions = [],
  variableSets = [],
  variableSchemas = [],
  llmConfig
}) => {
  const [selectedVersion, setSelectedVersion] = useState(versions?.[0]?.id);
  const [llmModel, setLlmModel] = useState(llmConfig?.llmModel || 'AISTUDIO');
  const [temperature, setTemperature] = useState(llmConfig?.temperature ?? 0.7);
  const [topP, setTopP] = useState(llmConfig?.topP ?? 0.95);
  const [maxOutputTokens, setMaxOutputTokens] = useState(llmConfig?.maxOutputTokens ?? 2000);
  const [topK, setTopK] = useState(llmConfig?.topK ?? 40);
  const [n, setN] = useState(llmConfig?.n ?? 1);
  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState(null);
  const [resultTab, setResultTab] = useState(0);
  const [satisfaction, setSatisfaction] = useState(0);

  const currentVersion = versions?.find(v => v.id === selectedVersion);

  // 버전 변경 시 저장된 환경 설정 불러오기
  React.useEffect(() => {
    const applyDefaultConfig = () => {
      const version = versions?.find(v => v.id === selectedVersion);
      if (version?.llmConfig) {
        setLlmModel(version.llmConfig.model || 'AISTUDIO');
        setTemperature(version.llmConfig.temperature ?? 0.7);
        setTopP(version.llmConfig.topP ?? 0.95);
        setMaxOutputTokens(version.llmConfig.maxOutputTokens ?? 2000);
        setTopK(version.llmConfig.topK ?? 40);
        setN(version.llmConfig.n ?? 1);
      } else if (llmConfig) {
        // Version config가 없으면 Prompt 레벨의 기본 설정 사용
        setLlmModel(llmConfig.llmModel || llmConfig.model || 'AISTUDIO');
        setTemperature(llmConfig.temperature ?? 0.7);
        setTopP(llmConfig.topP ?? 0.95);
        setMaxOutputTokens(llmConfig.maxOutputTokens ?? 2000);
        setTopK(llmConfig.topK ?? 40);
        setN(llmConfig.n ?? 1);
      } else {
        setLlmModel('AISTUDIO');
        setTemperature(0.7);
        setTopP(0.95);
        setMaxOutputTokens(2000);
        setTopK(40);
        setN(1);
      }
    };

    const fetchLlmConfig = async () => {
      if (!selectedVersion) return;
      try {
        const response = await testService.getLlmConfig(promptCode, selectedVersion);
        const data = response.data;
        if (data && data.llmConfig) {
          setLlmModel(data.llmConfig.model || 'AISTUDIO');
          setTemperature(data.llmConfig.temperature ?? 0.7);
          setTopP(data.llmConfig.topP ?? 0.95);
          setMaxOutputTokens(data.llmConfig.maxOutputTokens ?? 2000);
          setTopK(data.llmConfig.topK ?? 40);
          setN(data.llmConfig.n ?? 1);
        } else {
          // 저장된 설정이 없으면 버전의 기본 설정 또는 Default 사용
          applyDefaultConfig();
        }
      } catch (error) {
        // 404 등 에러 시 기본값 적용
        console.log('No saved llm config found, using defaults.');
        applyDefaultConfig();
      }
    };
    fetchLlmConfig();
  }, [selectedVersion, promptCode, versions]);

  // 변수 치환 - Editor에서 입력한 content 값 사용
  const resolvedPrompt = React.useMemo(() => {
    if (!currentVersion?.content) return '';

    let resolved = currentVersion.content;
    const schema = currentVersion.variableSchema || [];

    schema.forEach(variable => {
      // Editor 화면에서 입력한 content 값 사용
      const value = variable.content || variable.defaultValue || '';
      const regex = new RegExp(`\\{\\{\\s*${variable.key}\\s*\\}\\}`, 'g');
      resolved = resolved.replace(regex, value);
    });

    return resolved;
  }, [currentVersion]);

  // 치환되지 않은 변수 감지
  const hasUnresolvedVariables = React.useMemo(() => {
    if (!resolvedPrompt) return false;
    return /\{\{[^}]+\}\}/g.test(resolvedPrompt);
  }, [resolvedPrompt]);

  // 치환되지 않은 변수명 추출
  const unresolvedVariableNames = React.useMemo(() => {
    if (!resolvedPrompt) return [];
    const matches = resolvedPrompt.match(/\{\{([^}]+)\}\}/g);
    if (!matches) return [];
    return [...new Set(matches.map(m => m.replace(/\{\{|\}\}/g, '').trim()))];
  }, [resolvedPrompt]);

  const handleCopyToClipboard = (text) => {
    navigator.clipboard.writeText(text);
  };

  const handleSaveLlmConfig = async () => {
    try {
      const variables = {};
      currentVersion?.variableSchema?.forEach(v => {
        variables[v.key] = v.content || v.defaultValue || '';
      });

      await testService.saveLlmConfig(promptCode, selectedVersion, {
        testName: `Llm Config`, // Singleton이라 이름은 크게 중요하지 않음
        variables,
        llmConfig: {
          model: llmModel,
          temperature,
          topP,
          maxOutputTokens,
          topK,
          n
        }
      });
      alert('환경 설정이 저장되었습니다.');
    } catch (error) {
      console.error('Save llm config failed:', error);
      alert('환경 설정 저장에 실패했습니다.');
    }
  };

  const handleSaveSatisfaction = async () => {
    try {
      // snapshotId는 result에서 가져와야 함
      const snapshotId = result?.snapshotId;
      if (!snapshotId) {
        alert('저장할 테스트 결과가 없습니다.');
        return;
      }

      await testService.saveSatisfaction(snapshotId, satisfaction, '');
      alert('만족도가 저장되었습니다.');
    } catch (error) {
      console.error('Save satisfaction failed:', error);
      alert('만족도 저장에 실패했습니다.');
    }
  };

  const handleTest = async () => {
    setLoading(true);
    setSatisfaction(0);
    try {
      const variables = {};
      currentVersion?.variableSchema?.forEach(v => {
        variables[v.key] = v.content || v.defaultValue || '';
      });

      // 테스트 실행 (스냅샷 생성)
      // executeTest는 api/testService.js에는 없지만, PromptController에는 POST /{code}/test가 있음.
      // services/testService.js에는 executeTest가 없으므로 추가 필요하거나, 직접 호출해야 함.
      // 하지만 기존 코드에서는 executeTestSet을 호출했음.
      // 여기서는 axiosClient를 직접 쓰거나 testService에 executeTest를 추가해야 함.
      // 일단 testService.executeTestSet 대신 axiosClient로 직접 호출하거나, 
      // testService.js에 executeTest를 추가하는 것이 좋음.
      // Step 963에서 services/testService.js에 executeTest를 추가하지 않았음.
      // 하지만 api/testService.js에도 executeTest가 없음 (executeTestSet만 있었고 제거함).
      // PromptController의 POST /{code}/test는 존재함.

      // 잠시, api/testService.js에 executeTest를 추가해야 함.
      // 지금은 일단 axiosClient를 직접 import해서 사용하거나, 
      // 이 파일 상단에 axiosClient가 import 되어 있으므로 그것을 사용.

      const executeResponse = await axiosClient.post(`/prompts/${promptCode}/test`, {
        versionId: selectedVersion,
        variables,
        llmConfig: {
          model: llmModel,
          temperature,
          topP,
          maxOutputTokens,
          topK,
          n
        }
      });

      setResult(executeResponse?.data?.data || executeResponse?.data);
    } catch (error) {
      console.error('Test failed:', error);
      setResult({ error: error.message || 'API 호출에 실패했습니다.' });
    } finally {
      setLoading(false);
    }
  };



  return (
    <Box sx={{ p: 1, width: '100%' }}>
      {/* 설정 영역 */}
      <Paper sx={{ p: 2, mb: 2 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
          <Typography variant="h6">
            환경 설정
          </Typography>
          <Button
            variant="outlined"
            startIcon={<SaveIcon />}
            onClick={handleSaveLlmConfig}
            size="small"
          >
            환경 저장
          </Button>
        </Box>
        <Grid container spacing={2}>
          <Grid item xs={12} md={2}>
            <FormControl fullWidth sx={{ minWidth: '150px' }}>
              <InputLabel>버전 선택</InputLabel>
              <Select
                value={selectedVersion}
                onChange={(e) => setSelectedVersion(e.target.value)}
                label="버전 선택"
              >
                {versions.map((version) => (
                  <MenuItem key={version.id} value={version.id}>
                    {version.version} {version.isActive && '(활성)'}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Grid>

          <Grid item xs={12} md={2}>
            <FormControl fullWidth>
              <InputLabel>LLM 모델</InputLabel>
              <Select
                value={llmModel}
                onChange={(e) => setLlmModel(e.target.value)}
                label="LLM 모델"
              >
                <MenuItem value="AISTUDIO">AISTUDIO (Gemini Flash)</MenuItem>
                <MenuItem value="GEMINI_2_5_PRO">Gemini 2.5 Pro (SDK)</MenuItem>
                <MenuItem value="OPENAI">OPENAI</MenuItem>
                <MenuItem value="ANTHROPIC">ANTHROPIC</MenuItem>
              </Select>
            </FormControl>
          </Grid>

          <Grid item xs={12} md={2}>
            <TextField
              fullWidth
              label="Temperature"
              type="number"
              value={temperature}
              onChange={(e) => setTemperature(parseFloat(e.target.value))}
              inputProps={{ min: 0, max: 2, step: 0.1 }}
            />
          </Grid>

          <Grid item xs={12} md={2}>
            <TextField
              fullWidth
              label="Top P"
              type="number"
              value={topP}
              onChange={(e) => setTopP(parseFloat(e.target.value))}
              inputProps={{ min: 0, max: 1, step: 0.05 }}
            />
          </Grid>

          <Grid item xs={12} md={2}>
            <TextField
              fullWidth
              label="Max Tokens"
              type="number"
              value={maxOutputTokens}
              onChange={(e) => setMaxOutputTokens(parseInt(e.target.value))}
              inputProps={{ min: 1, max: 10000, step: 100 }}
            />
          </Grid>

          <Grid item xs={12} md={2}>
            <TextField
              fullWidth
              label="Top K"
              type="number"
              value={topK}
              onChange={(e) => setTopK(parseInt(e.target.value))}
              inputProps={{ min: 1, max: 100, step: 1 }}
            />
          </Grid>

          <Grid item xs={12} md={2}>
            <TextField
              fullWidth
              label="N"
              type="number"
              value={n}
              onChange={(e) => setN(parseInt(e.target.value))}
              inputProps={{ min: 1, max: 10, step: 1 }}
            />
          </Grid>
        </Grid>
      </Paper>

      {/* 프롬프트 비교 영역 */}
      <Box sx={{ display: 'flex', gap: 2, mb: 2, width: '100%' }}>
        <Box sx={{ flex: '1 1 50%', minWidth: 0 }}>
          <Paper sx={{ p: 2, height: '400px', display: 'flex', flexDirection: 'column' }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
              <Typography variant="h6">
                원본 프롬프트
              </Typography>
              <Tooltip title="복사">
                <IconButton
                  size="small"
                  onClick={() => handleCopyToClipboard(currentVersion?.content || '')}
                >
                  <CopyIcon fontSize="small" />
                </IconButton>
              </Tooltip>
            </Box>
            <Box
              sx={{
                whiteSpace: 'pre-wrap',
                fontFamily: 'monospace',
                fontSize: '14px',
                bgcolor: '#f5f5f5',
                p: 2,
                borderRadius: 1,
                flex: 1,
                overflow: 'auto',
                minHeight: 0
              }}
            >
              {currentVersion?.content || '버전을 선택하세요.'}
            </Box>
          </Paper>
        </Box>

        <Box sx={{ flex: '1 1 50%', minWidth: 0 }}>
          <Paper sx={{ p: 2, height: '400px', display: 'flex', flexDirection: 'column' }}>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <Typography variant="h6" sx={{ color: hasUnresolvedVariables ? 'error.main' : 'inherit' }}>
                  변수 치환 후 (Resolved)
                </Typography>
                {hasUnresolvedVariables && (
                  <Typography variant="caption" sx={{ color: 'error.main' }}>
                    (미치환: {unresolvedVariableNames.join(', ')})
                  </Typography>
                )}
              </Box>
              <Tooltip title="복사">
                <IconButton
                  size="small"
                  onClick={() => handleCopyToClipboard(resolvedPrompt)}
                >
                  <CopyIcon fontSize="small" />
                </IconButton>
              </Tooltip>
            </Box>
            <Box
              sx={{
                whiteSpace: 'pre-wrap',
                fontFamily: 'monospace',
                fontSize: '14px',
                bgcolor: '#e3f2fd',
                p: 2,
                borderRadius: 1,
                flex: 1,
                overflow: 'auto',
                minHeight: 0
              }}
            >
              {resolvedPrompt || '변수를 설정하세요.'}
            </Box>
          </Paper>
        </Box>
      </Box>

      {/* 테스트 실행 */}
      <Box display="flex" justifyContent="center" sx={{ mb: 2 }}>
        <Button
          variant="contained"
          size="large"
          startIcon={loading ? <CircularProgress size={20} color="inherit" /> : <PlayIcon />}
          onClick={handleTest}
          disabled={loading || !currentVersion}
        >
          {loading ? '실행 중...' : 'API 호출 테스트'}
        </Button>
      </Box>

      {/* 결과 영역 */}
      {result && (
        <Paper sx={{ p: 2, width: '100%', overflow: 'hidden' }}>
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
            <Typography variant="h6">
              테스트 결과
            </Typography>
            <Box sx={{ display: 'flex', gap: 2, alignItems: 'center' }}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                <Typography variant="body2">만족도:</Typography>
                <Rating
                  value={satisfaction}
                  onChange={(e, newValue) => setSatisfaction(newValue)}
                  size="small"
                />
                <Button
                  variant="outlined"
                  size="small"
                  onClick={handleSaveSatisfaction}
                  disabled={satisfaction === 0}
                >
                  저장
                </Button>
              </Box>
              <Tooltip title="복사">
                <IconButton
                  size="small"
                  onClick={() => handleCopyToClipboard(
                    resultTab === 0 ? JSON.stringify(result, null, 2) : (result.response?.text || result.error || '')
                  )}
                >
                  <CopyIcon fontSize="small" />
                </IconButton>
              </Tooltip>
            </Box>
          </Box>
          <Tabs value={resultTab} onChange={(e, v) => setResultTab(v)}>
            <Tab label="JSON" />
            <Tab label="텍스트" />
          </Tabs>
          <Box sx={{ mt: 2 }}>
            {resultTab === 0 ? (
              <pre style={{
                overflow: 'auto',
                maxHeight: '400px',
                backgroundColor: '#f5f5f5',
                padding: '16px',
                borderRadius: '4px',
                width: '100%',
                boxSizing: 'border-box',
                whiteSpace: 'pre-wrap',
                wordBreak: 'break-word'
              }}>
                {JSON.stringify(result, null, 2)}
              </pre>
            ) : (
              <Box sx={{
                whiteSpace: 'pre-wrap',
                wordBreak: 'break-word',
                maxHeight: '400px',
                overflow: 'auto',
                backgroundColor: '#f5f5f5',
                p: 2,
                borderRadius: 1,
                width: '100%',
                boxSizing: 'border-box'
              }}>
                {result.response?.text || result.error || '응답 없음'}
              </Box>
            )}
          </Box>
        </Paper>
      )}
    </Box>
  );
};

export default TestTab;
