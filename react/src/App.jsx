import { Routes, Route } from 'react-router-dom';
import './App.css';
import Home from './pages/Home';
import NotebookDetail from './components/NotebookDetail';
import Admin from './pages/Admin';
import PromptList from './prompt/components/prompts/PromptList';
import PromptDetail from './prompt/components/prompts/PromptDetail';

function App() {
  return (
    <div className="app">
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/notebook/:id" element={<NotebookDetail />} />
        <Route path="/admin/*" element={<Admin />} />
        <Route path="/prompts" element={<PromptList />} />
        <Route path="/prompts/:code" element={<PromptDetail />} />
      </Routes>
    </div>
  );
}

export default App;
