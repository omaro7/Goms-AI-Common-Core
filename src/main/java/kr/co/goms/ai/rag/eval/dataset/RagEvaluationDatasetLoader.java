/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */

package kr.co.goms.ai.rag.eval.dataset;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import kr.co.goms.ai.rag.eval.RagEvaluationCase;
import kr.co.goms.ai.rag.eval.RagEvaluationCaseType;
import kr.co.goms.ai.rag.util.RagUtil;

/**
 * JSON 파일에서 RAG Golden Dataset을 로드한다.
 *
 * {
 *   "name": "lunchwork-seoul-v1",
 *   "projectId": "lunchwork_seoul",
 *   "projectName": "lunchwork_seoul",
 *   "cases": [
 *     {
 *       "id": "LUNCH-001",
 *       "type": "ANSWERABLE",
 *       "question": "덕수궁 돌담길은 어떤 장소인가요?",
 *       "referenceAnswer": "덕수궁 돌담길은 서울 정동의 대표적인 산책길이다.",
 *       "expectedDocuments": [
 *         "OEBPS/Text/chapter10_4.xhtml"
 *       ]
 *     },
 *     {
 *       "id": "LUNCH-002",
 *       "type": "ANSWERABLE",
 *       "question": "서울시립미술관은 어떤 공간으로 소개되나요?",
 *       "referenceAnswer": "점심시간에 예술을 접할 수 있는 공간으로 소개된다.",
 *       "expectedDocuments": [
 *         "OEBPS/Text/chapter10_2.xhtml"
 *       ]
 *     }
 *   ]
 * }
 *
 * RagEvaluationDatasetLoader loader = new RagEvaluationDatasetLoader();
 *
 * RagEvaluationDataset dataset = loader.load(Path.of("eval/lunchwork-seoul-v1.json"));
 */
public final class RagEvaluationDatasetLoader {

	private final Gson gson;

	public RagEvaluationDatasetLoader() {
		this(new Gson());
	}

	public RagEvaluationDatasetLoader(Gson gson) {

		if (gson == null) throw new NullPointerException("gson must not be null");

		this.gson = gson;
	}

	public RagEvaluationDataset load(Path path) throws IOException {

		if (path == null) throw new NullPointerException("path must not be null");

		if (!Files.exists(path)) throw new IOException("Dataset file does not exist: " + path);

		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			return load(reader);
		}
	}

	public RagEvaluationDataset load(Reader reader) {

		if (reader == null) throw new NullPointerException("reader must not be null");

		try {

			DatasetJson datasetJson = gson.fromJson(reader, DatasetJson.class);

			if (datasetJson == null) throw new IllegalArgumentException("Dataset JSON must not be empty");

			String name = RagUtil.requireText(datasetJson.name, "name");
			String projectId = RagUtil.requireProjectId(datasetJson.projectId);
			String projectName = RagUtil.requireText(datasetJson.projectName, "projectName");
			List<RagEvaluationCase> cases = convertCases(datasetJson.cases);

			return new RagEvaluationDataset(name, projectId, projectName, cases);

		} catch (JsonParseException e) {

			throw new IllegalArgumentException("Failed to parse RAG evaluation dataset JSON", e);
		}
	}

	private List<RagEvaluationCase> convertCases(List<CaseJson> caseJsonList) {

		if (caseJsonList == null || caseJsonList.isEmpty()) return Collections.emptyList();

		List<RagEvaluationCase> cases = new ArrayList<>();

		for (CaseJson caseJson : caseJsonList) {

			if (caseJson == null) continue;

			RagEvaluationCaseType type = caseJson.type != null ? caseJson.type : RagEvaluationCaseType.ANSWERABLE;
			List<String> expectedDocuments = resolveExpectedDocuments(caseJson);

			cases.add(new RagEvaluationCase(caseJson.id, type, caseJson.question, caseJson.referenceAnswer, expectedDocuments));
		}

		return Collections.unmodifiableList(cases);
	}

	private List<String> resolveExpectedDocuments(CaseJson caseJson) {

		if (caseJson.expectedDocuments != null && !caseJson.expectedDocuments.isEmpty()) return RagUtil.normalizeExpectedDocuments(caseJson.expectedDocuments);

		return convertSourceHints(caseJson.sourceHints);
	}

	private List<String> convertSourceHints(List<String> sourceHints) {

		if (sourceHints == null || sourceHints.isEmpty()) return Collections.emptyList();

		List<String> expectedDocuments = new ArrayList<>();

		for (String sourceHint : sourceHints) {

			String documentPath = toDocumentPath(sourceHint);

			if (documentPath == null || RagUtil.isExcludedDocument(documentPath) || expectedDocuments.contains(documentPath)) continue;

			expectedDocuments.add(documentPath);
		}

		return Collections.unmodifiableList(expectedDocuments);
	}

	private String toDocumentPath(String sourceHint) {

		String normalized = RagUtil.normalizeDocumentPath(sourceHint);

		if (normalized == null) return null;

		int fragmentIndex = normalized.indexOf('#');

		return fragmentIndex >= 0 ? normalized.substring(0, fragmentIndex) : normalized;
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
		private List<String> sourceHints;
		private List<String> expectedDocuments;
	}
}