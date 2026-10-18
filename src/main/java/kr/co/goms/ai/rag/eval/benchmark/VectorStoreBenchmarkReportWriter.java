/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 *
 * Project: GomsBook AI
 * AI-powered EPUB authoring, validation, accessibility, and publishing automation.
 */
package kr.co.goms.ai.rag.eval.benchmark;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Vector Store Benchmark 결과를 JSON으로 저장합니다.
 */
public final class VectorStoreBenchmarkReportWriter {

    private final Gson gson;

    public VectorStoreBenchmarkReportWriter() {
        this(new GsonBuilder().setPrettyPrinting().create());
    }

    public VectorStoreBenchmarkReportWriter(Gson gson) {
        this.gson = Objects.requireNonNull(gson, "gson must not be null");
    }

    public void write(VectorStoreBenchmarkReport report, Path path) throws IOException {

        Objects.requireNonNull(report, "report must not be null");
        Objects.requireNonNull(path, "path must not be null");

        Path parent = path.getParent();

        if (parent != null) Files.createDirectories(parent);

        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            gson.toJson(report, writer);
        }
    }
}