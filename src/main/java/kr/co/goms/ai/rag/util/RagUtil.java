/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.rag.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HexFormat;
import java.util.List;

public final class RagUtil {

	private static final String EXCLUDED_QUIZ_DOCUMENT = "quiz.xhtml";

	private RagUtil() {
	}

	public static boolean isExcludedDocument(String documentPath) {

		String normalizedPath = normalizeDocumentPath(documentPath);

		if (normalizedPath == null || normalizedPath.isBlank()) return false;

		int index = normalizedPath.lastIndexOf('/');
		String fileName = index >= 0 ? normalizedPath.substring(index + 1) : normalizedPath;

		return EXCLUDED_QUIZ_DOCUMENT.equalsIgnoreCase(fileName);
	}

	public static String sha256(String value) {

		String normalized = value == null ? "" : value;

		try {

			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(normalized.getBytes(StandardCharsets.UTF_8));

			return HexFormat.of().formatHex(hash);

		} catch (NoSuchAlgorithmException e) {

			throw new IllegalStateException("SHA-256 algorithm is not available.", e);
		}
	}

	public static String normalizeDocumentPath(String value) {

		if (value == null || value.isBlank()) return null;

		return value.trim().replace('\\', '/');
	}

	public static String requireText(String value, String fieldName) {

		if (value == null) throw new NullPointerException(fieldName + " must not be null");

		String normalized = value.trim();

		if (normalized.isEmpty()) throw new IllegalArgumentException(fieldName + " must not be blank");

		return normalized;
	}

	public static String normalizeOptional(String value) {

		if (value == null) return null;

		String normalized = value.trim();

		return normalized.isEmpty() ? null : normalized;
	}

	public static List<String> normalizeExpectedDocuments(List<String> expectedDocuments) {

		if (expectedDocuments == null || expectedDocuments.isEmpty()) return Collections.emptyList();

		List<String> normalized = new ArrayList<>();

		for (String expectedDocument : expectedDocuments) {

			String value = normalizeDocumentPath(expectedDocument);

			if (value == null || isExcludedDocument(value) || normalized.contains(value)) continue;

			normalized.add(value);
		}

		return Collections.unmodifiableList(normalized);
	}

	public static String requireProjectId(String projectId) {

		if (projectId == null || projectId.isBlank()) throw new IllegalArgumentException("projectId must not be blank.");

		return projectId.trim();
	}
}