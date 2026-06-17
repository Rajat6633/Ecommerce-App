package com.ecommerce.product.infrastructure.config;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic offline {@link EmbeddingModel} for {@code local}/{@code test}.
 * Produces a small, stable vector from a character histogram so that identical
 * (or near-identical) product text embeds to near-identical vectors — enough for
 * the pgvector / in-memory store to exercise similarity logic without downloading
 * the Transformers ONNX model or calling any external API.
 *
 * <p>Mirrors the kyc-service stub. Spring AI 1.0.0-M1 represents embeddings as
 * {@code List<Double>}.
 */
public class StubEmbeddingModel implements EmbeddingModel {

    private static final int DIMS = 26;

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        List<Embedding> embeddings = new ArrayList<>();
        List<String> inputs = request.getInstructions();
        for (int i = 0; i < inputs.size(); i++) {
            embeddings.add(new Embedding(embed(inputs.get(i)), i));
        }
        return new EmbeddingResponse(embeddings);
    }

    @Override
    public List<Double> embed(String text) {
        String t = text == null ? "" : text.toLowerCase();
        double[] v = new double[DIMS];
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (c >= 'a' && c <= 'z') {
                v[c - 'a'] += 1.0;
            }
        }
        double norm = 0.0;
        for (double f : v) {
            norm += f * f;
        }
        norm = Math.sqrt(norm);
        List<Double> out = new ArrayList<>(DIMS);
        for (int i = 0; i < DIMS; i++) {
            out.add(norm > 0 ? v[i] / norm : 0.0);
        }
        return out;
    }

    @Override
    public List<Double> embed(Document document) {
        return embed(document.getContent());
    }

    @Override
    public int dimensions() {
        return DIMS;
    }
}
