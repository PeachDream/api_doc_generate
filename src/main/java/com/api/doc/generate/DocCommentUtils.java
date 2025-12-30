package com.api.doc.generate;

import com.intellij.psi.PsiElement;
import com.intellij.psi.javadoc.PsiDocComment;

import java.util.regex.Pattern;

final class DocCommentUtils {
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile(
            "</?(p|br|li|ul|ol|h[1-6]|div|span|code|pre|b|i|em|strong|tt|a|img|table|thead|tbody|tr|td|th|hr)(\\s+[^>]*)?>",
            Pattern.CASE_INSENSITIVE);

    private DocCommentUtils() {
    }

    static String extractSummary(PsiDocComment docComment) {
        if (docComment == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (PsiElement element : docComment.getDescriptionElements()) {
            String text = element.getText();
            if (text == null || text.isEmpty()) {
                continue;
            }
            String cleaned = HTML_TAG_PATTERN.matcher(text).replaceAll("");
            cleaned = cleaned.replace("\r", " ").replace("\n", " ").trim();
            if (cleaned.isEmpty() || cleaned.startsWith("@")) {
                continue;
            }
            sb.append(cleaned).append(" ");
        }

        String summary = sb.toString().trim();
        if (summary.isEmpty()) {
            return "";
        }
        return summary.replaceAll("\\s+", " ");
    }
}
