/**
 * 프롬프트 텍스트에서 변수 추출
 * @param {string} content - 프롬프트 텍스트
 * @returns {string[]} - 추출된 변수 키 배열
 */
export const extractVariables = (content) => {
    const regex = /\{\{\s*([a-zA-Z0-9_\.]+)\s*\}\}/g;
    const matches = [];
    let match;

    while ((match = regex.exec(content)) !== null) {
        matches.push(match[1]);
    }

    return [...new Set(matches)];
};

/**
 * 변수 치환
 * @param {string} content - 프롬프트 텍스트
 * @param {Object} variables - 변수 값 객체
 * @returns {string} - 치환된 프롬프트
 */
export const resolveVariables = (content, variables) => {
    let resolved = content;

    Object.entries(variables).forEach(([key, value]) => {
        const regex = new RegExp(`\\{\\{\\s*${key}\\s*\\}\\}`, 'g');
        resolved = resolved.replace(regex, value);
    });

    return resolved;
};

/**
 * 변수 검증
 * @param {string} content - 프롬프트 텍스트
 * @param {Array} schema - 변수 스키마 배열
 * @returns {Object} - 검증 결과
 */
export const validateVariables = (content, schema) => {
    const usedVars = extractVariables(content);
    const definedVars = schema.map(s => s.key);

    const missing = usedVars.filter(v => !definedVars.includes(v));
    const unused = definedVars.filter(v => !usedVars.includes(v));

    return {
        valid: missing.length === 0,
        usedVars,
        definedVars,
        missing,
        unused
    };
};
