/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.rag.retrieval;

import java.util.Objects;

/**
 * Retriever가 반환하는 검색 결과 모델, Context Expansion까지 고려
 * Represents a document chunk returned by the RAG retrieval pipeline.
 *
 * <p>
 * A {@code RetrievedDocument} can represent either:
 * </p>
 *
 * <ul>
 *     <li>a chunk directly retrieved by vector search / reranking</li>
 *     <li>a neighboring chunk added through context expansion</li>
 * </ul>
 *
 * <p>
 * For directly retrieved chunks:
 * </p>
 *
 * <pre>
 * retrievalScore       != null
 * parentRetrievalScore == null
 * expanded             == false
 * parentChunkId        == null
 * </pre>
 *
 * <p>
 * For context-expanded chunks:
 * </p>
 *
 * <pre>
 * retrievalScore       == null
 * parentRetrievalScore != null
 * expanded             == true
 * parentChunkId        != null
 * </pre>
 */
public final class RetrievedDocument {

    private final String projectId;

    private final String chunkId;

    private final String sourcePath;

    private final int sequence;

    private final String heading;

    private final String text;

    /*
     * Score assigned directly to this chunk
     * by the retrieval pipeline.
     *
     * null means that this chunk was not
     * directly retrieved.
     */
    private final Double retrievalScore;

    /*
     * Retrieval score of the parent chunk
     * that caused this chunk to be included
     * through context expansion.
     *
     * null for directly retrieved chunks.
     */
    private final Double parentRetrievalScore;

    /*
     * true when this chunk was added
     * through context expansion.
     */
    private final boolean expanded;

    /*
     * Chunk ID of the directly retrieved
     * parent chunk.
     *
     * null for directly retrieved chunks.
     */
    private final String parentChunkId;


    /**
     * Creates a directly retrieved document.
     *
     * @param projectId      project identifier
     * @param chunkId        unique chunk identifier
     * @param sourcePath     source document path
     * @param sequence       chunk position within source
     * @param heading        heading or section title
     * @param text           chunk text
     * @param retrievalScore retrieval score assigned to this chunk
     */
    public RetrievedDocument(String projectId, String chunkId, String sourcePath, int sequence, String heading, String text, Double retrievalScore) {
        this(projectId, chunkId, sourcePath, sequence, heading, text, retrievalScore, null, false, null);
    }

    /**
     * Creates a retrieved or context-expanded document.
     *
     * @param projectId            project identifier
     * @param chunkId              unique chunk identifier
     * @param source               source document path
     * @param sequence             chunk position within source
     * @param heading              heading or section title
     * @param text                 chunk text
     * @param retrievalScore       direct retrieval score
     * @param parentRetrievalScore score of the parent retrieved chunk
     * @param expanded             whether this chunk was context-expanded
     * @param parentChunkId        parent chunk identifier
     */
    public RetrievedDocument(
            String projectId,
            String chunkId,
            String sourcePath,
            int sequence,
            String heading,
            String text,
            Double retrievalScore,
            Double parentRetrievalScore,
            boolean expanded,
            String parentChunkId) {

    	this.projectId = requireText(projectId, "projectId");
    	this.chunkId = requireText(chunkId, "chunkId");
    	this.sourcePath = requireText(sourcePath, "sourcePath");

        if (sequence < 0) {
            throw new IllegalArgumentException(
                    "sequence must be >= 0");
        }

        this.sequence = sequence;

        this.heading = heading;

        this.text = Objects.requireNonNull( text, "text");

        validateExpansionState(
                retrievalScore,
                parentRetrievalScore,
                expanded,
                parentChunkId
        );

        this.retrievalScore = retrievalScore;

        this.parentRetrievalScore = parentRetrievalScore;

        this.expanded = expanded;

        this.parentChunkId = parentChunkId;
    }


    public String getProjectId() {

        return projectId;
    }


    public String getChunkId() {

        return chunkId;
    }


    public String getSourcePath() {

        return sourcePath;
    }


    public int getSequence() {

        return sequence;
    }


    public String getHeading() {

        return heading;
    }


    public String getText() {

        return text;
    }


    public Double getRetrievalScore() {

        return retrievalScore;
    }


    public Double getParentRetrievalScore() {

        return parentRetrievalScore;
    }


    public boolean isExpanded() {

        return expanded;
    }


    public String getParentChunkId() {

        return parentChunkId;
    }


    /**
     * Returns whether this chunk was directly retrieved.
     *
     * @return true when the chunk belongs to the original retrieval result
     */
    public boolean isDirectlyRetrieved() {

        return !expanded;
    }


    /**
     * Creates a context-expanded document based on this chunk.
     *
     * @param chunkId    expanded chunk identifier
     * @param sourcePath expanded chunk source path
     * @param sequence   expanded chunk sequence
     * @param heading    expanded chunk heading
     * @param text       expanded chunk text
     *
     * @return context-expanded document
     */
    public RetrievedDocument createExpandedDocument(String chunkId, String sourcePath, int sequence, String heading, String text) {

        if (expanded) {
            throw new IllegalStateException("An expanded document cannot be used as the parent of another expansion.");
        }

        return new RetrievedDocument(
            projectId,
            chunkId,
            sourcePath,
            sequence,
            heading,
            text,
            null,
            retrievalScore,
            true,
            this.chunkId
        );
    }

    private static void validateExpansionState(
            Double retrievalScore,
            Double parentRetrievalScore,
            boolean expanded,
            String parentChunkId) {

        if (expanded) {

            if (retrievalScore != null) {

                throw new IllegalArgumentException(
                        "Expanded document must not have "
                                + "a direct retrievalScore.");
            }

            if (parentRetrievalScore == null) {

                throw new IllegalArgumentException(
                        "Expanded document requires "
                                + "parentRetrievalScore.");
            }

            if (isBlank(parentChunkId)) {

                throw new IllegalArgumentException(
                        "Expanded document requires "
                                + "parentChunkId.");
            }

            return;
        }


        if (retrievalScore == null) {

            throw new IllegalArgumentException(
                    "Directly retrieved document requires "
                            + "retrievalScore.");
        }

        if (parentRetrievalScore != null) {

            throw new IllegalArgumentException(
                    "Directly retrieved document must not have "
                            + "parentRetrievalScore.");
        }

        if (!isBlank(parentChunkId)) {

            throw new IllegalArgumentException(
                    "Directly retrieved document must not have "
                            + "parentChunkId.");
        }
    }


    private static String requireText(String value, String name) {

        if (isBlank(value)) {
            throw new IllegalArgumentException(name + " must not be blank");
        }

        return value.trim();
    }

    private static boolean isBlank( String value) {

        return value == null || value.trim().isEmpty();
    }


    @Override
    public String toString() {

        return "RetrievedDocument{"
                + "projectId='" + projectId + '\''
                + ", chunkId='" + chunkId + '\''
                + ", sourcePath='" + sourcePath + '\''
                + ", sequence=" + sequence
                + ", heading='" + heading + '\''
                + ", retrievalScore=" + retrievalScore
                + ", parentRetrievalScore="
                + parentRetrievalScore
                + ", expanded=" + expanded
                + ", parentChunkId='" + parentChunkId + '\''
                + '}';
    }
}