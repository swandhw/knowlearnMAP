import React, { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Box,
  Paper,
  Typography,
  Tabs,
  Tab,
  CircularProgress,
  IconButton,
  TextField,
  Button,
} from '@mui/material';
import { Edit as EditIcon, Check as CheckIcon, Close as CloseIcon, ArrowBack as ArrowBackIcon } from '@mui/icons-material';
import { usePromptDetail, useUpdatePrompt } from '../../hooks/usePrompts';
import { useVersions } from '../../hooks/useVersions';
import EditorTab from '../editor/EditorTab';
import TestTab from '../test/TestTab';
import HistoryTab from '../history/HistoryTab';

function TabPanel({ children, value, index, ...other }) {
  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      id={`tabpanel-${index}`}
      aria-labelledby={`tab-${index}`}
      style={{ width: '100%' }}
      {...other}
    >
      {value === index && <Box sx={{ width: '100%' }}>{children}</Box>}
    </div>
  );
}

const PromptDetailContent = () => {
  const { code } = useParams();
  const navigate = useNavigate();
  const [currentTab, setCurrentTab] = useState(0);
  const [isEditingName, setIsEditingName] = useState(false);
  const [isEditingDescription, setIsEditingDescription] = useState(false);
  const [editedName, setEditedName] = useState('');
  const [editedDescription, setEditedDescription] = useState('');

  const { data: promptData, isLoading: promptLoading } = usePromptDetail(code);
  const { data: versionsData, isLoading: versionsLoading } = useVersions(code);
  const updatePrompt = useUpdatePrompt();

  const handleGoBack = () => {
    navigate('/prompts');
  };

  if (promptLoading || versionsLoading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    );
  }

  const prompt = promptData?.data;
  const versions = Array.isArray(versionsData?.data?.content) ? versionsData.data.content :
    Array.isArray(versionsData?.data) ? versionsData.data : [];

  // versions에 variableSchema가 포함되어 있으므로 별도 API 호출 불필요
  const schemas = [];
  const sets = [];

  const activeVersion = versions.find(v => v.isActive);

  const handleSaveVersion = (versionData) => {
    console.log('Save version:', versionData);
    // TODO: API 호출
  };

  const handlePublishVersion = (versionId) => {
    console.log('Publish version:', versionId);
    // TODO: API 호출
  };

  const handleEditName = () => {
    setEditedName(prompt?.name || '');
    setIsEditingName(true);
  };

  const handleSaveName = async () => {
    if (!editedName.trim()) {
      alert('이름을 입력해주세요.');
      return;
    }

    try {
      await updatePrompt.mutateAsync({
        code,
        data: {
          name: editedName,
          description: prompt?.description
        }
      });
      setIsEditingName(false);
    } catch (error) {
      console.error('Failed to update prompt name:', error);
      alert('이름 수정에 실패했습니다.');
    }
  };

  const handleCancelName = () => {
    setIsEditingName(false);
  };

  const handleEditDescription = () => {
    setEditedDescription(prompt?.description || '');
    setIsEditingDescription(true);
  };

  const handleSaveDescription = async () => {
    try {
      await updatePrompt.mutateAsync({
        code,
        data: {
          name: prompt?.name,
          description: editedDescription
        }
      });
      setIsEditingDescription(false);
    } catch (error) {
      console.error('Failed to update prompt description:', error);
      alert('설명 수정에 실패했습니다.');
    }
  };

  const handleCancelDescription = () => {
    setIsEditingDescription(false);
  };

  return (
    <Box sx={{ p: 3 }}>
      <Paper sx={{ p: 1, mb: 2 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          {/* 코드 */}
          <Typography variant="h6" sx={{ fontWeight: 600, minWidth: '200px', mr: 3 }}>
            코드: {code}
          </Typography>

          {/* 이름 */}
          {isEditingName ? (
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
              <Typography variant="h6" sx={{ fontWeight: 600 }}>이름:</Typography>
              <TextField
                value={editedName}
                onChange={(e) => setEditedName(e.target.value)}
                size="small"
                sx={{ width: '200px' }}
              />
              <IconButton size="small" onClick={handleSaveName} color="primary">
                <CheckIcon fontSize="small" />
              </IconButton>
              <IconButton size="small" onClick={handleCancelName}>
                <CloseIcon fontSize="small" />
              </IconButton>
            </Box>
          ) : (
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
              <Typography variant="h6" sx={{ fontWeight: 600 }}>
                이름: {prompt?.name || code}
              </Typography>
              <IconButton size="small" onClick={handleEditName}>
                <EditIcon fontSize="small" />
              </IconButton>
            </Box>
          )}

          {/* 설명 */}
          {isEditingDescription ? (
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5, flex: 1 }}>
              <TextField
                value={editedDescription}
                onChange={(e) => setEditedDescription(e.target.value)}
                size="small"
                fullWidth
              />
              <IconButton size="small" onClick={handleSaveDescription} color="primary">
                <CheckIcon fontSize="small" />
              </IconButton>
              <IconButton size="small" onClick={handleCancelDescription}>
                <CloseIcon fontSize="small" />
              </IconButton>
            </Box>
          ) : (
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5, flex: 1 }}>
              <Typography variant="body2" color="text.secondary">
                {prompt?.description || '-'}
              </Typography>
              <IconButton size="small" onClick={handleEditDescription}>
                <EditIcon fontSize="small" />
              </IconButton>
            </Box>
          )}
        </Box>
      </Paper>

      <Paper sx={{ width: '100%' }}>
        <Tabs
          value={currentTab}
          onChange={(e, newValue) => setCurrentTab(newValue)}
          sx={{ borderBottom: 1, borderColor: 'divider' }}
        >
          <Tab label="Editor" />
          <Tab label="Test" />
          <Tab label="History" />
        </Tabs>

        <TabPanel value={currentTab} index={0} sx={{ width: '100%' }}>
          <EditorTab
            promptCode={code}
            promptName={prompt?.name || code}
            promptDescription={prompt?.description || ''}
            versions={versions}
            activeVersion={activeVersion}
            variableSchemas={schemas}
            onSave={handleSaveVersion}
            onPublish={handlePublishVersion}
          />
        </TabPanel>

        <TabPanel value={currentTab} index={1}>
          <TestTab
            promptCode={code}
            versions={versions}
            variableSets={sets}
            variableSchemas={schemas}
            llmConfig={prompt?.llmConfig}
          />
        </TabPanel>

        <TabPanel value={currentTab} index={2}>
          <HistoryTab
            promptCode={code}
            versions={versions}
          />
        </TabPanel>
      </Paper>

      {/* 이전 버튼 */}
      <Box sx={{ mt: 3, display: 'flex', justifyContent: 'flex-end' }}>
        <Button
          variant="outlined"
          startIcon={<ArrowBackIcon />}
          onClick={handleGoBack}
          sx={{ minWidth: 120 }}
        >
          이전
        </Button>
      </Box>
    </Box>
  );
};

export default PromptDetailContent;
