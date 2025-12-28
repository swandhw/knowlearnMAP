package com.knowlearnmap.rag.service;

import org.springframework.stereotype.Service;

@Service
public class TextCleaningService {

    public String cleanText(String text) {
        if (text == null) {
            return "";
        }

        // 1. NULL 문자 제거
        String clean = text.replace("\0", "");

        // 2. 윈도우 줄바꿈을 유닉스 스타일로 통일
        clean = clean.replace("\r\n", "\n").replace("\r", "\n");

        // 3. 연속된 공백을 하나로 (줄바꿈 제외)
        clean = clean.replaceAll("[ \\t\\f\\v]+", " ");

        // 4. 불필요한 줄바꿈 제거 (문장이 이어지는 경우)
        // 한글/영문 뒤에 줄바꿈이 있고, 다음 줄이 소문자거나 한글이면 이어붙이기 시도
        // 단, 목록이나 제목 같은 경우는 제외해야 함.

        StringBuilder sb = new StringBuilder();
        String[] lines = clean.split("\n");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty())
                continue;

            sb.append(line);

            // 마지막 줄이 아니면 다음 줄 확인
            if (i < lines.length - 1) {
                String nextLine = lines[i + 1].trim();
                boolean endsWithPunctuation = line.matches(".*[\\.\\?!]$"); // 문장 종료 부호로 끝나는가?

                // 다음 줄이 새로운 섹션/목록의 시작인지 확인
                boolean nextStartsNewSection = nextLine.matches("^[0-9]+[\\.\\)].*") || // 번호 매기기 (1. 1))
                        nextLine.matches("^[가-힣a-zA-Z]{2,}:.*") || // 제목: 등의 패턴
                        nextLine.matches("^[\\-].*"); // - 리스트

                if (endsWithPunctuation || nextStartsNewSection) {
                    // 문장이 끝났거나, 새로운 섹션이면 줄바꿈 유지
                    sb.append("\n");
                } else {
                    // 문장이 중간에 끊긴 것으로 판단되면 공백으로 연결
                    sb.append(" ");
                }
            }
        }

        // 5. 연속된 줄바꿈을 최대 2개로 제한
        String result = sb.toString();
        result = result.replaceAll("\n{3,}", "\n\n");

        // 6. 앞뒤 공백 제거
        return result.trim();
    }
}
