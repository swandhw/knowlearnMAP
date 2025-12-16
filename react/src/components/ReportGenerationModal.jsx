import { useEffect, useRef, useState } from 'react';
import './ReportGenerationModal.css';
import ReportCreationModal from './ReportCreationModal';

function ReportGenerationModal({ isOpen, onClose }) {
    const modalRef = useRef(null);
    const [isCreationModalOpen, setIsCreationModalOpen] = useState(false);
    const [selectedTemplate, setSelectedTemplate] = useState(null);

    // Close modal when clicking outside
    useEffect(() => {
        const handleClickOutside = (event) => {
            if (modalRef.current && !modalRef.current.contains(event.target)) {
                onClose();
            }
        };

        if (isOpen) {
            document.addEventListener('mousedown', handleClickOutside);
            document.body.style.overflow = 'hidden';
        }

        return () => {
            document.removeEventListener('mousedown', handleClickOutside);
            document.body.style.overflow = 'unset';
        };
    }, [isOpen, onClose]);

    // Close on Escape key
    useEffect(() => {
        const handleEscape = (event) => {
            if (event.key === 'Escape') {
                onClose();
            }
        };

        if (isOpen) {
            document.addEventListener('keydown', handleEscape);
        }

        return () => {
            document.removeEventListener('keydown', handleEscape);
        };
    }, [isOpen, onClose]);

    // Reset state when modal opens
    useEffect(() => {
        if (isOpen) {
            setIsCreationModalOpen(false);
            setSelectedTemplate(null);
        }
    }, [isOpen]);

    if (!isOpen) return null;

    const reportFormats = [
        {
            id: 1,
            category: '형식',
            items: [
                { id: 'custom', title: '직접 만들기', description: '주제, 스타일, 요즘 출력 지정하여 참여하는 방식으로 보고서를 작성하세요.', isCustom: true },
                { id: 'briefing', title: '브리핑 문서', description: '주요 인사이트와 인용문을 포함한 소스 개요', customizable: true },
                { id: 'study-guide', title: '학습 가이드', description: '단답형 퀴즈, 주요 예제인 질문, 학습 용어', customizable: true },
                { id: 'blog-post', title: '블로그 게시물', description: '원가 쉬운 기사 형식으로 요약한 유용한 정보', customizable: true },
            ]
        },
        {
            id: 2,
            category: '추천 형식',
            items: [
                { id: 'sermon', title: '설교 요약본', description: '주일 예배 설교의 핵심 내용을 요약하여 리더나 듣는 자료로 활용하기 위한 문서입니다.', customizable: true },
                { id: 'ministry-letter', title: '목회 서신', description: '성과 말씀을 바탕으로 교인들에게 신앙적 격려와 권면을 전하는 목회 서신입니다.', customizable: true },
                { id: 'concept-explanation', title: '핵심 개념 설명', description: '설교에 등장하는 주기도문의 의미나 중요성을 쉽고 대략적으로 설명하는 자료입니다.', customizable: true },
                { id: 'story-commentary', title: '이야기 형식 해설', description: '성과 속 비유의 인물이 중심으로 흥미롭게 풀어낸 이야기입니다.', customizable: true },
            ]
        }
    ];

    const handleCardClick = (format) => {
        // Close the format selection modal
        onClose();
    };

    const handleEditClick = (e, format) => {
        e.stopPropagation(); // Prevent card click

        // Open creation modal with template data
        setSelectedTemplate({
            title: format.title,
            description: format.description,
            defaultPrompt: getDefaultPrompt(format.id)
        });
        setIsCreationModalOpen(true);
    };

    const handleCustomClick = (format) => {
        // Open creation modal without template info
        setSelectedTemplate({
            placeholder: "예:\n\n새로운 웰니스 음료 출시를 위해 2026년 가능성 음료 시장에 관한 전문적인 경쟁 분석 리포트를 작성해 줘. 어조는 분석적이고 전문적이어야 하고, 주요 경쟁사와 유통 및 가격 책정에 중점을 두고 출시 전략을 수립해 줘."
        });
        setIsCreationModalOpen(true);
    };

    const getDefaultPrompt = (formatId) => {
        const prompts = {
            'briefing': 'You are a highly capable research assistant and tutor. Create a detailed briefing document with key insights and important quotes from the sources.',
            'study-guide': 'You are a highly capable research assistant and tutor. Create a detailed study guide designed to review understanding of the sources. Create a quiz with ten short-answer questions (2-3 sentences each) and include a separate answer key. Suggest five essay format questions, but do not supply answers. Also conclude with a comprehensive glossary of key terms with definitions.',
            'blog-post': 'Create an engaging blog post that summarizes the key information from the sources in an accessible article format.',
            'sermon': '주일 예배 설교의 핵심 내용을 요약하여 리더나 듣는 자료로 활용할 수 있는 설교 요약본을 작성해주세요.',
            'ministry-letter': '성경 말씀을 바탕으로 교인들에게 신앙적 격려와 권면을 전하는 목회 서신을 작성해주세요.',
            'concept-explanation': '설교에 등장하는 주요 개념의 의미나 중요성을 쉽고 명확하게 설명하는 자료를 만들어주세요.',
            'story-commentary': '성경 속 비유를 인물 중심으로 흥미롭게 풀어낸 이야기 형식의 해설을 작성해주세요.'
        };
        return prompts[formatId] || '';
    };

    const handleCreationModalClose = () => {
        setIsCreationModalOpen(false);
        setSelectedTemplate(null);
    };

    return (
        <div className="modal-overlay">
            <div className="modal-container report-modal" ref={modalRef}>
                <div className="modal-header">
                    <div className="modal-header-content">
                        <span className="modal-icon">📄</span>
                        <h2 className="modal-title">보고서 생성</h2>
                    </div>
                    <button className="modal-close-btn" onClick={onClose}>
                        <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
                            <path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z" />
                        </svg>
                    </button>
                </div>

                <div className="modal-body">
                    {reportFormats.map(section => (
                        <div key={section.id} className="format-section">
                            <h3 className="section-title">
                                {section.category === '추천 형식' && (
                                    <svg width="18" height="18" viewBox="0 0 18 18" fill="currentColor" style={{ marginRight: '6px' }}>
                                        <path d="M9 1l2.5 5.5L17 7.8l-4 3.9L14 17l-5-2.6L4 17l1-5.3-4-3.9 5.5-1.3z" />
                                    </svg>
                                )}
                                {section.category}
                            </h3>

                            <div className="format-grid">
                                {section.items.map(format => (
                                    <button
                                        key={format.id}
                                        className="format-card"
                                        onClick={() => format.isCustom ? handleCustomClick(format) : handleCardClick(format)}
                                    >
                                        <div className="format-header">
                                            <h4 className="format-title">{format.title}</h4>
                                            {format.customizable && (
                                                <button
                                                    className="edit-icon-btn"
                                                    onClick={(e) => handleEditClick(e, format)}
                                                    aria-label="수정"
                                                >
                                                    ✏️
                                                </button>
                                            )}
                                        </div>
                                        <p className="format-description">{format.description}</p>
                                    </button>
                                ))}
                            </div>
                        </div>
                    ))}
                </div>
            </div>

            {/* Report Creation Modal */}
            <ReportCreationModal
                isOpen={isCreationModalOpen}
                onClose={handleCreationModalClose}
                templateData={selectedTemplate}
            />
        </div>
    );
}

export default ReportGenerationModal;
