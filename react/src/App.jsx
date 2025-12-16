import { Routes, Route } from 'react-router-dom';
import './App.css';
import Home from './pages/Home';
import NotebookDetail from './components/NotebookDetail';

function App() {
  return (
    <div className="app">
      {/* Header is now part of pages if they have different headers, 
          or global if they share it. 
          Use conditional rendering or Layout component if needed. 
          Currently Home has Header, NotebookDetail has Header. 
          So App just routes. 
          But the original App had a 'header' class wrapping the whole app? 
          No, 'div.app' splits into Header and Main. 
          Home has Header inside it now? 
          Wait, I copied Header into Home.jsx in previous step? 
          Let's check Home.jsx content again in next step or assume yes.
          NotebookDetail has its own header.
          So App just renders Routes.
      */}
      <Routes>
        <Route path="/" element={<Home />} />
        <Route path="/notebook/:id" element={<NotebookDetail />} />
      </Routes>
    </div>
  );
}

export default App;
