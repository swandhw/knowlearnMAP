import React, { useState, useEffect, useRef } from 'react';
import ForceGraph2D from 'react-force-graph-2d';
import './KnowledgeGraphModal.css'; // Reusing styles
import { API_URL } from '../config/api';

export default function KnowledgeMapView({ workspaceId, documents = [], initialSelectedDocIds = [] }) {
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

    const fetchGraphData = async (docIds = selectedDocumentIds) => {
        if (!docIds || docIds.length === 0) {
            setFullGraphData({ nodes: [], links: [] });
            setGraphData({ nodes: [], links: [] });
            return;
        }

        setIsLoading(true);
        try {
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

                if (!response.ok) throw new Error('Failed to fetch graph data');

                const responseData = await response.json();
                // Handle ApiResponse format or direct format
                const data = responseData.data ? responseData.data : responseData;

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

                const initialData = { nodes, links };
                setFullGraphData(initialData);
                setGraphData(initialData);
            } catch (fetchError) {
                if (fetchError.name !== 'AbortError') {
                    console.error("Failed to load graph data", fetchError);
                }
            }
        } catch (error) {
            console.error("Error in fetchGraphData", error);
        } finally {
            setIsLoading(false);
        }
    };

    useEffect(() => {
        const updateDimensions = () => {
            if (containerRef.current) {
                setDimensions({
                    width: containerRef.current.clientWidth,
                    height: containerRef.current.clientHeight
                });
            }
        };

        // Initial delay to allow layout to settle
        const timer = setTimeout(updateDimensions, 100);
        window.addEventListener('resize', updateDimensions);

        return () => {
            clearTimeout(timer);
            window.removeEventListener('resize', updateDimensions);
        };
    }, []);

    useEffect(() => {
        // Re-check dimensions when data changes (optional, but sometimes needed)
        if (containerRef.current) {
            setDimensions({
                width: containerRef.current.clientWidth,
                height: containerRef.current.clientHeight
            });
        }
    }, [graphData]);

    useEffect(() => {
        fetchGraphData();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [selectedDocumentIds]);

    // Sync with parent's selection
    useEffect(() => {
        if (initialSelectedDocIds) {
            setSelectedDocumentIds(initialSelectedDocIds);
        }
    }, [initialSelectedDocIds]);

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

    const handleSearch = () => {
        if (!searchQuery.trim()) {
            setGraphData(fullGraphData);
            setTimeout(() => { if (graphRef.current) graphRef.current.zoomToFit(400); }, 100);
            return;
        }

        setIsSearching(true);
        const term = searchQuery.trim().toLowerCase();

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

        const visitedNodeIds = new Set();
        const activeLinks = new Set();
        let currentLevel = startNodes.map(n => n.id);

        currentLevel.forEach(id => visitedNodeIds.add(id));

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
        setTimeout(() => { if (graphRef.current) graphRef.current.zoomToFit(400); }, 300);
    };

    const handleReset = () => {
        setSearchQuery('');
        setDepth(1);
        setGraphData(fullGraphData);
        setTimeout(() => { if (graphRef.current) graphRef.current.zoomToFit(400); }, 100);
    };

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
        <div style={{ display: 'flex', flexDirection: 'column', height: '100%', width: '100%', position: 'relative' }}>
            {/* Toolbar */}
            <div style={{ display: 'flex', alignItems: 'center', padding: '10px', backgroundColor: '#f5f5f5', borderBottom: '1px solid #ddd', gap: '10px' }}>
                <div className="kg-input-group">
                    <input
                        type="text"
                        className="kg-search-input"
                        placeholder="노드 검색 (*와일드카드)"
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
                <div className="kg-input-group" style={{ width: '60px' }}>
                    <input
                        type="number"
                        className="kg-depth-input"
                        min="1" max="5"
                        value={depth}
                        onChange={(e) => setDepth(parseInt(e.target.value))}
                        title="Depth"
                    />
                </div>
                <button className="kg-btn primary" onClick={handleSearch}>검색</button>
                <button className="kg-btn secondary" onClick={handleReset}>초기화</button>

                <div style={{ position: 'relative', marginLeft: 'auto' }}>
                    <button
                        className="kg-btn secondary"
                        onClick={() => setIsDocDropdownOpen(!isDocDropdownOpen)}
                    >
                        문서 필터 ({selectedDocumentIds.length}/{documents.length}) ▼
                    </button>
                    {isDocDropdownOpen && (
                        <div className="kg-dropdown-menu" style={{
                            position: 'absolute', top: '100%', right: 0, zIndex: 1000,
                            backgroundColor: '#2d2d2d', border: '1px solid #444',
                            padding: '8px', minWidth: '250px', maxHeight: '300px', overflowY: 'auto'
                        }}>
                            <div style={{ marginBottom: '8px', paddingBottom: '8px', borderBottom: '1px solid #444' }}>
                                <label style={{ display: 'flex', alignItems: 'center', color: '#fff', cursor: 'pointer' }}>
                                    <input type="checkbox" checked={documents.length > 0 && selectedDocumentIds.length === documents.length} onChange={handleSelectAllDocs} style={{ marginRight: '8px' }} />
                                    전체 선택
                                </label>
                            </div>
                            {documents.map(doc => (
                                <div key={doc.id} style={{ marginBottom: '4px' }}>
                                    <label style={{ display: 'flex', alignItems: 'center', color: '#eee', fontSize: '13px', cursor: 'pointer' }}>
                                        <input type="checkbox" checked={selectedDocumentIds.includes(doc.id)} onChange={() => handleDocumentToggle(doc.id)} style={{ marginRight: '8px' }} />
                                        <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', maxWidth: '200px' }} title={doc.filename}>{doc.filename}</span>
                                    </label>
                                </div>
                            ))}
                        </div>
                    )}
                </div>
            </div>

            <div ref={containerRef} style={{ flex: 1, position: 'relative', overflow: 'hidden', backgroundColor: '#fff' }}>
                {isLoading && (
                    <div className="kg-loading-overlay">
                        <div className="kg-progress-container">
                            <div className="kg-progress-bar"></div>
                        </div>
                        <span>지식 그래프 불러오는 중...</span>
                    </div>
                )}

                <ForceGraph2D
                    ref={graphRef}
                    width={dimensions.width}
                    height={dimensions.height}
                    graphData={graphData}
                    nodeLabel="name"
                    nodeAutoColorBy="group"
                    backgroundColor="#ffffff"
                    nodeCanvasObject={(node, ctx, globalScale) => {
                        const label = node.name;
                        const fontSize = 12 / globalScale;
                        ctx.font = `${fontSize}px "Pretendard Variable", sans-serif`;

                        // Check match by Name or ID
                        const isMatch = node.id === searchQuery || (searchQuery && node.name && node.name.toLowerCase() === searchQuery.trim().toLowerCase());

                        if (isMatch) {
                            ctx.beginPath();
                            ctx.arc(node.x, node.y, 8, 0, 2 * Math.PI, false);
                            ctx.fillStyle = '#FFA500'; // Orange highlight
                            ctx.fill();
                            ctx.strokeStyle = '#FF8C00';
                            ctx.lineWidth = 2 / globalScale;
                            ctx.stroke();
                        }

                        ctx.beginPath();
                        ctx.arc(node.x, node.y, 5, 0, 2 * Math.PI, false);
                        ctx.fillStyle = isMatch ? '#FFA500' : (node.color || '#fac858');
                        ctx.fill();
                        ctx.strokeStyle = '#fff';
                        ctx.lineWidth = 1.5 / globalScale;
                        ctx.stroke();

                        ctx.textAlign = 'center';
                        ctx.textBaseline = 'middle';
                        ctx.fillStyle = '#333';
                        ctx.fillText(label, node.x, node.y + 8);
                    }}
                    linkCanvasObject={(link, ctx, globalScale) => {
                        const start = link.source;
                        const end = link.target;
                        if (typeof start.x !== 'number' || typeof end.x !== 'number') return;

                        // Draw Line
                        ctx.beginPath();
                        ctx.moveTo(start.x, start.y);
                        ctx.lineTo(end.x, end.y);
                        ctx.strokeStyle = '#A5ABB6';
                        ctx.lineWidth = 1.5 / globalScale;
                        ctx.stroke();

                        // Draw Arrow
                        const arrowLength = 6;
                        const angle = Math.atan2(end.y - start.y, end.x - start.x);
                        // Back off slightly from node so arrow tip is visible
                        const targetRadius = 5;
                        const tipX = end.x - targetRadius * Math.cos(angle);
                        const tipY = end.y - targetRadius * Math.sin(angle);

                        ctx.beginPath();
                        ctx.moveTo(tipX, tipY);
                        ctx.lineTo(tipX - arrowLength * Math.cos(angle - Math.PI / 6), tipY - arrowLength * Math.sin(angle - Math.PI / 6));
                        ctx.lineTo(tipX - arrowLength * Math.cos(angle + Math.PI / 6), tipY - arrowLength * Math.sin(angle + Math.PI / 6));
                        ctx.fillStyle = '#A5ABB6';
                        ctx.fill();

                        // Draw Label
                        const label = link.label_ko || link.label_en || link.relation_ko || link.relation_en; // Use available label
                        if (label) {
                            const midX = (start.x + end.x) / 2;
                            const midY = (start.y + end.y) / 2;
                            const fontSize = 10 / globalScale;
                            ctx.font = `${fontSize}px sans-serif`;
                            const textWidth = ctx.measureText(label).width;
                            const bckgDimensions = [textWidth, fontSize].map(n => n + fontSize * 0.2); // padding

                            // Background for text (optional but good for readability)
                            ctx.fillStyle = 'rgba(255, 255, 255, 0.8)';
                            ctx.fillRect(midX - bckgDimensions[0] / 2, midY - bckgDimensions[1] / 2, ...bckgDimensions);

                            ctx.textAlign = 'center';
                            ctx.textBaseline = 'middle';
                            ctx.fillStyle = '#666';
                            ctx.fillText(label, midX, midY);
                        }
                    }}
                />
            </div>
        </div>
    );
}
