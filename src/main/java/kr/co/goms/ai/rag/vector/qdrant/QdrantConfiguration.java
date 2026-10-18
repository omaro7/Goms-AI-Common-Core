/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 *
 * Project: GomsBook AI
 * AI-powered EPUB authoring, validation, accessibility, and publishing automation.
 */
package kr.co.goms.ai.rag.vector.qdrant;

import java.util.Objects;

public final class QdrantConfiguration {

    private final String host;
    private final int grpcPort;
    private final String collectionName;
    private final boolean tls;


    public QdrantConfiguration(String host, int grpcPort, String collectionName, boolean tls) {

        this.host = requireText(host, "host");
        this.grpcPort = requirePort(grpcPort);
        this.collectionName = requireText(collectionName, "collectionName");
        this.tls = tls;
    }


    public String getHost() {

        return host;
    }


    public int getGrpcPort() {

        return grpcPort;
    }


    public String getCollectionName() {

        return collectionName;
    }


    public boolean isTls() {

        return tls;
    }


    private static String requireText(String value, String name) {

        Objects.requireNonNull(value, name + " must not be null");

        String normalized = value.trim();

        if (normalized.isEmpty()) {

            throw new IllegalArgumentException(name + " must not be blank");
        }

        return normalized;
    }


    private static int requirePort(int value) {

        if (value < 1 || value > 65535) {

            throw new IllegalArgumentException("grpcPort must be between 1 and 65535");
        }

        return value;
    }
}