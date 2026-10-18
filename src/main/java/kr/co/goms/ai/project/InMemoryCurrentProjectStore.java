/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */

package kr.co.goms.ai.project;

import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 메모리 기반 CurrentProjectStore 구현체.
 */
public final class InMemoryCurrentProjectStore
        implements CurrentProjectStore {

    private final AtomicReference<Path> currentProjectRoot = new AtomicReference<>();


    public InMemoryCurrentProjectStore() {
    }


    public InMemoryCurrentProjectStore(Path initialProjectRoot) {

        setCurrentProjectRoot(initialProjectRoot);
    }


    @Override
    public Path getCurrentProjectRoot() {

        return currentProjectRoot.get();
    }


    @Override
    public void setCurrentProjectRoot(Path projectRoot) {

        Objects.requireNonNull(projectRoot, "projectRoot must not be null");

        Path normalizedProjectRoot = projectRoot.toAbsolutePath().normalize();

        currentProjectRoot.set(normalizedProjectRoot);
    }


    @Override
    public boolean hasCurrentProject() {

        return currentProjectRoot.get() != null;
    }


    @Override
    public void clear() {

        currentProjectRoot.set(null);
    }
}