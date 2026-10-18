/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.epub.service;

import java.nio.file.Path;


/**
 * EPUB 출판 결과 저장 디렉터리를 제공합니다.
 *
 * <p>실제 경로는 GomsBookEditor의 결과저장 환경설정에서
 * 제공하는 것을 원칙으로 합니다.</p>
 */
public interface PublishDirectoryProvider {

    Path getPublishDirectory();
}