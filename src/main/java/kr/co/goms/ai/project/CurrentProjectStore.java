/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.project;

import java.nio.file.Path;

public interface CurrentProjectStore {

    Path getCurrentProjectRoot();

    void setCurrentProjectRoot(Path projectRoot);

    boolean hasCurrentProject();

    void clear();
}