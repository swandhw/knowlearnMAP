import React from 'react';
import {
  Box,
  Tabs,
  Tab,
  TextField,
  IconButton,
  Tooltip,
} from '@mui/material';
import {
  ContentCopy as CopyIcon,
  ContentPaste as PasteIcon,
  ClearAll as ClearIcon,
  Fullscreen as FullscreenIcon,
  FullscreenExit as FullscreenExitIcon,
  CheckCircleOutline as CheckIcon,
} from '@mui/icons-material';

const PromptEditTabs = ({
  activeTab,
  onTabChange,
  extractedVariables = [],
  promptContent,
  onPromptContentChange,
  variables = {},
  onVariableUpdate,
  onCheckVariables,
  disabled = false,
  showToolbar = false,
  isExpanded = false,
  onToggleExpand,
  customHeight,
  children,
}) => {
  // 툴바 핸들러
  const handleCopy = () => {
    if (activeTab === 0) {
      navigator.clipboard.writeText(promptContent);
    } else {
      const varKey = extractedVariables[activeTab - 1];
      navigator.clipboard.writeText(variables[varKey]?.content || '');
    }
  };

  const handlePaste = async () => {
    const text = await navigator.clipboard.readText();
    if (activeTab === 0) {
      onPromptContentChange(text);
    } else {
      const varKey = extractedVariables[activeTab - 1];
      onVariableUpdate(varKey, 'content', text);
    }
  };

  const handleClear = () => {
    if (activeTab === 0) {
      onPromptContentChange('');
    } else {
      const varKey = extractedVariables[activeTab - 1];
      onVariableUpdate(varKey, 'content', '');
    }
  };

  return (
    <Box id="prompt-edit-tabs-root" sx={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
      {/* Tabs와 툴바 버튼 */}
      <Box
        sx={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          borderBottom: 1,
          borderColor: 'divider',
        }}
        id="tabs-toolbar-header"
      >
        {/* 탭과 변수 체크 버튼 */}
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }} id="tabs-and-check-box">
          <Tabs
            value={activeTab}
            onChange={(e, newValue) => onTabChange(newValue)}
            sx={{
              minHeight: 40,
              '& .MuiTab-root': {
                minHeight: 40,
                textTransform: 'none',
                fontWeight: 500,
              }
            }}
          >
            <Tab label="프롬프트 편집" disabled={disabled} />
            {extractedVariables.map((varKey, index) => (
              <Tab
                key={varKey}
                label={varKey}
                value={index + 1}
                disabled={disabled}
                sx={{
                  color: !variables[varKey]?.content ? 'error.main' : undefined,
                  '&.Mui-selected': {
                    color: !variables[varKey]?.content ? 'error.main' : 'primary.main',
                  }
                }}
              />
            ))}
          </Tabs>

          {/* 변수 체크 버튼 (프롬프트 편집 탭에서만 표시) */}
          {activeTab === 0 && (
            <Tooltip title="변수 체크">
              <IconButton
                size="small"
                onClick={onCheckVariables}
                disabled={disabled}
                sx={{ ml: 1 }}
              >
                <CheckIcon fontSize="small" />
              </IconButton>
            </Tooltip>
          )}
        </Box>

        {/* 툴바 버튼들 (optional) */}
        {showToolbar && (
          <Box sx={{ display: 'flex', gap: 0.5, pr: 1 }} id="toolbar-buttons-box">
            <Tooltip title="복사">
              <IconButton size="small" onClick={handleCopy} disabled={disabled}>
                <CopyIcon fontSize="small" />
              </IconButton>
            </Tooltip>
            <Tooltip title="붙여넣기">
              <IconButton size="small" onClick={handlePaste} disabled={disabled}>
                <PasteIcon fontSize="small" />
              </IconButton>
            </Tooltip>
            <Tooltip title="내용 지우기">
              <IconButton size="small" onClick={handleClear} disabled={disabled}>
                <ClearIcon fontSize="small" />
              </IconButton>
            </Tooltip>
            {onToggleExpand && (
              <Tooltip title={isExpanded ? "축소" : "확장"}>
                <IconButton size="small" onClick={onToggleExpand} disabled={disabled}>
                  {isExpanded ? <FullscreenExitIcon fontSize="small" /> : <FullscreenIcon fontSize="small" />}
                </IconButton>
              </Tooltip>
            )}
          </Box>
        )}
      </Box>

      {/* 프롬프트 편집 탭 */}
      {activeTab === 0 && (
        <Box sx={{
          mt: 2,
          mb: 0,
          border: showToolbar ? '1px solid' : 'none',
          borderColor: 'divider',
          borderRadius: showToolbar ? 1 : 0,
          height: customHeight || (showToolbar ? (isExpanded ? 'calc(100vh - 350px)' : '450px') : 'auto'),
          overflow: showToolbar ? 'hidden' : 'visible'
        }} id="prompt-content-textarea-box">
          <TextField
            value={promptContent}
            onChange={(e) => onPromptContentChange(e.target.value)}
            fullWidth
            multiline
            disabled={disabled}
            placeholder="프롬프트 내용을 입력하세요&#10;&#10;예시:&#10;You are a document chunking assistant.&#10;Split the following document into chunks based on the rule: {{rule}}.&#10;Language: {{lang}}.&#10;Max length per chunk: {{max_length}} characters."
            variant={showToolbar ? "standard" : "outlined"}
            minRows={showToolbar ? undefined : 20}
            maxRows={showToolbar ? undefined : 25}
            id="prompt-content-textarea"
            InputProps={{
              disableUnderline: showToolbar,
            }}
            sx={{
              height: showToolbar ? '100%' : 'auto',
              mb: showToolbar ? 0 : 2,
              '& .MuiInputBase-root': {
                bgcolor: 'background.paper',
                fontFamily: 'monospace',
                fontSize: showToolbar ? '0.875rem' : '14px',
                p: showToolbar ? 1.5 : undefined,
                height: showToolbar ? '100%' : 'auto',
                alignItems: 'flex-start',
                '& textarea': showToolbar ? {
                  height: '100% !important',
                  overflow: 'auto !important',
                  resize: 'none',
                } : {}
              }
            }}
          />
          {children && activeTab === 0 && <Box sx={{ mt: 2 }}>{children}</Box>}
        </Box>
      )}

      {/* 변수 탭들 - 각 변수별로 textarea */}
      {extractedVariables.map((varKey, index) => (
        activeTab === index + 1 && (
          <Box key={varKey} sx={{
            mt: 2,
            mb: 0,
            border: showToolbar ? '1px solid' : 'none',
            borderColor: 'divider',
            borderRadius: showToolbar ? 1 : 0,
            height: customHeight || (showToolbar ? (isExpanded ? 'calc(100vh - 350px)' : '450px') : 'auto'),
            overflow: showToolbar ? 'hidden' : 'visible'
          }} id={`variable-textarea-box-${varKey}`}>
            <TextField
              value={variables[varKey]?.content || ''}
              onChange={(e) => onVariableUpdate(varKey, 'content', e.target.value)}
              fullWidth
              multiline
              disabled={disabled}
              placeholder={`${varKey} 변수의 내용을 입력하세요`}
              variant={showToolbar ? "standard" : "outlined"}
              minRows={showToolbar ? undefined : 20}
              maxRows={showToolbar ? undefined : 25}
              id={`variable-textarea-${varKey}`}
              InputProps={{
                disableUnderline: showToolbar,
              }}
              sx={{
                height: showToolbar ? '100%' : 'auto',
                '& .MuiInputBase-root': {
                  bgcolor: 'background.paper',
                  fontFamily: 'monospace',
                  fontSize: showToolbar ? '0.875rem' : '14px',
                  p: showToolbar ? 1.5 : undefined,
                  height: showToolbar ? '100%' : 'auto',
                  alignItems: 'flex-start',
                  '& textarea': showToolbar ? {
                    height: '100% !important',
                    overflow: 'auto !important',
                    resize: 'none',
                  } : {}
                }
              }}
            />
          </Box>
        )
      ))}
    </Box>
  );
};

export default PromptEditTabs;
