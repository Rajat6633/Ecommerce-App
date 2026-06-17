package com.ecommerce.common.events.contract;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.io.UncheckedIOException;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Shared helpers for the event-contract tests: snapshot loading, schema-lock
 * assertion, and producer&rarr;consumer round-trip assertion.
 */
final class Contracts {

    static final ObjectMapper MAPPER = ContractObjectMapper.create();

    private Contracts() {
    }

    /** Loads a committed expected-JSON snapshot from {@code src/test/resources/contracts/}. */
    static JsonNode loadSnapshot(String name) {
        String path = "/contracts/" + name + ".json";
        try (InputStream in = Contracts.class.getResourceAsStream(path)) {
            assertNotNull(in, "Missing contract snapshot resource: " + path);
            return MAPPER.readTree(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * SCHEMA LOCK. Serializes {@code payload} with the runtime-equivalent mapper and
     * asserts the JSON tree equals the committed snapshot exactly.
     *
     * <p>This compares the full structure: field names, the exact set of fields
     * (no extras, none missing), and each value's JSON type/representation
     * (string vs number, ISO-8601 instant vs epoch, etc.). A failure here means an
     * event contract changed.
     */
    static void assertSchemaLocked(String snapshotName, Object payload) {
        JsonNode actual;
        try {
            actual = MAPPER.readTree(MAPPER.writeValueAsString(payload));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        JsonNode expected = loadSnapshot(snapshotName);

        // Surface the precise mismatch (renamed/removed/added/retyped field) on failure.
        assertEquals(fieldShape(expected), fieldShape(actual),
                () -> "Event contract DRIFT in '" + snapshotName + "': the field set/types no "
                        + "longer match the committed snapshot.\nExpected: " + expected
                        + "\nActual:   " + actual);
        assertEquals(expected, actual,
                () -> "Event contract DRIFT in '" + snapshotName + "': serialized JSON differs "
                        + "from the committed snapshot.\nExpected: " + expected
                        + "\nActual:   " + actual);
    }

    /**
     * PRODUCER&rarr;CONSUMER ROUND-TRIP. Serialize on the producer side, deserialize
     * back into the record on the consumer side, assert structural + value equality.
     * Records implement value-based {@code equals}, so this proves both sides agree
     * on every field including nested records and collections.
     */
    static <T> void assertRoundTrip(T payload, Class<T> type) {
        try {
            String json = MAPPER.writeValueAsString(payload);
            T back = MAPPER.readValue(json, type);
            assertEquals(payload, back,
                    () -> "Round-trip mismatch for " + type.getSimpleName()
                            + ": producer-serialized JSON did not deserialize back equal on the "
                            + "consumer side. JSON was: " + json);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Builds a stable "shape" string: each leaf field path mapped to its JSON node
     * type. Renames, additions, removals, and type changes all alter this, giving a
     * crisp diff in the assertion message.
     */
    private static String fieldShape(JsonNode node) {
        StringBuilder sb = new StringBuilder();
        appendShape("", node, sb);
        return sb.toString();
    }

    private static void appendShape(String prefix, JsonNode node, StringBuilder sb) {
        if (node.isObject()) {
            // TreeMap-free but deterministic: iterate sorted field names.
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            java.util.TreeMap<String, JsonNode> sorted = new java.util.TreeMap<>();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> e = fields.next();
                sorted.put(e.getKey(), e.getValue());
            }
            for (Map.Entry<String, JsonNode> e : sorted.entrySet()) {
                appendShape(prefix.isEmpty() ? e.getKey() : prefix + "." + e.getKey(),
                        e.getValue(), sb);
            }
        } else if (node.isArray()) {
            if (node.isEmpty()) {
                sb.append(prefix).append("[]=ARRAY\n");
            } else {
                // Describe element shape from the first element (homogeneous arrays).
                appendShape(prefix + "[]", node.get(0), sb);
            }
        } else {
            sb.append(prefix).append('=').append(node.getNodeType()).append('\n');
        }
    }
}
