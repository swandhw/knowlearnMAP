import React, { useState, useEffect, useRef, useMemo } from 'react';
import ForceGraph2D from 'react-force-graph-2d';
import './KnowledgeGraphModal.css';
import { API_URL } from '../config/api';

export default function KnowledgeGraphModal({ isOpen, onClose, workspaceId, initialSelectedDocIds = [], documents = [] }) {
    const [fullGraphData, setFullGraphData] = useState({ nodes: [], links: [] });
    const [graphData, setGraphData] = useState({ nodes: [], links: [] });
    const graphRef = useRef();
    const [dimensions, setDimensions] = useState({ width: 800, height: 600 });
    const containerRef = useRef(null);

    // Search States
    const [searchQuery, setSearchQuery] = useState('');
    const [depth, setDepth] = useState(1);
    const [suggestions, setSuggestions] = useState([]);
    const [isSearching, setIsSearching] = useState(false);

    const [isLoading, setIsLoading] = useState(false);
    const [selectedDocumentIds, setSelectedDocumentIds] = useState(initialSelectedDocIds || []);
    const [isDocDropdownOpen, setIsDocDropdownOpen] = useState(false);

    // ... (existing constants)

    // ...

    const fetchGraphData = async (docIds = selectedDocumentIds) => {
        // [User Requirement] If no document is selected, show nothing.
        if (!docIds || docIds.length === 0) {
            setFullGraphData({ nodes: [], links: [] });
            setGraphData({ nodes: [], links: [] });
            return;
        }

        setIsLoading(true);
        try {
            console.log(`Fetching graph data from: ${API_URL}/api/graph/workspace/${workspaceId}`);

            const controller = new AbortController();
            const timeoutId = setTimeout(() => controller.abort(), 60000);

            let url = `${API_URL}/api/graph/workspace/${workspaceId}`;
            if (docIds && docIds.length > 0) {
                const queryParams = new URLSearchParams();
                docIds.forEach(id => queryParams.append('documentIds', id));
                url += `?${queryParams.toString()}`;
            }

            try {
                const response = await fetch(url, {
                    credentials: 'include',
                    signal: controller.signal
                });
                clearTimeout(timeoutId);

                if (!response.ok) {
                    console.error('Graph fetch failed:', response.status, response.statusText);
                    throw new Error('Failed to fetch graph data');
                }

                const data = await response.json();
                console.log('Graph data received:', data);

                const rawNodes = Array.isArray(data.nodes) ? data.nodes : [];
                const rawLinks = Array.isArray(data.links) ? data.links : [];

                const nodes = rawNodes.map(n => ({
                    ...n,
                    id: n._id,
                    name: n.label_ko || n.label_en || n.term_ko || n._key,
                    val: 1
                }));

                const links = rawLinks.map(l => ({
                    ...l,
                    source: l._from,
                    target: l._to
                }));

                console.log('Processed Nodes:', nodes.length, 'Links:', links.length);
                const initialData = { nodes, links };
                setFullGraphData(initialData);
                setGraphData(initialData);
            } catch (fetchError) {
                if (fetchError.name === 'AbortError') {
                    console.error("Graph fetch timed out");
                    alert("데이터 요청 시간이 초과되었습니다.");
                } else {
                    throw fetchError;
                }
            }
        } catch (error) {
            console.error("Failed to load graph data", error);
        } finally {
            setIsLoading(false);
        }
    };

    useEffect(() => {
        if (isOpen && containerRef.current) {
            const updateDimensions = () => {
                if (containerRef.current) {
                    setDimensions({
                        width: containerRef.current.clientWidth,
                        height: containerRef.current.clientHeight
                    });
                }
            };
            updateDimensions();
            window.addEventListener('resize', updateDimensions);
            return () => window.removeEventListener('resize', updateDimensions);
        }
    }, [isOpen]);

    // Data Load on Mount & Selection Change
    useEffect(() => {
        fetchGraphData();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [selectedDocumentIds]);

    const handleDocumentToggle = (docId) => {
        const newSelected = selectedDocumentIds.includes(docId)
            ? selectedDocumentIds.filter(id => id !== docId)
            : [...selectedDocumentIds, docId];

        setSelectedDocumentIds(newSelected);
    };

    const handleSelectAllDocs = () => {
        if (selectedDocumentIds.length === documents.length) {
            setSelectedDocumentIds([]);
        } else {
            const allIds = documents.map(d => d.id);
            setSelectedDocumentIds(allIds);
        }
    };

    // Filter Logic
    const handleSearch = () => {
        if (!searchQuery.trim()) {
            setGraphData(fullGraphData);
            setTimeout(() => {
                if (graphRef.current) {
                    graphRef.current.zoomToFit(400);
                }
            }, 100);
            return;
        }

        setIsSearching(true);
        const term = searchQuery.trim().toLowerCase();

        // 1. Find Start Nodes (Wildcard Support)
        const startNodes = fullGraphData.nodes.filter(node => {
            const name = node.name.toLowerCase();
            if (term.includes('*')) {
                const regex = new RegExp('^' + term.replace(/\*/g, '.*') + '$');
                return regex.test(name);
            }
            return name.includes(term);
        });

        if (startNodes.length === 0) {
            alert('검색 결과가 없습니다.');
            setIsSearching(false);
            return;
        }

        // 2. BFS Traversal for Depth
        const visitedNodeIds = new Set();
        const activeLinks = new Set();
        let currentLevel = startNodes.map(n => n.id);

        currentLevel.forEach(id => visitedNodeIds.add(id));

        // Create Adjacency Map for fast lookup
        const adjacency = {};
        fullGraphData.links.forEach(link => {
            const sId = typeof link.source === 'object' ? link.source.id : link.source;
            const tId = typeof link.target === 'object' ? link.target.id : link.target;

            if (!adjacency[sId]) adjacency[sId] = [];
            if (!adjacency[tId]) adjacency[tId] = [];
            adjacency[sId].push({ target: tId, link });
            adjacency[tId].push({ target: sId, link });
        });

        for (let d = 0; d < depth; d++) {
            const nextLevel = [];
            currentLevel.forEach(nodeId => {
                const neighbors = adjacency[nodeId] || [];
                neighbors.forEach(({ target, link }) => {
                    activeLinks.add(link);
                    if (!visitedNodeIds.has(target)) {
                        visitedNodeIds.add(target);
                        nextLevel.push(target);
                    }
                });
            });
            currentLevel = nextLevel;
        }

        const filteredNodes = fullGraphData.nodes.filter(n => visitedNodeIds.has(n.id));
        const filteredLinks = Array.from(activeLinks);

        setGraphData({ nodes: filteredNodes, links: filteredLinks });
        setIsSearching(false);

        setTimeout(() => {
            if (graphRef.current) {
                graphRef.current.zoomToFit(400);
            }
        }, 300);
    };

    const handleReset = () => {
        setSearchQuery('');
        setDepth(1);
        setGraphData(fullGraphData);
        setTimeout(() => {
            if (graphRef.current) {
                graphRef.current.zoomToFit(400);
            }
        }, 100);
    };

    // Autocomplete
    const handleInputChange = (e) => {
        const val = e.target.value;
        setSearchQuery(val);
        if (val.length > 0) {
            const matches = fullGraphData.nodes
                .filter(n => n.name.toLowerCase().includes(val.toLowerCase()))
                .map(n => n.name)
                .slice(0, 10);
            setSuggestions(matches);
        } else {
            setSuggestions([]);
        }
    };

    const selectSuggestion = (val) => {
        setSearchQuery(val);
        setSuggestions([]);
    };



    return (
        <div className="kg-modal-overlay" onClick={onClose}>
            <div className="kg-modal-content" style={{ width: '98%', height: '98vh', maxWidth: 'none', display: 'flex', flexDirection: 'column' }} onClick={e => e.stopPropagation()}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px', flexShrink: 0 }}>
                    {/* ... (Header content unchanged) ... */}
                    <h2 className="kg-modal-title" style={{ margin: 0 }}>지식 그래프</h2>

                    {/* Search Controls */}
                    <div className="kg-search-controls">
                        <div className="kg-node-count" style={{ marginRight: '15px', fontWeight: 'bold', color: '#333', fontSize: '14px' }}>
                            총 노드: {graphData.nodes.length}개
                        </div>

                        <div className="kg-input-group">
                            <input
                                type="text"
                                className="kg-search-input"
                                placeholder="노드 검색 (*와일드카드 가능)"
                                value={searchQuery}
                                onChange={handleInputChange}
                                onKeyDown={(e) => e.key === 'Enter' && handleSearch()}
                            />
                            {suggestions.length > 0 && (
                                <ul className="kg-suggestions">
                                    {suggestions.map((s, i) => (
                                        <li key={i} onClick={() => selectSuggestion(s)}>{s}</li>
                                    ))}
                                </ul>
                            )}
                        </div>
                        <div className="kg-input-group" style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
                            <span style={{ color: '#333', fontSize: '14px', fontWeight: '500' }}>Depth:</span>
                            <input
                                type="number"
                                className="kg-depth-input"
                                min="1" max="5"
                                value={depth}
                                onChange={(e) => setDepth(parseInt(e.target.value))}
                                title="Depth (깊이)"
                                style={{ width: '60px' }}
                            />
                        </div>
                        <button className="kg-btn primary" onClick={handleSearch}>검색</button>
                        <button className="kg-btn secondary" onClick={handleReset}>초기화</button>
                    </div>

                    {/* Document Filter Dropdown */}
                    <div style={{ position: 'relative', marginLeft: '10px' }}>
                        <button
                            className="kg-btn secondary"
                            onClick={() => setIsDocDropdownOpen(!isDocDropdownOpen)}
                            style={{ display: 'flex', alignItems: 'center', gap: '5px' }}
                        >
                            <span>문서 필터 ({selectedDocumentIds.length}/{documents.length})</span>
                            <span style={{ fontSize: '10px' }}>{isDocDropdownOpen ? '▲' : '▼'}</span>
                        </button>

                        {isDocDropdownOpen && (
                            <div className="kg-dropdown-menu" style={{
                                position: 'absolute',
                                top: '100%',
                                right: 0,
                                transform: 'none',
                                zIndex: 1000,
                                backgroundColor: '#2d2d2d',
                                border: '1px solid #444',
                                borderRadius: '4px',
                                padding: '8px',
                                minWidth: '250px',
                                maxHeight: '300px',
                                overflowY: 'auto',
                                boxShadow: '0 4px 12px rgba(0,0,0,0.5)'
                            }}>
                                <div style={{ marginBottom: '8px', paddingBottom: '8px', borderBottom: '1px solid #444' }}>
                                    <label style={{ display: 'flex', alignItems: 'center', color: '#fff', cursor: 'pointer' }}>
                                        <input
                                            type="checkbox"
                                            checked={documents.length > 0 && selectedDocumentIds.length === documents.length}
                                            onChange={handleSelectAllDocs}
                                            style={{ marginRight: '8px' }}
                                        />
                                        전체 선택
                                    </label>
                                </div>
                                {documents.map(doc => (
                                    <div key={doc.id} style={{ marginBottom: '4px' }}>
                                        <label style={{ display: 'flex', alignItems: 'center', color: '#eee', fontSize: '13px', cursor: 'pointer' }}>
                                            <input
                                                type="checkbox"
                                                checked={selectedDocumentIds.includes(doc.id)}
                                                onChange={() => handleDocumentToggle(doc.id)}
                                                style={{ marginRight: '8px' }}
                                            />
                                            <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', maxWidth: '200px' }} title={doc.filename}>
                                                {doc.filename}
                                            </span>
                                        </label>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>

                    <div style={{ flex: 1 }}></div>

                    <button className="kg-close-btn" style={{ margin: 0 }} onClick={onClose}>닫기</button>
                </div>

                <div ref={containerRef} style={{ flex: 1, border: '1px solid #333', borderRadius: '4px', overflow: 'hidden', position: 'relative' }}>
                    {isLoading && (
                        <div style={{
                            position: 'absolute',
                            top: 0,
                            left: 0,
                            right: 0,
                            bottom: 0,
                            backgroundColor: 'rgba(0,0,0,0.7)',
                            zIndex: 2000,
                            display: 'flex',
                            flexDirection: 'column',
                            justifyContent: 'center',
                            alignItems: 'center',
                            color: '#fff'
                        }}>
                            <div className="spinner" style={{
                                width: '40px',
                                height: '40px',
                                border: '4px solid rgba(255,255,255,0.3)',
                                borderRadius: '50%',
                                borderTop: '4px solid #fff',
                                animation: 'spin 1s linear infinite',
                                marginBottom: '10px'
                            }}></div>
                            <span>데이터 로딩 중...</span>
                            <style>{`
                                @keyframes spin {
                                    0% { transform: rotate(0deg); }
                                    100% { transform: rotate(360deg); }
                                }
                            `}</style>
                        </div>
                    )}
                    <ForceGraph2D
                        ref={graphRef}
                        width={dimensions.width}
                        height={dimensions.height}
                        graphData={graphData}
                        nodeLabel="name"
                        nodeAutoColorBy="group"
                        backgroundColor="#ffffff" // White background (Neo4j style)

                        // Custom Node Rendering (Label)
                        nodeCanvasObject={(node, ctx, globalScale) => {
                            const label = node.name;
                            const fontSize = 12 / globalScale;
                            ctx.font = `${fontSize}px "Pretendard Variable", sans-serif`;
                            const textWidth = ctx.measureText(label).width;
                            const bckgDimensions = [textWidth, fontSize].map(n => n + fontSize * 0.2); // some padding

                            // Highlight selected/found node
                            if (node.id === searchQuery) {
                                ctx.beginPath();
                                ctx.arc(node.x, node.y, 8, 0, 2 * Math.PI, false);
                                ctx.fillStyle = 'rgba(255, 215, 0, 0.5)'; // Gold highlight
                                ctx.fill();
                            }

                            // Draw Node Circle
                            ctx.beginPath();
                            ctx.arc(node.x, node.y, 5, 0, 2 * Math.PI, false); // Increased radius slightly
                            ctx.fillStyle = node.color || '#fac858'; // Default fallback color
                            ctx.fill();
                            ctx.strokeStyle = '#fff';
                            ctx.lineWidth = 1.5 / globalScale;
                            ctx.stroke();

                            // Draw Label (Dark text for white bg)
                            ctx.textAlign = 'center';
                            ctx.textBaseline = 'middle';
                            ctx.fillStyle = '#333'; // Dark text
                            ctx.fillText(label, node.x, node.y + 8);

                            node.__bckgDimensions = bckgDimensions; // to re-use in nodePointerAreaPaint
                        }}

                        // Custom Link Rendering (Label + Arrow)
                        linkCanvasObject={(link, ctx, globalScale) => {
                            const start = link.source;
                            const end = link.target;

                            if (typeof start.x !== 'number' || typeof end.x !== 'number') return;

                            const nodeRadius = 6; // Node radius + padding
                            const arrowLength = 6 / globalScale;

                            const deltaX = end.x - start.x;
                            const deltaY = end.y - start.y;
                            const angle = Math.atan2(deltaY, deltaX);

                            // Calculate arrow tip position (near target node)
                            const tipX = end.x - nodeRadius * Math.cos(angle);
                            const tipY = end.y - nodeRadius * Math.sin(angle);

                            // Neo4j-style Light Edge color
                            const edgeColor = '#A5ABB6';

                            // Draw Line
                            ctx.beginPath();
                            ctx.moveTo(start.x, start.y);
                            ctx.lineTo(tipX, tipY);
                            ctx.strokeStyle = edgeColor;
                            ctx.lineWidth = 1.5 / globalScale; // Slightly thicker
                            ctx.stroke();

                            // Draw Arrow Head
                            ctx.beginPath();
                            ctx.moveTo(tipX, tipY);
                            ctx.lineTo(
                                tipX - arrowLength * Math.cos(angle - Math.PI / 6),
                                tipY - arrowLength * Math.sin(angle - Math.PI / 6)
                            );
                            ctx.lineTo(
                                tipX - arrowLength * Math.cos(angle + Math.PI / 6),
                                tipY - arrowLength * Math.sin(angle + Math.PI / 6)
                            );
                            ctx.closePath();
                            ctx.fillStyle = edgeColor;
                            ctx.fill();

                            // Draw Label
                            if (link.label_ko || link.label_en) {
                                const label = link.label_ko || link.label_en || "";
                                const fontSize = 10 / globalScale;
                                ctx.font = `${fontSize}px "Pretendard Variable", sans-serif`;

                                // Calculate midpoint
                                const textX = start.x + (end.x - start.x) / 2;
                                const textY = start.y + (end.y - start.y) / 2;

                                // Label Background for readability
                                const textWidth = ctx.measureText(label).width;
                                ctx.fillStyle = 'rgba(255, 255, 255, 0.8)';
                                ctx.fillRect(textX - textWidth / 2 - 2, textY - fontSize / 2 - 2, textWidth + 4, fontSize + 4);

                                ctx.textAlign = 'center';
                                ctx.textBaseline = 'middle';
                                ctx.fillStyle = '#666'; // Darker gray for edge label
                                ctx.fillText(label, textX, textY);
                            }
                        }}
                    />
                </div>
            </div>
        </div>
    );
}

