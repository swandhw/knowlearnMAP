import { Routes, Route } from 'react-router-dom';
import './App.css';
import Home from './pages/Home';
import NotebookDetail from './components/NotebookDetail';
import Admin from './pages/Admin';
import DomainSelection from './pages/DomainSelection';
import PromptList from './prompt/components/prompts/PromptList';
import PromptDetail from './prompt/components/prompts/PromptDetail';
import Login from './pages/Login';
import Signup from './pages/Signup';
import EmailVerification from './pages/EmailVerification';
import SetPassword from './pages/SetPassword';
import PrivateRoute from './components/PrivateRoute';

function App() {
  return (
    <div className="app">
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/signup" element={<Signup />} />
        <Route path="/verify-email" element={<EmailVerification />} />
        <Route path="/set-password" element={<SetPassword />} />

        <Route element={<PrivateRoute />}>
          <Route path="/" element={<DomainSelection />} />
          <Route path="/workspaces" element={<Home />} />
          <Route path="/notebook/:id" element={<NotebookDetail />} />
          <Route path="/admin/*" element={<Admin />} />
          <Route path="/prompts" element={<PromptList />} />
          <Route path="/prompts/:code" element={<PromptDetail />} />
        </Route>
      </Routes>
    </div>
  );
}

export default App;
