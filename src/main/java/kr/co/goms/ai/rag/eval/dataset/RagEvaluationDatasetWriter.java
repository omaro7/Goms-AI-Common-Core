/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */

package kr.co.goms.ai.rag.eval.dataset;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import kr.co.goms.ai.rag.eval.RagEvaluationCase;
import kr.co.goms.ai.rag.eval.RagEvaluationCaseType;

/**
 * RAG Golden Dataset을 JSON 형식으로 저장한다.
 * 
 * RagEvaluationDatasetWriter writer = new RagEvaluationDatasetWriter();
 * writer.write( dataset, Path.of("eval/lunchwork-seoul-v1.json"));
 * 
 * {
 *  "name": "lunchwork-seoul-v1",
 *   "projectId": "lunchwork_seoul",
 *   "projectName": "lunchwork_seoul",
 *  "cases": [
 *     {
 *       "id": "LUNCH-001",
 *   	 "projectId": "lunchwork_seoul",
 *       "question": "덕수궁 돌담길은 어떤 장소인가요?",
 *       "referenceAnswer": "덕수궁 돌담길은 서울 정동의 대표적인 산책길이다."
 *     }
 *   ]
 * }
 */
public final class RagEvaluationDatasetWriter {

    private final Gson gson;

    public RagEvaluationDatasetWriter() {
        this(new GsonBuilder()
                .setPrettyPrinting()
                .create());
    }

    public RagEvaluationDatasetWriter(Gson gson) {
        if (gson == null) {
            throw new NullPointerException("gson must not be null");
        }

        this.gson = gson;
    }

    public void write(RagEvaluationDataset dataset, Path path)
            throws IOException {

        if (dataset == null) {
            throw new NullPointerException("dataset must not be null");
        }

        if (path == null) {
            throw new NullPointerException("path must not be null");
        }

        Path parent = path.getParent();

        if (parent != null) {
            Files.createDirectories(parent);
        }

        try (Writer writer = Files.newBufferedWriter(
                path,
                StandardCharsets.UTF_8)) {

            write(dataset, writer);
        }
    }

    public void write(RagEvaluationDataset dataset, Writer writer) {
        if (dataset == null) {
            throw new NullPointerException("dataset must not be null");
        }

        if (writer == null) {
            throw new NullPointerException("writer must not be null");
        }

        DatasetJson datasetJson = convert(dataset);

        gson.toJson(datasetJson, writer);
    }

    private DatasetJson convert(RagEvaluationDataset dataset) {
        DatasetJson result = new DatasetJson();

        result.name = dataset.getName();
        result.projectId = dataset.getProjectId();
        result.projectName = dataset.getProjectName();
        result.cases = new ArrayList<>();

        for (RagEvaluationCase evaluationCase : dataset.getCases()) {
            CaseJson caseJson = new CaseJson();

            caseJson.id = evaluationCase.getId();
            caseJson.type = evaluationCase.getType();
            caseJson.question = evaluationCase.getQuestion();
            caseJson.referenceAnswer =
                    evaluationCase.getReferenceAnswer();

            result.cases.add(caseJson);
        }

        return result;
    }

    private static final class DatasetJson {

        private String name;
        private String projectId;
        private String projectName;
        private List<CaseJson> cases;
    }

    private static final class CaseJson {

        private String id;
        private RagEvaluationCaseType type;
        private String question;
        private String referenceAnswer;
    }
}