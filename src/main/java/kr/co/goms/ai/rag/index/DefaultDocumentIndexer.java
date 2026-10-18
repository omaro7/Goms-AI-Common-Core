/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.rag.index;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import kr.co.goms.ai.rag.model.DocumentChunk;
import kr.co.goms.ai.rag.model.DocumentChunkType;
import kr.co.goms.ai.rag.model.DocumentSource;
import kr.co.goms.ai.rag.model.DocumentSourceType;

/**
 * EPUB 프로젝트 문서를 {@link DocumentChunk} 목록으로 변환하는
 * 기본 {@link DocumentIndexer} 구현체입니다.
 *
 * 지원 문서:
 *
 * <ul>
 *     <li>XHTML 및 HTML 본문</li>
 *     <li>EPUB Navigation Document</li>
 *     <li>OPF Package Document</li>
 *     <li>NCX 문서</li>
 *     <li>CSS 문서</li>
 *     <li>일반 텍스트 및 Markdown</li>
 * </ul>
 *
 * XHTML은 다음 요소를 Chunk로 생성합니다.
 *
 * <ul>
 *     <li>h1~h6</li>
 *     <li>p</li>
 *     <li>li</li>
 *     <li>blockquote</li>
 *     <li>figure 및 figcaption</li>
 *     <li>img alt</li>
 *     <li>table</li>
 * </ul>
 */
public class DefaultDocumentIndexer implements DocumentIndexer {

    private static final Set<DocumentSourceType> SUPPORTED_TYPES =
        Set.of(
            DocumentSourceType.XHTML,
            DocumentSourceType.HTML,
            DocumentSourceType.NAV,
            DocumentSourceType.OPF,
            DocumentSourceType.NCX,
            DocumentSourceType.XML,
            DocumentSourceType.CSS,
            DocumentSourceType.TEXT,
            DocumentSourceType.MARKDOWN,
            DocumentSourceType.JSON
        );

    private static final Set<String> HEADING_TAGS =
        Set.of("h1", "h2", "h3", "h4", "h5", "h6");

    private final int minimumTextLength;
    private final int maximumChunkLength;

    /**
     * 기본 설정으로 인덱서를 생성합니다.
     *
     * 최소 텍스트 길이: 1자
     * 최대 Chunk 길이: 2,000자
     */
    public DefaultDocumentIndexer() {
        this(1, 2_000);
    }

    /**
     * Chunk 생성 조건을 지정합니다.
     *
     * @param minimumTextLength 인덱싱할 최소 텍스트 길이
     * @param maximumChunkLength Chunk 하나의 최대 문자 수
     */
    public DefaultDocumentIndexer(
        int minimumTextLength,
        int maximumChunkLength
    ) {
        if (minimumTextLength < 1) {
            throw new IllegalArgumentException(
                "minimumTextLength must be greater than zero"
            );
        }

        if (maximumChunkLength < minimumTextLength) {
            throw new IllegalArgumentException(
                "maximumChunkLength must be greater than or equal to "
                    + "minimumTextLength"
            );
        }

        this.minimumTextLength = minimumTextLength;
        this.maximumChunkLength = maximumChunkLength;
    }

    @Override
    public List<DocumentChunk> index(
        DocumentSource source
    ) throws DocumentIndexException {

        validate(source);

        try {
            List<DocumentChunk> chunks;

            switch (source.getType()) {
                case XHTML:
                case HTML:
                case XML:
                    chunks = indexMarkupDocument(source);
                    break;

                case NAV:
                    chunks = indexNavigationDocument(source);
                    break;

                case OPF:
                    chunks = indexOpfDocument(source);
                    break;

                case NCX:
                    chunks = indexNcxDocument(source);
                    break;

                case CSS:
                    chunks = indexCssDocument(source);
                    break;

                case TEXT:
                case MARKDOWN:
                case JSON:
                    chunks = indexPlainTextDocument(source);
                    break;

                default:
                    throw new DocumentIndexException(
                        "Unsupported document source type: "
                            + source.getType(),
                        source.getRelativePath()
                    );
            }

            validateChunkIds(chunks);

            return List.copyOf(chunks);

        } catch (DocumentIndexException exception) {
            throw exception;

        } catch (Exception exception) {
            throw new DocumentIndexException(
                "Failed to index document: "
                    + source.getRelativePath(),
                source.getRelativePath(),
                exception
            );
        }
    }

    @Override
    public boolean supports(DocumentSourceType type) {
        return type != null && SUPPORTED_TYPES.contains(type);
    }

    /**
     * XHTML 및 HTML 문서를 인덱싱합니다.
     */
    private List<DocumentChunk> indexMarkupDocument(
        DocumentSource source
    ) throws Exception {

        Document document = parseXml(source);
        Element root = document.getDocumentElement();

        if (root == null) {
            return List.of();
        }

        IndexContext context = new IndexContext(source);

        String documentTitle = findDocumentTitle(document);
        context.setCurrentTitle(documentTitle);

        traverseMarkupElement(root, context);

        return context.getChunks();
    }

    /**
     * XHTML 요소를 문서 순서대로 순회합니다.
     */
    private void traverseMarkupElement(
        Element element,
        IndexContext context
    ) {
        String tagName = localName(element);

        if (HEADING_TAGS.contains(tagName)) {
            indexHeading(element, context);
            return;
        }

        switch (tagName) {
            case "p":
                indexTextElement(
                    element,
                    DocumentChunkType.PARAGRAPH,
                    context
                );
                return;

            case "li":
                indexTextElement(
                    element,
                    DocumentChunkType.LIST_ITEM,
                    context
                );
                return;

            case "blockquote":
                indexTextElement(
                    element,
                    DocumentChunkType.QUOTE,
                    context
                );
                return;

            case "figure":
                indexFigure(element, context);
                return;

            case "img":
                indexImage(element, context);
                return;

            case "table":
                indexTable(element, context);
                return;

            case "script":
            case "style":
            case "svg":
            case "math":
                return;

            default:
                traverseChildren(element, context);
                break;
        }
    }

    private void traverseChildren(
        Element parent,
        IndexContext context
    ) {
        NodeList children = parent.getChildNodes();

        for (int index = 0; index < children.getLength(); index++) {
            Node child = children.item(index);

            if (child.getNodeType() == Node.ELEMENT_NODE) {
                traverseMarkupElement(
                    (Element) child,
                    context
                );
            }
        }
    }

    /**
     * 제목 요소를 Chunk로 생성하고 이후 문단의 현재 제목으로 저장합니다.
     */
    private void indexHeading(
        Element element,
        IndexContext context
    ) {
        String text = normalizeText(element.getTextContent());

        if (!isIndexableText(text)) {
            return;
        }

        context.setCurrentTitle(text);

        Map<String, String> metadata =
            extractCommonMetadata(element);

        metadata.put("headingLevel", resolveHeadingLevel(element));
        metadata.put("tagName", localName(element));

        addTextChunks(
            context,
            element,
            DocumentChunkType.HEADING,
            text,
            metadata
        );
    }

    /**
     * 일반 텍스트 요소를 Chunk로 생성합니다.
     */
    private void indexTextElement(
        Element element,
        DocumentChunkType type,
        IndexContext context
    ) {
        String text = extractOwnSemanticText(element);

        if (!isIndexableText(text)) {
            return;
        }

        Map<String, String> metadata =
            extractCommonMetadata(element);

        metadata.put("tagName", localName(element));

        addTextChunks(
            context,
            element,
            type,
            text,
            metadata
        );
    }

    /**
     * figure의 캡션과 내부 이미지를 인덱싱합니다.
     */
    private void indexFigure(
        Element figure,
        IndexContext context
    ) {
        String figureId = attribute(figure, "id");
        String epubType = attributeByLocalName(
            figure,
            "type",
            "epub:type"
        );

        Element caption = firstDescendant(
            figure,
            "figcaption"
        );

        String captionText = caption == null
            ? ""
            : normalizeText(caption.getTextContent());

        if (isIndexableText(captionText)) {
            Map<String, String> metadata =
                extractCommonMetadata(figure);

            metadata.put("tagName", "figure");
            metadata.put("figureId", figureId);
            metadata.put("caption", captionText);

            if (!epubType.isBlank()) {
                metadata.put("epubType", epubType);
            }

            addTextChunks(
                context,
                figure,
                DocumentChunkType.IMAGE,
                captionText,
                metadata
            );
        }

        NodeList images = figure.getElementsByTagNameNS(
            "*",
            "img"
        );

        if (images.getLength() == 0) {
            images = figure.getElementsByTagName("img");
        }

        for (int index = 0; index < images.getLength(); index++) {
            Element image = (Element) images.item(index);

            indexImage(
                image,
                context,
                figureId,
                captionText
            );
        }
    }

    private void indexImage(
        Element image,
        IndexContext context
    ) {
        indexImage(image, context, "", "");
    }

    /**
     * 이미지 alt 및 경로 정보를 Chunk로 생성합니다.
     */
    private void indexImage(
        Element image,
        IndexContext context,
        String figureId,
        String captionText
    ) {
        String alt = normalizeText(attribute(image, "alt"));
        String src = normalizeText(attribute(image, "src"));
        String imageId = normalizeText(attribute(image, "id"));

        Map<String, String> metadata =
            extractCommonMetadata(image);

        metadata.put("tagName", "img");
        metadata.put("src", src);
        metadata.put("imageId", imageId);
        metadata.put("figureId", figureId);
        metadata.put("caption", captionText);

        if (alt.isBlank()) {
            metadata.put("altMissing", "true");

            String fallback = buildImageFallbackText(
                src,
                captionText
            );

            if (isIndexableText(fallback)) {
                addTextChunks(
                    context,
                    image,
                    DocumentChunkType.IMAGE,
                    fallback,
                    metadata
                );
            }

            return;
        }

        metadata.put("altMissing", "false");

        addTextChunks(
            context,
            image,
            DocumentChunkType.ALT_TEXT,
            alt,
            metadata
        );
    }

    /**
     * 표를 행 단위 텍스트로 변환합니다.
     */
    private void indexTable(
        Element table,
        IndexContext context
    ) {
        String caption = textOfFirstDescendant(
            table,
            "caption"
        );

        List<String> rows = new ArrayList<>();

        NodeList rowElements =
            table.getElementsByTagNameNS("*", "tr");

        if (rowElements.getLength() == 0) {
            rowElements = table.getElementsByTagName("tr");
        }

        for (int rowIndex = 0;
             rowIndex < rowElements.getLength();
             rowIndex++) {

            Element row = (Element) rowElements.item(rowIndex);
            List<String> cells = new ArrayList<>();

            NodeList children = row.getChildNodes();

            for (int childIndex = 0;
                 childIndex < children.getLength();
                 childIndex++) {

                Node child = children.item(childIndex);

                if (child.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }

                Element cell = (Element) child;
                String cellTag = localName(cell);

                if (!"th".equals(cellTag)
                    && !"td".equals(cellTag)) {

                    continue;
                }

                String cellText =
                    normalizeText(cell.getTextContent());

                if (!cellText.isBlank()) {
                    cells.add(cellText);
                }
            }

            if (!cells.isEmpty()) {
                rows.add(String.join(" | ", cells));
            }
        }

        StringBuilder tableText = new StringBuilder();

        if (!caption.isBlank()) {
            tableText.append("표 제목: ")
                .append(caption)
                .append('\n');
        }

        for (String row : rows) {
            if (tableText.length() > 0) {
                tableText.append('\n');
            }

            tableText.append(row);
        }

        String normalized =
            normalizeMultilineText(tableText.toString());

        if (isIndexableText(normalized)) {
            Map<String, String> metadata =
                extractCommonMetadata(table);

            metadata.put("tagName", "table");
            metadata.put("caption", caption);
            metadata.put(
                "rowCount",
                Integer.toString(rows.size())
            );

            addTextChunks(
                context,
                table,
                DocumentChunkType.TABLE,
                normalized,
                metadata
            );
        }
    }

    /**
     * EPUB Navigation Document를 인덱싱합니다.
     */
    private List<DocumentChunk> indexNavigationDocument(
        DocumentSource source
    ) throws Exception {

        Document document = parseXml(source);
        IndexContext context = new IndexContext(source);

        String title = findDocumentTitle(document);
        context.setCurrentTitle(title);

        NodeList anchors =
            document.getElementsByTagNameNS("*", "a");

        if (anchors.getLength() == 0) {
            anchors = document.getElementsByTagName("a");
        }

        for (int index = 0;
             index < anchors.getLength();
             index++) {

            Element anchor = (Element) anchors.item(index);

            String text =
                normalizeText(anchor.getTextContent());

            if (!isIndexableText(text)) {
                continue;
            }

            String href =
                normalizeText(attribute(anchor, "href"));

            Map<String, String> metadata =
                extractCommonMetadata(anchor);

            metadata.put("href", href);
            metadata.put(
                "navigationDepth",
                Integer.toString(resolveListDepth(anchor))
            );

            Element nav = nearestAncestor(anchor, "nav");

            if (nav != null) {
                metadata.put(
                    "navType",
                    attributeByLocalName(
                        nav,
                        "type",
                        "epub:type"
                    )
                );
            }

            addTextChunks(
                context,
                anchor,
                DocumentChunkType.TOC_ENTRY,
                text,
                metadata
            );
        }

        return context.getChunks();
    }

    /**
     * OPF 메타데이터와 manifest/spine 정보를 인덱싱합니다.
     */
    private List<DocumentChunk> indexOpfDocument(
        DocumentSource source
    ) throws Exception {

        Document document = parseXml(source);
        IndexContext context = new IndexContext(source);

        context.setCurrentTitle(
            source.getFileName()
        );

        Element root = document.getDocumentElement();

        if (root == null) {
            return List.of();
        }

        indexOpfMetadata(document, context);
        indexOpfManifest(document, context);
        indexOpfSpine(document, context);

        return context.getChunks();
    }

    private void indexOpfMetadata(
        Document document,
        IndexContext context
    ) {
        Element metadataElement =
            firstElementByLocalName(document, "metadata");

        if (metadataElement == null) {
            return;
        }

        NodeList children =
            metadataElement.getChildNodes();

        for (int index = 0;
             index < children.getLength();
             index++) {

            Node child = children.item(index);

            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            Element element = (Element) child;
            String name = localName(element);
            String value =
                normalizeText(element.getTextContent());

            if (!isIndexableText(value)) {
                continue;
            }

            Map<String, String> metadata =
                extractAttributes(element);

            metadata.put("metadataName", name);
            metadata.put("tagName", name);

            String content = name + ": " + value;

            addTextChunks(
                context,
                element,
                DocumentChunkType.METADATA,
                content,
                metadata
            );
        }
    }

    private void indexOpfManifest(
        Document document,
        IndexContext context
    ) {
        NodeList items =
            document.getElementsByTagNameNS("*", "item");

        if (items.getLength() == 0) {
            items = document.getElementsByTagName("item");
        }

        for (int index = 0;
             index < items.getLength();
             index++) {

            Element item = (Element) items.item(index);

            String id = attribute(item, "id");
            String href = attribute(item, "href");
            String mediaType =
                attribute(item, "media-type");
            String properties =
                attribute(item, "properties");

            String content = String.format(
                Locale.ROOT,
                "manifest item: id=%s, href=%s, media-type=%s, properties=%s",
                id,
                href,
                mediaType,
                properties
            );

            Map<String, String> metadata =
                extractAttributes(item);

            metadata.put("opfSection", "manifest");

            addTextChunks(
                context,
                item,
                DocumentChunkType.METADATA,
                content,
                metadata
            );
        }
    }

    private void indexOpfSpine(
        Document document,
        IndexContext context
    ) {
        NodeList itemRefs =
            document.getElementsByTagNameNS("*", "itemref");

        if (itemRefs.getLength() == 0) {
            itemRefs = document.getElementsByTagName("itemref");
        }

        for (int index = 0;
             index < itemRefs.getLength();
             index++) {

            Element itemRef =
                (Element) itemRefs.item(index);

            String idref =
                attribute(itemRef, "idref");
            String linear =
                attribute(itemRef, "linear");
            String properties =
                attribute(itemRef, "properties");

            String content = String.format(
                Locale.ROOT,
                "spine item: index=%d, idref=%s, linear=%s, properties=%s",
                index,
                idref,
                linear,
                properties
            );

            Map<String, String> metadata =
                extractAttributes(itemRef);

            metadata.put("opfSection", "spine");
            metadata.put(
                "spineIndex",
                Integer.toString(index)
            );

            addTextChunks(
                context,
                itemRef,
                DocumentChunkType.METADATA,
                content,
                metadata
            );
        }
    }

    /**
     * EPUB2 NCX 목차를 인덱싱합니다.
     */
    private List<DocumentChunk> indexNcxDocument(
        DocumentSource source
    ) throws Exception {

        Document document = parseXml(source);
        IndexContext context = new IndexContext(source);

        context.setCurrentTitle(
            textOfFirstElementByLocalName(
                document,
                "docTitle"
            )
        );

        NodeList navPoints =
            document.getElementsByTagNameNS("*", "navPoint");

        if (navPoints.getLength() == 0) {
            navPoints =
                document.getElementsByTagName("navPoint");
        }

        for (int index = 0;
             index < navPoints.getLength();
             index++) {

            Element navPoint =
                (Element) navPoints.item(index);

            String label =
                textOfFirstDescendant(navPoint, "text");

            Element contentElement =
                firstDescendant(navPoint, "content");

            String src = contentElement == null
                ? ""
                : attribute(contentElement, "src");

            if (!isIndexableText(label)) {
                continue;
            }

            Map<String, String> metadata =
                extractCommonMetadata(navPoint);

            metadata.put("src", src);
            metadata.put(
                "playOrder",
                attribute(navPoint, "playOrder")
            );

            addTextChunks(
                context,
                navPoint,
                DocumentChunkType.TOC_ENTRY,
                label,
                metadata
            );
        }

        return context.getChunks();
    }

    /**
     * CSS를 규칙 단위로 분리해 인덱싱합니다.
     */
    private List<DocumentChunk> indexCssDocument(
        DocumentSource source
    ) {
        IndexContext context = new IndexContext(source);
        context.setCurrentTitle(source.getFileName());

        String content = removeCssComments(source.getContent());

        int sequence = 0;
        int cursor = 0;

        while (cursor < content.length()) {
            int openBrace = content.indexOf('{', cursor);

            if (openBrace < 0) {
                break;
            }

            int closeBrace =
                findMatchingBrace(content, openBrace);

            if (closeBrace < 0) {
                break;
            }

            String selector = normalizeText(
                content.substring(cursor, openBrace)
            );

            String declarations = normalizeMultilineText(
                content.substring(openBrace + 1, closeBrace)
            );

            cursor = closeBrace + 1;

            if (selector.isBlank()
                || declarations.isBlank()) {

                continue;
            }

            String rule = selector
                + " { "
                + declarations
                + " }";

            Map<String, String> metadata =
                new LinkedHashMap<>();

            metadata.put("selector", selector);
            metadata.put(
                "ruleIndex",
                Integer.toString(sequence++)
            );

            addTextChunks(
                context,
                null,
                DocumentChunkType.STYLE,
                rule,
                metadata
            );
        }

        return context.getChunks();
    }

    /**
     * 일반 텍스트 문서를 빈 줄 기준으로 분할합니다.
     */
    private List<DocumentChunk> indexPlainTextDocument(
        DocumentSource source
    ) {
        IndexContext context = new IndexContext(source);
        context.setCurrentTitle(source.getFileName());

        String normalized =
            source.getContent()
                .replace("\r\n", "\n")
                .replace('\r', '\n');

        String[] blocks =
            normalized.split("\\n\\s*\\n");

        for (String block : blocks) {
            String text =
                normalizeMultilineText(block);

            if (!isIndexableText(text)) {
                continue;
            }

            addTextChunks(
                context,
                null,
                DocumentChunkType.TEXT,
                text,
                Map.of()
            );
        }

        return context.getChunks();
    }

    /**
     * 최대 길이를 초과하는 텍스트를 여러 Chunk로 나눕니다.
     */
    private void addTextChunks(
        IndexContext context,
        Element element,
        DocumentChunkType type,
        String text,
        Map<String, String> metadata
    ) {
        List<String> splitTexts =
            splitText(text, maximumChunkLength);

        for (int partIndex = 0;
             partIndex < splitTexts.size();
             partIndex++) {

            String part = splitTexts.get(partIndex);

            if (!isIndexableText(part)) {
                continue;
            }

            Map<String, String> partMetadata =
                new LinkedHashMap<>(metadata);

            if (splitTexts.size() > 1) {
                partMetadata.put(
                    "partIndex",
                    Integer.toString(partIndex)
                );
                partMetadata.put(
                    "partCount",
                    Integer.toString(splitTexts.size())
                );
            }

            context.addChunk(
                element,
                type,
                part,
                partMetadata
            );
        }
    }

    /**
     * XML 외부 엔티티 공격을 방지하도록 안전한 파서를 생성합니다.
     */
    private Document parseXml(
        DocumentSource source
    ) throws Exception {

        DocumentBuilderFactory factory =
            DocumentBuilderFactory.newInstance();

        factory.setNamespaceAware(true);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);

        setFeatureSafely(
            factory,
            "http://apache.org/xml/features/disallow-doctype-decl",
            false
        );

        setFeatureSafely(
            factory,
            "http://xml.org/sax/features/external-general-entities",
            false
        );

        setFeatureSafely(
            factory,
            "http://xml.org/sax/features/external-parameter-entities",
            false
        );

        setFeatureSafely(
            factory,
            "http://apache.org/xml/features/nonvalidating/load-external-dtd",
            false
        );

        try {
            factory.setAttribute(
                XMLConstants.ACCESS_EXTERNAL_DTD,
                ""
            );
            factory.setAttribute(
                XMLConstants.ACCESS_EXTERNAL_SCHEMA,
                ""
            );
        } catch (IllegalArgumentException ignored) {
            // 일부 XML 구현체에서는 해당 속성을 지원하지 않을 수 있습니다.
        }

        DocumentBuilder builder =
            factory.newDocumentBuilder();

        InputSource inputSource =
            new InputSource(
                new StringReader(source.getContent())
            );

        inputSource.setSystemId(
            source.getRelativePath()
        );

        return builder.parse(inputSource);
    }

    private void setFeatureSafely(
        DocumentBuilderFactory factory,
        String feature,
        boolean value
    ) {
        try {
            factory.setFeature(feature, value);
        } catch (Exception ignored) {
            // XML 파서 구현체별 미지원 기능은 무시합니다.
        }
    }

    private String findDocumentTitle(
        Document document
    ) {
        String title =
            textOfFirstElementByLocalName(
                document,
                "title"
            );

        if (!title.isBlank()) {
            return title;
        }

        for (String heading : HEADING_TAGS) {
            String value =
                textOfFirstElementByLocalName(
                    document,
                    heading
                );

            if (!value.isBlank()) {
                return value;
            }
        }

        return "";
    }

    /**
     * p 또는 li 안의 중첩 figure/table 등의 텍스트가 중복되지 않도록
     * 직접적인 의미 텍스트를 추출합니다.
     */
    private String extractOwnSemanticText(
        Element element
    ) {
        StringBuilder result = new StringBuilder();

        appendSemanticText(element, result, true);

        return normalizeText(result.toString());
    }

    private void appendSemanticText(
        Node node,
        StringBuilder result,
        boolean root
    ) {
        NodeList children = node.getChildNodes();

        for (int index = 0;
             index < children.getLength();
             index++) {

            Node child = children.item(index);

            if (child.getNodeType() == Node.TEXT_NODE
                || child.getNodeType() == Node.CDATA_SECTION_NODE) {

                result.append(child.getNodeValue())
                    .append(' ');

                continue;
            }

            if (child.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            Element childElement = (Element) child;
            String tag = localName(childElement);

            if (!root && isBlockIndexedSeparately(tag)) {
                continue;
            }

            if ("img".equals(tag)) {
                continue;
            }

            appendSemanticText(
                childElement,
                result,
                false
            );
        }
    }

    private boolean isBlockIndexedSeparately(String tag) {
        return HEADING_TAGS.contains(tag)
            || "p".equals(tag)
            || "li".equals(tag)
            || "blockquote".equals(tag)
            || "figure".equals(tag)
            || "table".equals(tag);
    }

    private Map<String, String> extractCommonMetadata(
        Element element
    ) {
        Map<String, String> metadata =
            extractAttributes(element);

        metadata.put(
            "elementId",
            attribute(element, "id")
        );

        String className =
            attribute(element, "class");

        if (!className.isBlank()) {
            metadata.put("class", className);
        }

        String epubType =
            attributeByLocalName(
                element,
                "type",
                "epub:type"
            );

        if (!epubType.isBlank()) {
            metadata.put("epubType", epubType);
        }

        String role =
            attribute(element, "role");

        if (!role.isBlank()) {
            metadata.put("role", role);
        }

        return metadata;
    }

    private Map<String, String> extractAttributes(
        Element element
    ) {
        Map<String, String> result =
            new LinkedHashMap<>();

        NamedNodeMap attributes =
            element.getAttributes();

        for (int index = 0;
             index < attributes.getLength();
             index++) {

            Node attribute =
                attributes.item(index);

            String name =
                attribute.getNodeName();

            String value =
                normalizeText(attribute.getNodeValue());

            if (!name.isBlank()
                && !value.isBlank()) {

                result.put(name, value);
            }
        }

        return result;
    }

    private List<String> splitText(
        String text,
        int maximumLength
    ) {
        String normalized =
            normalizeMultilineText(text);

        if (normalized.length() <= maximumLength) {
            return List.of(normalized);
        }

        List<String> result = new ArrayList<>();
        int start = 0;

        while (start < normalized.length()) {
            int desiredEnd = Math.min(
                start + maximumLength,
                normalized.length()
            );

            int end = desiredEnd;

            if (desiredEnd < normalized.length()) {
                end = findSplitPosition(
                    normalized,
                    start,
                    desiredEnd
                );
            }

            String part =
                normalized.substring(start, end).trim();

            if (!part.isBlank()) {
                result.add(part);
            }

            start = end;

            while (start < normalized.length()
                && Character.isWhitespace(
                    normalized.charAt(start)
                )) {

                start++;
            }
        }

        return result;
    }

    private int findSplitPosition(
        String text,
        int start,
        int desiredEnd
    ) {
        int minimumEnd =
            Math.max(start + 1, desiredEnd - 300);

        for (int index = desiredEnd;
             index > minimumEnd;
             index--) {

            char character =
                text.charAt(index - 1);

            if (character == '.'
                || character == '!'
                || character == '?'
                || character == '。'
                || character == '！'
                || character == '？'
                || character == '\n') {

                return index;
            }
        }

        for (int index = desiredEnd;
             index > minimumEnd;
             index--) {

            if (Character.isWhitespace(
                text.charAt(index - 1)
            )) {
                return index;
            }
        }

        return desiredEnd;
    }

    private int findMatchingBrace(
        String content,
        int openBrace
    ) {
        int depth = 0;
        boolean inString = false;
        char quote = 0;

        for (int index = openBrace;
             index < content.length();
             index++) {

            char character = content.charAt(index);

            if (inString) {
                if (character == quote
                    && (index == 0
                    || content.charAt(index - 1) != '\\')) {

                    inString = false;
                }

                continue;
            }

            if (character == '\''
                || character == '"') {

                inString = true;
                quote = character;
                continue;
            }

            if (character == '{') {
                depth++;
            } else if (character == '}') {
                depth--;

                if (depth == 0) {
                    return index;
                }
            }
        }

        return -1;
    }

    private String removeCssComments(String css) {
        if (css == null || css.isBlank()) {
            return "";
        }

        return css.replaceAll(
            "(?s)/\\*.*?\\*/",
            " "
        );
    }

    private String buildImageFallbackText(
        String src,
        String caption
    ) {
        if (!caption.isBlank()) {
            return "이미지 캡션: " + caption;
        }

        if (!src.isBlank()) {
            return "대체 텍스트가 없는 이미지: " + src;
        }

        return "대체 텍스트가 없는 이미지";
    }

    private String resolveHeadingLevel(Element element) {
        String tag = localName(element);

        if (tag.length() == 2
            && tag.charAt(0) == 'h'
            && Character.isDigit(tag.charAt(1))) {

            return Character.toString(tag.charAt(1));
        }

        return "";
    }

    private int resolveListDepth(Element element) {
        int depth = 0;
        Node current = element.getParentNode();

        while (current != null) {
            if (current.getNodeType() == Node.ELEMENT_NODE) {
                String name =
                    localName((Element) current);

                if ("ol".equals(name)
                    || "ul".equals(name)) {

                    depth++;
                }
            }

            current = current.getParentNode();
        }

        return depth;
    }

    private Element nearestAncestor(
        Element element,
        String tagName
    ) {
        Node current = element.getParentNode();

        while (current != null) {
            if (current.getNodeType() == Node.ELEMENT_NODE) {
                Element candidate =
                    (Element) current;

                if (tagName.equals(
                    localName(candidate)
                )) {
                    return candidate;
                }
            }

            current = current.getParentNode();
        }

        return null;
    }

    private Element firstDescendant(
        Element parent,
        String localName
    ) {
        NodeList elements =
            parent.getElementsByTagNameNS("*", localName);

        if (elements.getLength() == 0) {
            elements =
                parent.getElementsByTagName(localName);
        }

        return elements.getLength() == 0
            ? null
            : (Element) elements.item(0);
    }

    private Element firstElementByLocalName(
        Document document,
        String localName
    ) {
        NodeList elements =
            document.getElementsByTagNameNS("*", localName);

        if (elements.getLength() == 0) {
            elements =
                document.getElementsByTagName(localName);
        }

        return elements.getLength() == 0
            ? null
            : (Element) elements.item(0);
    }

    private String textOfFirstElementByLocalName(
        Document document,
        String localName
    ) {
        Element element =
            firstElementByLocalName(
                document,
                localName
            );

        return element == null
            ? ""
            : normalizeText(element.getTextContent());
    }

    private String textOfFirstDescendant(
        Element parent,
        String localName
    ) {
        Element element =
            firstDescendant(parent, localName);

        return element == null
            ? ""
            : normalizeText(element.getTextContent());
    }

    private String localName(Element element) {
        String localName =
            element.getLocalName();

        if (localName != null
            && !localName.isBlank()) {

            return localName.toLowerCase(Locale.ROOT);
        }

        String nodeName = element.getNodeName();
        int separator = nodeName.indexOf(':');

        if (separator >= 0
            && separator < nodeName.length() - 1) {

            nodeName =
                nodeName.substring(separator + 1);
        }

        return nodeName.toLowerCase(Locale.ROOT);
    }

    private String attribute(
        Element element,
        String name
    ) {
        if (element == null
            || name == null
            || name.isBlank()) {

            return "";
        }

        return normalizeText(
            element.getAttribute(name)
        );
    }

    private String attributeByLocalName(
        Element element,
        String localName,
        String fallbackName
    ) {
        String value = "";

        if (element.hasAttributeNS(
            "http://www.idpf.org/2007/ops",
            localName
        )) {
            value = element.getAttributeNS(
                "http://www.idpf.org/2007/ops",
                localName
            );
        }

        if (value == null || value.isBlank()) {
            value = element.getAttribute(fallbackName);
        }

        if (value == null || value.isBlank()) {
            value = element.getAttribute(localName);
        }

        return normalizeText(value);
    }

    private boolean isIndexableText(String text) {
        return text != null
            && text.trim().length() >= minimumTextLength;
    }

    private String normalizeText(String text) {
        if (text == null) {
            return "";
        }

        return text
            .replace('\u00A0', ' ')
            .replaceAll("\\s+", " ")
            .trim();
    }

    private String normalizeMultilineText(String text) {
        if (text == null) {
            return "";
        }

        return text
            .replace('\u00A0', ' ')
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .replaceAll("[\\t ]+", " ")
            .replaceAll("\\n{3,}", "\n\n")
            .trim();
    }

    public int getMinimumTextLength() {
        return minimumTextLength;
    }

    public int getMaximumChunkLength() {
        return maximumChunkLength;
    }

    /**
     * 문서 하나의 Chunk 생성 상태를 관리합니다.
     */
    private static final class IndexContext {

        private final DocumentSource source;
        private final List<DocumentChunk> chunks =
            new ArrayList<>();

        private String currentTitle = "";
        private int sequence;

        private IndexContext(DocumentSource source) {
            this.source = Objects.requireNonNull(
                source,
                "source must not be null"
            );
        }

        private void setCurrentTitle(String title) {
            if (title != null && !title.isBlank()) {
                this.currentTitle = title.trim();
            }
        }

        private void addChunk(
            Element element,
            DocumentChunkType type,
            String content,
            Map<String, String> metadata
        ) {
            int currentSequence = sequence++;

            String elementId = element == null
                ? ""
                : normalizeElementId(
                    element.getAttribute("id")
                );

            String chunkId = createChunkId(
                source,
                type,
                elementId,
                currentSequence
            );

            Map<String, String> mergedMetadata =
                new LinkedHashMap<>(
                    source.getMetadata()
                );

            if (metadata != null) {
                mergedMetadata.putAll(metadata);
            }

            mergedMetadata.put(
                "sourceType",
                source.getType().name()
            );

            mergedMetadata.put(
                "sequence",
                Integer.toString(currentSequence)
            );

            DocumentChunk chunk =
                DocumentChunk.builder()
                    .id(chunkId)
                    .sourcePath(source.getRelativePath())
                    .title(currentTitle)
                    .type(type)
                    .content(content)
                    .sequence(currentSequence)
                    .elementId(elementId)
                    .epubType(
                        mergedMetadata.getOrDefault(
                            "epubType",
                            source.getMetadataOrDefault(
                                "epubType",
                                ""
                            )
                        )
                    )
                    .language(source.getLanguage())
                    .metadata(mergedMetadata)
                    .build();

            chunks.add(chunk);
        }

        private List<DocumentChunk> getChunks() {
            return chunks;
        }

        private static String createChunkId(
            DocumentSource source,
            DocumentChunkType type,
            String elementId,
            int sequence
        ) {
            StringBuilder id =
                new StringBuilder(
                    source.getRelativePath()
                );

            id.append('#');

            if (!elementId.isBlank()) {
                id.append(elementId);
            } else {
                id.append(
                    type.name()
                        .toLowerCase(Locale.ROOT)
                );
                id.append('_');
                id.append(
                    String.format(
                        Locale.ROOT,
                        "%04d",
                        sequence
                    )
                );
            }

            return id.toString();
        }

        private static String normalizeElementId(
            String elementId
        ) {
            return elementId == null
                ? ""
                : elementId.trim();
        }
    }
}