import { useState } from 'react';
import './KnowledgeMapView.css';
import KnowledgeGraphModal from './KnowledgeGraphModal';

function KnowledgeMapView({ sources, title }) {
    // Mock data for the graph
    const [graphSettings, setGraphSettings] = useState({
        graphName: 'KnowlearnGraph',
        startNode: 'ObjectNodes',
        startValue: 'ObjectNodes/(CH2CH2O)8_32...',
        layout: 'forceAtlas2',
        depth: 3,
        limit: 250
    });
    const [kgModalOpen, setKgModalOpen] = useState(false);

    return (
        <div className="knowledge-map-view">
            {/* Left Panel: Graph Visualization */}
            <div className="graph-panel">
                <button className="open-kg-modal" onClick={() => setKgModalOpen(true)}>
                    그래프 보기 (Mock)
                </button>
                <div className="graph-header">
                    <span className="graph-view-title">지식그래프 - {title}</span>
                </div>
                <div className="graph-canvas">
                    {/* Mock Graph Visualization using SVG */}
                    <div className="mock-graph-node start-node" style={{ top: '60%', left: '50%' }}>
                        <div className="node-tooltip">
                            <div className="tooltip-header">◎ 현재 시작 노드</div>
                            <div>ID: ObjectNodes/(CH2CH2O)8_32025</div>
                            <div>라벨: 폴리에틸렌 글리콜 사슬 (8개 단위)</div>
                        </div>
                        <svg width="60" height="60" viewBox="0 0 60 60">
                            <circle cx="30" cy="30" r="15" fill="#FFA500" stroke="#fff" strokeWidth="2" />
                        </svg>
                        <span className="node-label">폴리에틸렌 글리콜 사슬</span>
                    </div>

                    {/* Surrounding Nodes */}
                    {[...Array(15)].map((_, i) => {
                        const angle = (i * (360 / 15)) * (Math.PI / 180);
                        const radius = 200;
                        const x = 50 + (radius * Math.cos(angle) / 800) * 100;
                        const y = 50 + (radius * Math.sin(angle) / 600) * 100;
                        return (
                            <div key={i} className="mock-graph-node" style={{ top: `${y}%`, left: `${x}%` }}>
                                <svg width="200" height="200" style={{ position: 'absolute', top: -100, left: -100, pointerEvents: 'none', zIndex: -1 }}>
                                    <line x1="100" y1="100" x2={100 - (Math.cos(angle) * 100)} y2={100 - (Math.sin(angle) * 100)} stroke="#ccc" strokeWidth="1" />
                                </svg>
                                <circle r="6" fill="#4CAF50" />
                                <svg width="20" height="20" viewBox="0 0 20 20">
                                    <circle cx="10" cy="10" r="6" fill="#4CAF50" stroke="#fff" strokeWidth="1" />
                                </svg>
                                <span className="node-label-small">관련 노드 {i + 1}</span>
                            </div>
                        );
                    })}

                    <div className="graph-stats">
                        102 nodes 118 edges Response time: 64 ms
                    </div>
                </div>
            </div>
            <KnowledgeGraphModal isOpen={kgModalOpen} onClose={() => setKgModalOpen(false)} />

            {/* Right Panel: Graph Settings */}
            <div className="graph-settings-panel">
                <div className="settings-header">
                    <h3>Graph Settings</h3>
                </div>
                <div className="settings-form">
                    <div className="form-group">
                        <label>Graph name</label>
                        <select
                            value={graphSettings.graphName}
                            onChange={(e) => setGraphSettings({ ...graphSettings, graphName: e.target.value })}
                        >
                            <option value="KnowlearnGraph">KnowlearnGraph</option>
                        </select>
                    </div>

                    <div className="form-group">
                        <label>Start node</label>
                        <select
                            value={graphSettings.startNode}
                            onChange={(e) => setGraphSettings({ ...graphSettings, startNode: e.target.value })}
                        >
                            <option value="ObjectNodes">ObjectNodes</option>
                        </select>
                    </div>

                    <div className="form-group">
                        <label>Start value</label>
                        <select
                            value={graphSettings.startValue}
                            onChange={(e) => setGraphSettings({ ...graphSettings, startValue: e.target.value })}
                        >
                            <option value="ObjectNodes/(CH2CH2O)8_32...">ObjectNodes/(CH2CH2O)8_32...</option>
                        </select>
                    </div>

                    <div className="form-group">
                        <label>Layout</label>
                        <select
                            value={graphSettings.layout}
                            onChange={(e) => setGraphSettings({ ...graphSettings, layout: e.target.value })}
                        >
                            <option value="forceAtlas2">forceAtlas2</option>
                        </select>
                    </div>

                    <div className="form-group">
                        <label>Depth</label>
                        <input
                            type="number"
                            value={graphSettings.depth}
                            onChange={(e) => setGraphSettings({ ...graphSettings, depth: e.target.value })}
                        />
                    </div>

                    <div className="form-group">
                        <label>Limit</label>
                        <input
                            type="number"
                            value={graphSettings.limit}
                            onChange={(e) => setGraphSettings({ ...graphSettings, limit: e.target.value })}
                        />
                    </div>

                    <button className="generate-graph-btn">
                        그래프 생성
                    </button>

                    {/* Close button at top right like image? No, image has it globally */}
                </div>
            </div>
        </div>
    );
}

export default KnowledgeMapView;
