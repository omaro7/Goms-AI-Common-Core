/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.rag.context;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import kr.co.goms.ai.rag.retrieval.RetrievalResult;
import kr.co.goms.ai.rag.vector.VectorSearchResult;

/**
 * RetrievalResult를 LLM 프롬프트에 사용할 {@link RagContext}로 변환합니다.
 *
 * <p>컨텍스트 최대 길이를 적용할 때 문자열 중간을 자르지 않고
 * 출처 단위로 포함 여부를 결정합니다.</p>
 *
 * <pre>
 * RetrievalResult
 *      ↓
 * RagContextBuilder
 *      ├─ 출처 블록 생성
 *      ├─ 최대 길이 검사
 *      ├─ 출처 단위 선택
 *      └─ RagContext 생성
 * </pre>
 */
public final class RagContextBuilder {

    /**
     * 컨텍스트 문자 수 제한을 적용하지 않는 값입니다.
     */
    public static final int UNLIMITED_CHARACTERS = 0;

    /**
     * 기본 컨텍스트 최대 문자 수입니다.
     */
    public static final int DEFAULT_MAXIMUM_CONTEXT_CHARACTERS =
        18_000;

    /**
     * 컨텍스트가 일부 생략되었을 때 추가되는 안내문입니다.
     */
    public static final String DEFAULT_TRUNCATION_NOTICE =
        "[안내] 최대 컨텍스트 길이로 인해 일부 검색 결과가 생략되었습니다.";

    private final int maximumContextCharacters;
    private final boolean includeScore;
    private final boolean includeRank;
    private final boolean includeTitle;
    private final boolean includeElementId;
    private final boolean includeChunkType;
    private final boolean includeEpubType;
    private final boolean includeLanguage;
    private final boolean includeMetadata;
    private final boolean includeTruncationNotice;
    private final String truncationNotice;

    /**
     * 기본 설정으로 생성합니다.
     */
    public RagContextBuilder() {
        this(
            DEFAULT_MAXIMUM_CONTEXT_CHARACTERS,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            false,
            true,
            DEFAULT_TRUNCATION_NOTICE
        );
    }

    /**
     * 컨텍스트 최대 문자 수만 지정합니다.
     *
     * @param maximumContextCharacters 최대 문자 수. 0이면 제한 없음
     */
    public RagContextBuilder(
        int maximumContextCharacters
    ) {
        this(
            maximumContextCharacters,
            true,
            true,
            true,
            true,
            true,
            true,
            true,
            false,
            true,
            DEFAULT_TRUNCATION_NOTICE
        );
    }

    public RagContextBuilder(
        int maximumContextCharacters,
        boolean includeScore,
        boolean includeRank,
        boolean includeTitle,
        boolean includeElementId,
        boolean includeChunkType,
        boolean includeEpubType,
        boolean includeLanguage,
        boolean includeMetadata,
        boolean includeTruncationNotice,
        String truncationNotice
    ) {
        this.maximumContextCharacters =
            validateMaximumCharacters(
                maximumContextCharacters
            );

        this.includeScore = includeScore;
        this.includeRank = includeRank;
        this.includeTitle = includeTitle;
        this.includeElementId = includeElementId;
        this.includeChunkType = includeChunkType;
        this.includeEpubType = includeEpubType;
        this.includeLanguage = includeLanguage;
        this.includeMetadata = includeMetadata;
        this.includeTruncationNotice =
            includeTruncationNotice;

        this.truncationNotice =
            normalizeMultiline(
                truncationNotice
            );
    }

    /**
     * RetrievalResult를 RagContext로 변환합니다.
     *
     * @param retrievalResult 검색 결과
     * @return 생성된 RAG 컨텍스트
     */
    public RagContext build(
        RetrievalResult retrievalResult
    ) {
        Objects.requireNonNull(
            retrievalResult,
            "retrievalResult must not be null"
        );

        List<VectorSearchResult> searchResults =
            retrievalResult.getSearchResults();

        if (searchResults == null
            || searchResults.isEmpty()) {

            return createEmptyContext(
                retrievalResult
            );
        }

        List<PreparedSource> preparedSources =
            prepareSources(searchResults);

        int originalCharacterCount =
            calculateOriginalCharacterCount(
                preparedSources
            );

        SelectionResult selection =
            selectSources(preparedSources);

        Map<String, String> metadata =
            createContextMetadata(
                preparedSources.size(),
                selection
            );

        return RagContext.builder()
            .query(retrievalResult.getQuery())
            .contextText(selection.contextText)
            .sources(selection.sources)
            .embeddingModel(
                retrievalResult.getModel()
            )
            .retrievalDurationNanos(
                retrievalResult.getDurationNanos()
            )
            .createdAt(
                System.currentTimeMillis()
            )
            .truncated(selection.truncated)
            .originalCharacterCount(
                originalCharacterCount
            )
            .metadata(metadata)
            .build();
    }

    /**
     * 검색 결과를 출처 블록으로 미리 변환합니다.
     */
    private List<PreparedSource> prepareSources(
        List<VectorSearchResult> searchResults
    ) {
        List<PreparedSource> prepared =
            new ArrayList<>(
                searchResults.size()
            );

        int sourceIndex = 1;

        for (VectorSearchResult searchResult
            : searchResults) {

            if (searchResult == null) {
                continue;
            }

            RagSource source =
                RagSource.from(
                    sourceIndex,
                    searchResult
                );

            String block =
                formatSourceBlock(source);

            if (block.isBlank()) {
                continue;
            }

            prepared.add(
                new PreparedSource(
                    source,
                    block
                )
            );

            sourceIndex++;
        }

        return List.copyOf(prepared);
    }

    /**
     * 출처 블록 전체를 기준으로 컨텍스트에 포함할 출처를 선택합니다.
     */
    private SelectionResult selectSources(
        List<PreparedSource> preparedSources
    ) {
        if (preparedSources.isEmpty()) {
            return SelectionResult.empty();
        }

        if (maximumContextCharacters
            == UNLIMITED_CHARACTERS) {

            return selectAll(preparedSources);
        }

        String notice = resolveTruncationNotice();
        StringBuilder context =
            new StringBuilder();

        List<RagSource> selectedSources =
            new ArrayList<>();

        int omittedCount = 0;

        for (int index = 0;
             index < preparedSources.size();
             index++) {

            PreparedSource prepared =
                preparedSources.get(index);

            String separator =
                context.length() == 0
                    ? ""
                    : "\n\n";

            int candidateLength =
                context.length()
                    + separator.length()
                    + prepared.block.length();

            boolean hasRemainingSources =
                index < preparedSources.size() - 1;

            int reservedNoticeLength =
                hasRemainingSources
                    && includeTruncationNotice
                    && !notice.isBlank()
                        ? 2 + notice.length()
                        : 0;

            /*
             * 현재 출처를 추가하면 최대 길이를 넘는 경우
             * 해당 출처부터 나머지 출처는 모두 생략합니다.
             */
            if (candidateLength
                    + reservedNoticeLength
                > maximumContextCharacters) {

                omittedCount =
                    preparedSources.size()
                        - index;

                break;
            }

            context.append(separator)
                .append(prepared.block);

            selectedSources.add(
                prepared.source
            );
        }

        boolean truncated =
            omittedCount > 0;

        /*
         * 첫 번째 출처조차 최대 길이 안에 들어오지 않는 경우에도
         * 출처 본문을 중간에서 자르지 않습니다.
         */
        if (selectedSources.isEmpty()
            && truncated) {

            String emptyMessage =
                createNoSourceFitsMessage(
                    omittedCount
                );

            String finalText =
                fitFixedMessage(
                    emptyMessage
                );

            return new SelectionResult(
                List.of(),
                finalText,
                true,
                omittedCount
            );
        }

        if (truncated
            && includeTruncationNotice
            && !notice.isBlank()) {

            String formattedNotice =
                formatTruncationNotice(
                    notice,
                    omittedCount
                );

            appendNoticeWithinLimit(
                context,
                formattedNotice
            );
        }

        return new SelectionResult(
            List.copyOf(selectedSources),
            context.toString(),
            truncated,
            omittedCount
        );
    }

    /**
     * 제한이 없을 때 모든 출처를 선택합니다.
     */
    private SelectionResult selectAll(
        List<PreparedSource> preparedSources
    ) {
        StringBuilder context =
            new StringBuilder();

        List<RagSource> sources =
            new ArrayList<>(
                preparedSources.size()
            );

        for (PreparedSource prepared
            : preparedSources) {

            if (context.length() > 0) {
                context.append("\n\n");
            }

            context.append(prepared.block);
            sources.add(prepared.source);
        }

        return new SelectionResult(
            List.copyOf(sources),
            context.toString(),
            false,
            0
        );
    }

    /**
     * RagSource를 프롬프트용 출처 블록으로 변환합니다.
     */
    private String formatSourceBlock(
        RagSource source
    ) {
        StringBuilder builder =
            new StringBuilder();

        builder.append("[출처 ")
            .append(source.getIndex())
            .append("]\n");

        builder.append("파일: ")
            .append(
                normalizePath(
                    source.getSourcePath()
                )
            )
            .append('\n');

        if (includeRank
            && source.hasRank()) {

            builder.append("검색 순위: ")
                .append(source.getRank())
                .append('\n');
        }

        if (includeScore) {
            builder.append("유사도: ")
                .append(
                    String.format(
                        Locale.ROOT,
                        "%.4f",
                        source.getScore()
                    )
                )
                .append('\n');
        }

        if (includeTitle
            && !normalize(
                source.getTitle()
            ).isBlank()) {

            builder.append("제목: ")
                .append(source.getTitle())
                .append('\n');
        }

        if (includeElementId
            && !normalize(
                source.getElementId()
            ).isBlank()) {

            builder.append("요소 ID: ")
                .append(source.getElementId())
                .append('\n');
        }

        if (includeChunkType) {
            builder.append("Chunk 유형: ")
                .append(source.getChunkType())
                .append('\n');
        }

        if (includeEpubType
            && !normalize(
                source.getChunk()
                    .getEpubType()
            ).isBlank()) {

            builder.append("EPUB 유형: ")
                .append(
                    source.getChunk()
                        .getEpubType()
                )
                .append('\n');
        }

        if (includeLanguage
            && !normalize(
                source.getChunk()
                    .getLanguage()
            ).isBlank()) {

            builder.append("언어: ")
                .append(
                    source.getChunk()
                        .getLanguage()
                )
                .append('\n');
        }

        if (includeMetadata) {
            appendMetadata(
                builder,
                source
            );
        }

        builder.append("내용:\n")
            .append(
                normalizeMultiline(
                    source.getContent()
                )
            );

        return builder
            .toString()
            .trim();
    }

    private void appendMetadata(
        StringBuilder builder,
        RagSource source
    ) {
        Map<String, String> metadata =
            source.getChunk().getMetadata();

        if (metadata == null
            || metadata.isEmpty()) {

            return;
        }

        builder.append("메타데이터:\n");

        for (Map.Entry<String, String> entry
            : metadata.entrySet()) {

            String key =
                normalize(entry.getKey());

            String value =
                normalize(entry.getValue());

            if (key.isBlank()) {
                continue;
            }

            builder.append("- ")
                .append(key)
                .append(": ")
                .append(value)
                .append('\n');
        }
    }

    /**
     * 제한이 없다고 가정했을 때 전체 컨텍스트 문자 수를 계산합니다.
     */
    private int calculateOriginalCharacterCount(
        List<PreparedSource> preparedSources
    ) {
        int length = 0;

        for (int index = 0;
             index < preparedSources.size();
             index++) {

            if (index > 0) {
                length += 2;
            }

            length +=
                preparedSources
                    .get(index)
                    .block
                    .length();
        }

        return length;
    }

    private Map<String, String> createContextMetadata(
        int totalSourceCount,
        SelectionResult selection
    ) {
        Map<String, String> metadata =
            new LinkedHashMap<>();

        metadata.put(
            "totalSourceCount",
            Integer.toString(
                totalSourceCount
            )
        );

        metadata.put(
            "includedSourceCount",
            Integer.toString(
                selection.sources.size()
            )
        );

        metadata.put(
            "omittedSourceCount",
            Integer.toString(
                selection.omittedCount
            )
        );

        metadata.put(
            "maximumContextCharacters",
            Integer.toString(
                maximumContextCharacters
            )
        );

        metadata.put(
            "truncationStrategy",
            "SOURCE_BLOCK"
        );

        return metadata;
    }

    private RagContext createEmptyContext(
        RetrievalResult retrievalResult
    ) {
        return RagContext.builder()
            .query(retrievalResult.getQuery())
            .contextText("")
            .sources(List.of())
            .embeddingModel(
                retrievalResult.getModel()
            )
            .retrievalDurationNanos(
                retrievalResult.getDurationNanos()
            )
            .createdAt(
                System.currentTimeMillis()
            )
            .truncated(false)
            .originalCharacterCount(0)
            .metadata(
                "totalSourceCount",
                "0"
            )
            .metadata(
                "includedSourceCount",
                "0"
            )
            .metadata(
                "omittedSourceCount",
                "0"
            )
            .metadata(
                "truncationStrategy",
                "SOURCE_BLOCK"
            )
            .build();
    }

    private String resolveTruncationNotice() {
        if (!includeTruncationNotice) {
            return "";
        }

        if (!truncationNotice.isBlank()) {
            return truncationNotice;
        }

        return DEFAULT_TRUNCATION_NOTICE;
    }

    private String formatTruncationNotice(
        String notice,
        int omittedCount
    ) {
        return notice
            + " 생략된 출처 수: "
            + omittedCount
            + "개.";
    }

    private String createNoSourceFitsMessage(
        int omittedCount
    ) {
        return "[안내] 검색 결과가 존재하지만 "
            + "출처 블록 하나가 최대 컨텍스트 길이를 초과하여 "
            + "참고 문서에 포함하지 못했습니다. "
            + "생략된 출처 수: "
            + omittedCount
            + "개.";
    }

    /**
     * 안내문은 출처 본문이 아니므로 최대 길이에 맞춰 마지막 부분만
     * 안전하게 축약할 수 있습니다.
     */
    private String fitFixedMessage(
        String message
    ) {
        if (maximumContextCharacters
            == UNLIMITED_CHARACTERS
            || message.length()
                <= maximumContextCharacters) {

            return message;
        }

        if (maximumContextCharacters <= 3) {
            return ".".repeat(
                maximumContextCharacters
            );
        }

        return message.substring(
            0,
            maximumContextCharacters - 3
        ) + "...";
    }

    private void appendNoticeWithinLimit(
        StringBuilder context,
        String notice
    ) {
        if (notice.isBlank()) {
            return;
        }

        String separator =
            context.length() == 0
                ? ""
                : "\n\n";

        int available =
            maximumContextCharacters
                - context.length()
                - separator.length();

        if (available <= 0) {
            return;
        }

        context.append(separator);

        if (notice.length() <= available) {
            context.append(notice);
            return;
        }

        if (available <= 3) {
            context.append(
                ".".repeat(available)
            );
            return;
        }

        context.append(
            notice,
            0,
            available - 3
        ).append("...");
    }

    private static int validateMaximumCharacters(
        int value
    ) {
        if (value < 0) {
            throw new IllegalArgumentException(
                "maximumContextCharacters must be greater than "
                    + "or equal to zero"
            );
        }

        return value;
    }

    private static String normalize(
        String value
    ) {
        return value == null
            ? ""
            : value.trim();
    }

    private static String normalizePath(
        String value
    ) {
        return normalize(value)
            .replace('\\', '/');
    }

    private static String normalizeMultiline(
        String value
    ) {
        if (value == null) {
            return "";
        }

        return value
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .replaceAll("[\\t ]+", " ")
            .replaceAll("\\n[\\t ]+", "\n")
            .replaceAll("\\n{3,}", "\n\n")
            .trim();
    }

    public int getMaximumContextCharacters() {
        return maximumContextCharacters;
    }

    public boolean isIncludeScore() {
        return includeScore;
    }

    public boolean isIncludeRank() {
        return includeRank;
    }

    public boolean isIncludeTitle() {
        return includeTitle;
    }

    public boolean isIncludeElementId() {
        return includeElementId;
    }

    public boolean isIncludeChunkType() {
        return includeChunkType;
    }

    public boolean isIncludeEpubType() {
        return includeEpubType;
    }

    public boolean isIncludeLanguage() {
        return includeLanguage;
    }

    public boolean isIncludeMetadata() {
        return includeMetadata;
    }

    public boolean isIncludeTruncationNotice() {
        return includeTruncationNotice;
    }

    public String getTruncationNotice() {
        return truncationNotice;
    }

    @Override
    public String toString() {
        return "RagContextBuilder{" +
            "maximumContextCharacters="
                + maximumContextCharacters +
            ", includeScore="
                + includeScore +
            ", includeRank="
                + includeRank +
            ", includeTitle="
                + includeTitle +
            ", includeElementId="
                + includeElementId +
            ", includeChunkType="
                + includeChunkType +
            ", includeEpubType="
                + includeEpubType +
            ", includeLanguage="
                + includeLanguage +
            ", includeMetadata="
                + includeMetadata +
            ", includeTruncationNotice="
                + includeTruncationNotice +
            '}';
    }

    /**
     * 출처와 해당 출처의 완성된 프롬프트 블록을 묶습니다.
     */
    private static final class PreparedSource {

        private final RagSource source;
        private final String block;

        private PreparedSource(
            RagSource source,
            String block
        ) {
            this.source = Objects.requireNonNull(
                source,
                "source must not be null"
            );

            this.block = Objects.requireNonNull(
                block,
                "block must not be null"
            );
        }
    }

    /**
     * 최대 길이 적용 후 선택된 출처와 컨텍스트를 보관합니다.
     */
    private static final class SelectionResult {

        private final List<RagSource> sources;
        private final String contextText;
        private final boolean truncated;
        private final int omittedCount;

        private SelectionResult(
            List<RagSource> sources,
            String contextText,
            boolean truncated,
            int omittedCount
        ) {
            this.sources =
                List.copyOf(sources);

            this.contextText =
                contextText == null
                    ? ""
                    : contextText;

            this.truncated = truncated;
            this.omittedCount = omittedCount;
        }

        private static SelectionResult empty() {
            return new SelectionResult(
                List.of(),
                "",
                false,
                0
            );
        }
    }
}