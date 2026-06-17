package com.ecommerce.kyc.infrastructure.ai;

import com.ecommerce.kyc.application.port.out.DocumentExtractionPort.ExtractedDocument;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Lenient parser for the model's JSON reply. Tolerates code-fence wrapping and
 * missing fields; any parse failure yields a low-confidence result (fail closed).
 */
class DocumentJsonParser {

    private final ObjectMapper mapper = new ObjectMapper();

    ExtractedDocument parse(String content) {
        if (content == null || content.isBlank()) {
            return ExtractedDocument.lowConfidence();
        }
        String json = stripFences(content);
        try {
            JsonNode n = mapper.readTree(json);
            String name = text(n, "fullName");
            boolean confident = name != null && !name.isBlank();
            return new ExtractedDocument(
                    name, text(n, "documentNumber"), text(n, "dateOfBirth"),
                    text(n, "expiry"), text(n, "nationality"), confident);
        } catch (Exception e) {
            return ExtractedDocument.lowConfidence();
        }
    }

    private static String text(JsonNode n, String field) {
        JsonNode v = n.get(field);
        return v == null || v.isNull() ? null : v.asText();
    }

    private static String stripFences(String s) {
        String t = s.trim();
        if (t.startsWith("```")) {
            int firstNl = t.indexOf('\n');
            if (firstNl > 0) {
                t = t.substring(firstNl + 1);
            }
            if (t.endsWith("```")) {
                t = t.substring(0, t.length() - 3);
            }
        }
        return t.trim();
    }
}
