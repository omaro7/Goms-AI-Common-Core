/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */

package kr.co.goms.ai.rag.index;

import java.util.ArrayList;
import java.util.List;

import kr.co.goms.ai.rag.model.DocumentChunk;
import kr.co.goms.ai.rag.model.DocumentSource;
import kr.co.goms.ai.rag.util.RagUtil;

/**
 * GomsBook 프로젝트 문서를 RAG VectorStore에 인덱싱하는 서비스 인터페이스입니다.
 *
 * <p>DocumentSource를 DocumentChunk로 분할하고 Embedding Vector를 생성한 후
 * VectorRecord를 VectorStore에 저장하는 인덱싱 계층의 공통 계약입니다.</p>
 *
 * <pre>
 * GomsBook Project
 *      ↓
 * DocumentSource
 *      ↓
 * DocumentIndexer
 *      ↓
 * DocumentChunk
 *      ↓
 * EmbeddingClient
 *      ↓
 * VectorRecord
 *      ↓
 * VectorStore
 * </pre>
 *
 * <h2>Project ID 정책</h2>
 *
 * <p>{@code projectId}는 데이터베이스 UUID가 아니라 GomsBook 프로젝트의
 * 안정적인 논리 식별자를 사용합니다.</p>
 *
 * <pre>
 * projectId = lunchwork_seoul
 * projectId = epub-ai-agent
 * projectId = season
 * </pre>
 *
 * <p>인덱싱, 조회, 재인덱싱 및 삭제 과정에서 projectId를 동일하게 전달하여
 * 서로 다른 프로젝트의 Vector 데이터가 섞이지 않도록 합니다.</p>
 *
 * <h2>곰스북 1줄 원칙</h2>
 *
 * <p>메서드 선언, 단순 조건문, 단순 변수 선언, return 및 단순 메서드 호출은
 * 가능한 한 한 줄로 작성합니다.</p>
 *
 * <p>반복문, Builder, 복합 처리처럼 여러 단계의 의미를 표현해야 하는 경우에는
 * 가독성을 유지하기 위해 의미 단위로 줄을 나눕니다.</p>
 */
public interface RagIndexer {

    /**
     * 프로젝트 문서 하나를 기본 인덱싱 설정으로 인덱싱합니다.
     *
     * <p>동일한 projectId와 문서가 이미 인덱싱되어 있는 경우 구현체 정책에 따라
     * 기존 VectorRecord를 재사용하거나 갱신할 수 있습니다.</p>
     *
     * @param projectId GomsBook 논리 프로젝트 ID
     * @param source 인덱싱할 원본 문서
     * @return 인덱싱 결과
     * @throws RagIndexException 인덱싱 실패 시
     */
    RagIndexResult index(String projectId, DocumentSource source) throws RagIndexException;

    /**
     * 프로젝트 문서 하나를 지정된 인덱싱 설정으로 인덱싱합니다.
     *
     * <p>projectId는 VectorRecord에 저장되며 이후 검색, 재사용, 삭제 및
     * Qdrant Payload Filter의 프로젝트 범위로 사용됩니다.</p>
     *
     * @param projectId GomsBook 논리 프로젝트 ID
     * @param source 인덱싱할 원본 문서
     * @param request 인덱싱 설정
     * @return 인덱싱 결과
     * @throws RagIndexException 인덱싱 실패 시
     */
    RagIndexResult index(String projectId, DocumentSource source, RagIndexRequest request) throws RagIndexException;

    /**
     * 프로젝트의 여러 문서를 기본 인덱싱 설정으로 순차 인덱싱합니다.
     *
     * <p>모든 문서에는 동일한 projectId가 적용됩니다.</p>
     *
     * @param projectId GomsBook 논리 프로젝트 ID
     * @param sources 인덱싱할 문서 목록
     * @return 문서별 인덱싱 결과
     * @throws RagIndexException 인덱싱 실패 시
     */
    default List<RagIndexResult> indexAll(String projectId, List<DocumentSource> sources) throws RagIndexException {

        return indexAll(projectId, sources, RagIndexRequest.defaults());
    }

    /**
     * 프로젝트의 여러 문서를 동일한 인덱싱 설정으로 순차 처리합니다.
     *
     * <p>기본 구현은 {@link #index(String, DocumentSource, RagIndexRequest)}를
     * 문서별로 호출합니다.</p>
     *
     * <p>구현체가 Batch Embedding 또는 Batch Vector Upsert를 지원하는 경우
     * 성능 최적화를 위해 이 메서드를 재정의할 수 있습니다.</p>
     *
     * <p>{@link RagIndexRequest#isContinueOnError()}가 true이면 개별 문서 인덱싱
     * 실패가 발생해도 나머지 문서를 계속 처리합니다.</p>
     *
     * @param projectId GomsBook 논리 프로젝트 ID
     * @param sources 인덱싱할 문서 목록
     * @param request 인덱싱 설정
     * @return 문서별 인덱싱 결과
     * @throws RagIndexException 인덱싱 실패 시
     */
    default List<RagIndexResult> indexAll(String projectId, List<DocumentSource> sources, RagIndexRequest request) throws RagIndexException {

        String normalizedProjectId = RagUtil.requireProjectId(projectId);

        if (sources == null || sources.isEmpty()) return List.of();
        if (request == null) throw new IllegalArgumentException("request must not be null");

        List<RagIndexResult> results = new ArrayList<>(sources.size());

        for (DocumentSource source : sources) {

            if (source == null) continue;

            try {

                results.add(index(normalizedProjectId, source, request));

            } catch (RagIndexException exception) {

                if (!request.isContinueOnError()) throw exception;

                results.add(RagIndexResult.failed(source.getRelativePath(), safeMessage(exception)));
            }
        }

        return List.copyOf(results);
    }

    /**
     * 이미 생성된 DocumentChunk 목록을 직접 인덱싱합니다.
     *
     * <p>DocumentIndexer 단계를 외부에서 이미 수행했거나 테스트에서
     * Chunk 생성 단계를 생략하려는 경우 사용할 수 있습니다.</p>
     *
     * <p>생성되는 모든 VectorRecord에는 전달된 projectId가 적용되어야 합니다.</p>
     *
     * @param projectId GomsBook 논리 프로젝트 ID
     * @param source 원본 문서
     * @param chunks 인덱싱할 Chunk 목록
     * @param request 인덱싱 설정
     * @return 인덱싱 결과
     * @throws RagIndexException 인덱싱 실패 시
     */
    RagIndexResult indexChunks(String projectId, DocumentSource source, List<DocumentChunk> chunks, RagIndexRequest request) throws RagIndexException;

    /**
     * 특정 프로젝트 원본 문서의 현재 Embedding Model 인덱스를 삭제합니다.
     *
     * <p>projectId를 기준으로 삭제 범위를 제한하므로 다른 GomsBook 프로젝트에서
     * 동일한 sourcePath를 사용하는 경우에도 영향을 주지 않아야 합니다.</p>
     *
     * @param projectId GomsBook 논리 프로젝트 ID
     * @param sourcePath 프로젝트 기준 원본 문서 상대 경로
     * @return 삭제된 VectorRecord 수
     * @throws RagIndexException 삭제 실패 시
     */
    int remove(String projectId, String sourcePath) throws RagIndexException;

    /**
     * 특정 프로젝트, 원본 문서 경로, Embedding Model에 해당하는 인덱스를 삭제합니다.
     *
     * <pre>
     * projectId  = lunchwork_seoul
     * sourcePath = OEBPS/Text/chapter10_4.xhtml
     * model      = nomic-embed-text
     * </pre>
     *
     * <p>프로젝트 문서 삭제 또는 재인덱싱 시 사용하는 표준 삭제 API입니다.</p>
     *
     * @param projectId GomsBook 논리 프로젝트 ID
     * @param sourcePath 프로젝트 기준 원본 문서 상대 경로
     * @param model 임베딩 모델명
     * @return 삭제된 VectorRecord 수
     * @throws RagIndexException 삭제 실패 시
     */
    int remove(String projectId, String sourcePath, String model) throws RagIndexException;

    /**
     * 특정 프로젝트에서 현재 Embedding Model로 생성된 모든 VectorRecord를 삭제합니다.
     *
     * <p>프로젝트 전체 재인덱싱, 프로젝트별 Vector 초기화 등에 사용합니다.</p>
     *
     * <p>Qdrant와 같이 여러 프로젝트가 하나의 Collection을 공유하는 저장소에서는
     * 반드시 projectId를 삭제 Filter에 포함해야 합니다.</p>
     *
     * <pre>
     * Collection = gomsbook_rag
     *
     * 삭제 대상:
     * projectId = lunchwork_seoul
     * model     = nomic-embed-text
     *
     * 유지 대상:
     * projectId = epub-ai-agent
     * projectId = season
     * </pre>
     *
     * @param projectId GomsBook 논리 프로젝트 ID
     * @return 삭제된 VectorRecord 수
     * @throws RagIndexException 삭제 실패 시
     */
    int removeCurrentModel(String projectId) throws RagIndexException;

    /**
     * VectorStore의 모든 프로젝트 인덱스를 삭제합니다.
     *
     * <p>모든 projectId와 모든 Embedding Model의 VectorRecord가 삭제될 수 있으므로
     * 일반적인 프로젝트 전환이나 프로젝트 종료에서는 사용하지 않습니다.</p>
     *
     * <p>테스트 초기화 또는 명시적인 전체 Vector Store 관리 작업에서만 사용합니다.</p>
     *
     * @throws RagIndexException 전체 인덱스 삭제 실패 시
     */
    void clear() throws RagIndexException;

    /**
     * DocumentSource가 현재 인덱서에서 처리 가능한 형식인지 확인합니다.
     *
     * <p>지원하지 않는 문서는 실제 Chunk 생성이나 Embedding 처리 전에
     * 제외할 수 있습니다.</p>
     *
     * @param source 확인할 원본 문서
     * @return 인덱싱 가능하면 true
     */
    boolean supports(DocumentSource source);

    /**
     * RAG 인덱싱 구성요소가 현재 사용 가능한 상태인지 확인합니다.
     *
     * <p>구현체는 DocumentIndexer, EmbeddingClient, VectorStore 등의
     * 사용 가능 상태를 종합하여 판단할 수 있습니다.</p>
     *
     * @return 인덱싱 가능 상태이면 true
     */
    boolean isAvailable();


    /**
     * 예외 메시지를 결과에 안전하게 기록할 문자열로 변환합니다.
     *
     * @param throwable 원본 예외
     * @return 오류 메시지
     */
    private static String safeMessage(Throwable throwable) {

        if (throwable == null) return "Unknown RAG indexing error";

        String message = throwable.getMessage();

        return message == null || message.isBlank() ? throwable.getClass().getSimpleName() : message.trim();
    }
}