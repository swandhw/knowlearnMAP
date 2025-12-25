import React, { useEffect, useState, useRef } from 'react';
import ForceGraph2D from 'react-force-graph-2d';
import './KnowledgeGraphModal.css';

const KnowledgeGraphModal = ({ isOpen, onClose, workspaceId, selectedDocumentIds }) => {
    const [graphData, setGraphData] = useState({ nodes: [], links: [] });
    const [loading, setLoading] = useState(false);
    const [searchTerm, setSearchTerm] = useState('');
    const [suggestions, setSuggestions] = useState([]);
    const graphRef = useRef();

    const handleSearchChange = (e) => {
        const term = e.target.value;
        setSearchTerm(term);
        if (term.trim() === '') {
            setSuggestions([]);
            return;
        }
        const lowerTerm = term.toLowerCase();
        // Wildcard/Partial match logic
        const filtered = graphData.nodes.filter(n =>
            (n.name && n.name.toLowerCase().includes(lowerTerm))
        ).slice(0, 10); // Limit suggestions
        setSuggestions(filtered);
    };

    const handleSelectNode = (node) => {
        setSearchTerm(node.name);
        setSuggestions([]);
        if (graphRef.current) {
            graphRef.current.centerAt(node.x, node.y, 1000);
            graphRef.current.zoom(8, 2000);
        }
    };

    useEffect(() => {
        if (isOpen && workspaceId) {
            fetchGraphData();
        }
    }, [isOpen, workspaceId, selectedDocumentIds]);

    const fetchGraphData = async () => {
        setLoading(true);
        try {
            // Build Query Params
            let url = `/api/graph/workspace/${workspaceId}`;
            if (selectedDocumentIds && selectedDocumentIds.length > 0) {
                const params = new URLSearchParams();
                selectedDocumentIds.forEach(id => params.append('documentIds', id));
                url += `?${params.toString()}`;
            }

            const API_URL = import.meta.env.VITE_APP_API_URL || 'http://localhost:8080';
            const response = await fetch(`${API_URL}${url}`);
            if (!response.ok) throw new Error('Failed to fetch graph data');

            const data = await response.json();

            // Transform data for react-force-graph
            // API returns { nodes: [...], links: [...] } matching the library format
            // But we might need to process IDs or colors
            const processedNodes = data.nodes.map(node => ({
                ...node,
                id: node._key || node.id,
                name: node.label_ko || node.label_en || node.term_ko || node.term_en || 'Node',
                val: 1 // Default size
            }));

            const processedLinks = data.links.map(link => ({
                ...link,
                source: link._from.split('/')[1], // ArangoDB returns "ObjectNodes/Key"
                target: link._to.split('/')[1]
            }));

            setGraphData({ nodes: processedNodes, links: processedLinks });
        } catch (error) {
            console.error("Graph fetch error:", error);
        } finally {
            setLoading(false);
        }
    };

    if (!isOpen) return null;

    return (
        <div className="graph-modal-overlay">
            <div className="graph-modal-content">
                <button className="graph-modal-close" onClick={onClose}>&times;</button>
                <div className="graph-controls" style={{ padding: '10px', display: 'flex', gap: '10px', alignItems: 'center', position: 'relative', zIndex: 1001 }}>
                    <label style={{ fontWeight: 'bold' }}>시작 노드 검색:</label>
                    <div style={{ position: 'relative', width: '200px' }}>
                        <input
                            type="text"
                            value={searchTerm}
                            onChange={handleSearchChange}
                            placeholder="노드 이름 입력..."
                            style={{
                                padding: '5px',
                                borderRadius: '4px',
                                border: '1px solid #ccc',
                                width: '100%'
                            }}
                            onKeyDown={(e) => {
                                if (e.key === 'Enter') {
                                    // Select first suggestion or exact match
                                    if (suggestions.length > 0) {
                                        handleSelectNode(suggestions[0]);
                                    }
                                }
                            }}
                        />
                        {suggestions.length > 0 && (
                            <ul style={{
                                position: 'absolute',
                                top: '100%',
                                left: 0,
                                right: 0,
                                backgroundColor: 'white',
                                border: '1px solid #ccc',
                                borderRadius: '4px',
                                maxHeight: '200px',
                                overflowY: 'auto',
                                margin: 0,
                                padding: 0,
                                listStyle: 'none',
                                zIndex: 1002,
                                boxShadow: '0 2px 4px rgba(0,0,0,0.1)'
                            }}>
                                {suggestions.map(node => (
                                    <li
                                        key={node.id}
                                        onClick={() => handleSelectNode(node)}
                                        style={{
                                            padding: '8px',
                                            cursor: 'pointer',
                                            borderBottom: '1px solid #eee'
                                        }}
                                        onMouseEnter={(e) => e.target.style.backgroundColor = '#f0f0f0'}
                                        onMouseLeave={(e) => e.target.style.backgroundColor = 'white'}
                                    >
                                        {node.name}
                                    </li>
                                ))}
                            </ul>
                        )}
                    </div>
                </div>
                <div className="graph-container">
                    {loading ? (
                        <div className="graph-loading">지식 그래프 로딩중...</div>
                    ) : (
                        <ForceGraph2D
                            ref={graphRef}
                            graphData={graphData}
                            nodeLabel="name"
                            nodeAutoColorBy="group"
                            linkDirectionalArrowLength={3.5}
                            linkDirectionalArrowRelPos={1}
                            nodeCanvasObject={(node, ctx, globalScale) => {
                                const label = node.name;
                                const fontSize = 12 / globalScale;
                                ctx.font = `${fontSize}px Sans-Serif`;

                                // Draw Node Circle
                                const r = 5;
                                ctx.beginPath();
                                ctx.arc(node.x, node.y, r, 0, 2 * Math.PI, false);
                                ctx.fillStyle = node.color || '#333';
                                ctx.fill();

                                // Draw Label
                                const textWidth = ctx.measureText(label).width;
                                const bckgDimensions = [textWidth, fontSize].map(n => n + fontSize * 0.2); // some padding

                                ctx.textAlign = 'center';
                                ctx.textBaseline = 'top'; // Draw from top

                                const labelY = node.y + r + 4; // Start 4px below the circle

                                // Label Background
                                ctx.fillStyle = 'rgba(255, 255, 255, 0.8)';
                                ctx.fillRect(node.x - bckgDimensions[0] / 2, labelY - bckgDimensions[1] * 0.1, ...bckgDimensions);

                                // Label Text
                                ctx.fillStyle = '#333';
                                ctx.fillText(label, node.x, labelY);

                                node.__bckgDimensions = bckgDimensions; // to re-use in nodePointerAreaPaint
                            }}
                            nodePointerAreaPaint={(node, color, ctx) => {
                                ctx.fillStyle = color;
                                const bckgDimensions = node.__bckgDimensions;
                                const r = 5;
                                // Paint circle area
                                ctx.beginPath();
                                ctx.arc(node.x, node.y, r, 0, 2 * Math.PI, false);
                                ctx.fill();
                            }}
                            linkCanvasObject={(link, ctx, globalScale) => {
                                const start = link.source;
                                const end = link.target;

                                // ignore unbound links
                                if (typeof start !== 'object' || typeof end !== 'object') return;

                                // Draw Line
                                ctx.beginPath();
                                ctx.moveTo(start.x, start.y);
                                ctx.lineTo(end.x, end.y);
                                ctx.lineWidth = 1 / globalScale;
                                ctx.strokeStyle = '#cccccc';
                                ctx.stroke();

                                const label = link.label_ko || link.label_en || link.relation_ko || link.relation_en || link.type || '';
                                if (!label) return;

                                const textPos = Object.assign(...['x', 'y'].map(c => ({
                                    [c]: start[c] + (end[c] - start[c]) / 2 // calc middle point
                                })));

                                const relLink = { x: end.x - start.x, y: end.y - start.y };

                                const maxTextLength = Math.sqrt(Math.pow(relLink.x, 2) + Math.pow(relLink.y, 2)) - 10;

                                if (maxTextLength < 10) return; // Don't draw if too short

                                const fontSize = 10 / globalScale;
                                ctx.font = `${fontSize}px Sans-Serif`;

                                // Draw text background
                                const textWidth = ctx.measureText(label).width;
                                if (textWidth > maxTextLength) return; // Skip if label doesn't fit

                                ctx.save();
                                ctx.translate(textPos.x, textPos.y);

                                const angle = Math.atan2(relLink.y, relLink.x);
                                ctx.rotate(angle);

                                // Flip text if upside down
                                if (angle > Math.PI / 2 || angle < -Math.PI / 2) {
                                    ctx.rotate(Math.PI);
                                }

                                ctx.textAlign = 'center';
                                ctx.textBaseline = 'middle';

                                // White background for edge label
                                ctx.fillStyle = 'rgba(255, 255, 255, 0.9)';
                                const padding = 2;
                                ctx.fillRect(-textWidth / 2 - padding, -fontSize / 2 - padding, textWidth + padding * 2, fontSize + padding * 2);

                                ctx.fillStyle = '#666';
                                ctx.fillText(label, 0, 0);
                                ctx.restore();
                            }}
                            onNodeClick={node => {
                                // Zoom to node or show details
                                graphRef.current.centerAt(node.x, node.y, 1000);
                                graphRef.current.zoom(8, 2000);
                            }}
                        />
                    )}
                </div>
            </div>
        </div>
    );
};

export default KnowledgeGraphModal;
