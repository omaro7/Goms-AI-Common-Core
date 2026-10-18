/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */

package kr.co.goms.ai.rag.eval.dataset;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import kr.co.goms.ai.rag.eval.RagEvaluationCase;

/**
 * RAG 평가용 Golden Dataset.
 * 
 * RagEvaluationDataset
 *     ├─ name
 *     │
 *     └─ List<RagEvaluationCase>
 *           ├─ LUNCH-001
 *           ├─ LUNCH-002
 *           └─ ...
 *           
 * List<RagEvaluationCase> cases = List.of(
 *         new RagEvaluationCase(
 *                 "LUNCH-001",
 *                 "덕수궁 돌담길은 어떤 장소인가요?",
 *                 "덕수궁 돌담길은 서울 정동의 대표적인 산책길이다."),
 * 
 *         new RagEvaluationCase(
 *                 "LUNCH-002",
 *                 "서울시립미술관은 어떤 공간으로 소개되나요?",
 *                 "점심시간에 예술을 접할 수 있는 공간으로 소개된다.")
 * );
 * 
 * RagEvaluationDataset dataset = new RagEvaluationDataset( "lunchwork-seoul-v1", "f1b415f62fe7243637477e7de75f2011b6012fdac9d5c56a2317358be058831c", "lunchwork-seoul", cases);
 *                 
 */
public final class RagEvaluationDataset {

    private final String name;
    private final String projectId;
    private final String projectName;
    private final List<RagEvaluationCase> cases;

    public RagEvaluationDataset(String name,  String projectId, String projectName, List<RagEvaluationCase> cases) {
        this.name = requireText(name, "name");
        this.projectId = requireText(projectId, "projectId");
        this.projectName = requireText(projectName, "projectName");
        this.cases = normalizeCases(cases);
    }

    public String getName() {
        return name;
    }

    public String getProjectId() {
        return projectId;
    }
    
    public String getProjectName() {
        return projectName;
    }
    
    public List<RagEvaluationCase> getCases() {
        return cases;
    }

    public int size() {
        return cases.size();
    }

    public boolean isEmpty() {
        return cases.isEmpty();
    }

    public RagEvaluationCase getCase(String id) {
        if (id == null || id.trim().isEmpty()) {
            return null;
        }

        for (RagEvaluationCase evaluationCase : cases) {
            if (id.equals(evaluationCase.getId())) {
                return evaluationCase;
            }
        }

        return null;
    }

    private static List<RagEvaluationCase> normalizeCases(
            List<RagEvaluationCase> cases) {

        if (cases == null || cases.isEmpty()) {
            return Collections.emptyList();
        }

        List<RagEvaluationCase> normalized = new ArrayList<>();

        for (RagEvaluationCase evaluationCase : cases) {
            if (evaluationCase != null) {
                normalized.add(evaluationCase);
            }
        }

        return Collections.unmodifiableList(normalized);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null) {
            throw new NullPointerException(fieldName + " must not be null");
        }

        String normalized = value.trim();

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(
                    fieldName + " must not be blank");
        }

        return normalized;
    }
    
    @Override
    public String toString() {
        return "RagEvaluationDataset{" +
                "name='" + name + '\'' +
                "projectId='" + projectId + '\'' +
                "projectName='" + projectName + '\'' +
                ", size=" + cases.size() +
                '}';
    }

}