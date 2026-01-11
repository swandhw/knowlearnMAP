import React, { useState, useEffect, useRef } from 'react';
import ForceGraph2D from 'react-force-graph-2d';
import { Maximize2, Network } from 'lucide-react';
import './MiniKnowledgeGraph.css';

const MiniKnowledgeGraph = ({ nodes, links, onExpand }) => {
    const graphRef = useRef();
    const containerRef = useRef(null);
    const [dimensions, setDimensions] = useState({ width: 300, height: 300 });

    useEffect(() => {
        const updateDimensions = () => {
            if (containerRef.current) {
                setDimensions({
                    width: containerRef.current.clientWidth,
                    height: containerRef.current.clientHeight
                });
            }
        };

        const timer = setTimeout(updateDimensions, 100);
        window.addEventListener('resize', updateDimensions);
        return () => {
            clearTimeout(timer);
            window.removeEventListener('resize', updateDimensions);
        };
    }, []);

    useEffect(() => {
        if (graphRef.current) {
            graphRef.current.d3Force('charge').strength(-50); // Less repulsion for mini graph
            graphRef.current.d3Force('link').distance(40); // Shorter links
            setTimeout(() => graphRef.current.zoomToFit(400, 20), 500);
        }
    }, [nodes, links]);

    const graphData = { nodes: [...nodes], links: [...links] };

    return (
        <div className="mini-kg-container" ref={containerRef}>
            <div className="mini-kg-header">
                <div className="mini-kg-title">
                    <Network size={14} />
                    <span>지식 그래프 ({nodes.length})</span>
                </div>
                <button className="mini-kg-expand-btn" onClick={onExpand} title="크게 보기">
                    <Maximize2 size={14} />
                </button>
            </div>

            <div className="mini-kg-canvas">
                <ForceGraph2D
                    ref={graphRef}
                    width={dimensions.width}
                    height={dimensions.height}
                    graphData={graphData}
                    nodeLabel="name"
                    nodeAutoColorBy="group"
                    backgroundColor="#ffffff"
                    enableZoom={true} // Allow zooming even in mini view
                    enablePanInteraction={true}
                    nodeCanvasObject={(node, ctx, globalScale) => {
                        const label = node.name;
                        const fontSize = 12 / globalScale;
                        ctx.font = `${fontSize}px "Pretendard Variable", sans-serif`;

                        ctx.beginPath();
                        ctx.arc(node.x, node.y, 4, 0, 2 * Math.PI, false);
                        ctx.fillStyle = node.color || '#fac858';
                        ctx.fill();

                        ctx.textAlign = 'center';
                        ctx.textBaseline = 'middle';
                        ctx.fillStyle = '#333';
                        ctx.fillText(label, node.x, node.y + 6);
                    }}
                    linkCanvasObject={(link, ctx, globalScale) => {
                        const start = link.source;
                        const end = link.target;
                        if (typeof start !== 'object' || typeof end !== 'object') return; // Wait for D3 to link objects

                        ctx.beginPath();
                        ctx.moveTo(start.x, start.y);
                        ctx.lineTo(end.x, end.y);
                        ctx.strokeStyle = '#ccc';
                        ctx.lineWidth = 1 / globalScale;
                        ctx.stroke();

                        // Optional: Simplified arrow
                    }}
                />
            </div>
            {nodes.length === 0 && (
                <div className="mini-kg-empty">
                    검색 결과가 없습니다
                </div>
            )}
        </div>
    );
};

export default MiniKnowledgeGraph;
