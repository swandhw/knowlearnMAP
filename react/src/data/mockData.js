// Shared mock sources to reuse
const commonSources = [
    { id: 1, name: 'cls_biz_meta.md', type: 'markdown', icon: '📄', checked: true },
    { id: 2, name: 'cls_it_meta.md', type: 'markdown', icon: '📄', checked: true },
    { id: 3, name: 'cls_m_code.md', type: 'markdown', icon: '📄', checked: false },
    { id: 4, name: 'cls_m_dept.md', type: 'markdown', icon: '📄', checked: false },
    { id: 5, name: 'cls_m_emp.md', type: 'markdown', icon: '📄', checked: false },
    { id: 6, name: 'cls_t_project.md', type: 'markdown', icon: '📄', checked: false },
    { id: 7, name: 'cls_t_project_member.md', type: 'markdown', icon: '📄', checked: false },
    { id: 8, name: 'cls_t_project_partner_status.md', type: 'markdown', icon: '📄', checked: false },
    { id: 9, name: 'cls_t_project_resourc_status_norm.md', type: 'markdown', icon: '📄', checked: false },
];

export const mockNotebooks = [
    {
        id: 1,
        title: 'Untitled notebook',
        icon: '📄',
        source: '소스 0개',
        date: '2025. 12. 18.',
        role: 'Owner',
        color: 'yellow',
        sources: []
    },
    {
        id: 2,
        title: 'Business Metadata and Human Resources Data',
        icon: '👥',
        source: '소스 5개',
        date: '2025. 12. 11.',
        role: 'Owner',
        color: 'default',
        sources: commonSources.slice(0, 5)
    },
    {
        id: 3,
        title: '주기도문: 떠나하읺 농 화하, 거도의 완핚',
        icon: '🙏',
        source: '소스 1개',
        date: '2025. 12. 12.',
        role: 'Owner',
        color: 'blue',
        sources: commonSources.slice(0, 1)
    },
    {
        id: 4,
        title: '주기도문: 떠나하읺 농 화하, 거도의 완핚',
        icon: '🙏',
        source: '소스 1개',
        date: '2025. 12. 12.',
        role: 'Reader',
        color: 'blue',
        sources: commonSources.slice(0, 1)
    },
    {
        id: 5,
        title: 'IT Project and Resource Metadata Catalog',
        icon: '📊',
        source: '소스 10개',
        date: '2025. 12. 9.',
        role: 'Owner',
        color: 'purple',
        sources: commonSources
    }
];

export const addNotebook = (notebook) => {
    // Ensure new notebooks have an empty sources array if not provided
    if (!notebook.sources) {
        notebook.sources = [];
    }
    mockNotebooks.unshift(notebook);
};

export const getNotebook = (id) => {
    return mockNotebooks.find(n => n.id === Number(id));
};
