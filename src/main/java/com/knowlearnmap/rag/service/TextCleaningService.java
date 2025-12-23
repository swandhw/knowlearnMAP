package com.knowlearnmap.rag.service;

import org.springframework.stereotype.Service;

@Service
public class TextCleaningService {

    /**
     * PDF?ì„œ ì¶”ì¶œ???ìŠ¤?¸ë? ?•ë¦¬?©ë‹ˆ??
     * - ë¶ˆí•„?”í•œ ì¤„ë°”ê¿??œê±° (?¨ì–´/ë¬¸ì¥ ì¤‘ê°„???Šê¸´ ê²½ìš°)
     * - ?œê? ë¬¸ì¥ ë³‘í•©
     * - ê³µë°± ?•ê·œ??
     */
    public String clean(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        // 1. ?œì–´ ë¬¸ì ?œê±° (ì¤„ë°”ê¿? ??? ? ì?)
        String cleaned = text.replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", "");

        // 2. ?°ì†??ê³µë°±???˜ë‚˜ë¡?(???¬í•¨)
        cleaned = cleaned.replaceAll("[ \\t]+", " ");

        // 3. ì¤„ë°”ê¿??•ê·œ??(\r\n ??\n)
        cleaned = cleaned.replaceAll("\\r\\n", "\n");
        cleaned = cleaned.replaceAll("\\r", "\n");

        // 4. ë¶ˆí•„?”í•œ ì¤„ë°”ê¿??œê±° (?µì‹¬ ë¡œì§)
        StringBuilder sb = new StringBuilder();
        String[] lines = cleaned.split("\n");

        for (int i = 0; i < lines.length; i++) {
            String currentLine = lines[i].trim();

            // ë¹?ì¤„ì? ?¤í‚µ
            if (currentLine.isEmpty()) {
                continue;
            }

            // ?„ì¬ ì¤„ì„ ì¶”ê?
            sb.append(currentLine);

            // ?¤ìŒ ì¤„ì´ ?ˆëŠ”ì§€ ?•ì¸
            if (i < lines.length - 1) {
                String nextLine = lines[i + 1].trim();

                // ?¤ìŒ ì¤„ì´ ë¹„ì–´?ˆìœ¼ë©?ë¬¸ë‹¨ êµ¬ë¶„ ??ì¤„ë°”ê¿?2ê°?
                if (nextLine.isEmpty()) {
                    sb.append("\n\n");
                    continue;
                }

                // ?„ì¬ ì¤„ì´ ë¬¸ì¥ ì¢…ê²° ë¶€?¸ë¡œ ?ë‚˜?”ì? ?•ì¸
                boolean endsWithPunctuation = currentLine.matches(".*[.!????)]$");

                // ?¤ìŒ ì¤„ì´ ?ˆë¡œ??ë¬¸ì¥/?¹ì…˜ ?œì‘?¸ì? ?•ì¸
                boolean nextStartsNewSection = nextLine.matches("^[0-9]+[\\.\\)].*") || // ë²ˆí˜¸ ë§¤ê?
                        nextLine.matches("^[ê°€-?£A-Z]{2,}:.*") || // ?œëª©:
                        nextLine.matches("^[?â—‹? â–¡?ªâ–«-].*"); // ë¦¬ìŠ¤??

                if (endsWithPunctuation || nextStartsNewSection) {
                    // ë¬¸ì¥???„ê²°?˜ì—ˆê±°ë‚˜ ???¹ì…˜ ?œì‘ ??ì¤„ë°”ê¿?
                    sb.append("\n");
                } else {
                    // ë¬¸ì¥ ì¤‘ê°„???Šê¸´ ê²½ìš° ??ê³µë°±?¼ë¡œ ?°ê²°
                    // ?? "ì¹¨ìœ¤\n?? ??"ì¹¨ìœ¤ ??
                    sb.append(" ");
                }
            }
        }

        // 5. ?°ì†??ì¤„ë°”ê¿ˆì„ ìµœë? 2ê°œë¡œ ?œí•œ
        String result = sb.toString();
        result = result.replaceAll("\n{3,}", "\n\n");

        // 6. ?ë’¤ ê³µë°± ?œê±°
        return result.trim();
    }
}
