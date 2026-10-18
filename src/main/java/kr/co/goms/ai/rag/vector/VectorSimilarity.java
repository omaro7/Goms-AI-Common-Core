/*
 * Copyright (c) 2026 GomsBook (JungHoon Han)
 * All rights reserved.
 */
package kr.co.goms.ai.rag.vector;

import java.util.Objects;

/**
 * 벡터 간 유사도와 거리를 계산하는 유틸리티 클래스입니다.
 *
 * <p>검색 결과 점수는 모든 방식에서 값이 클수록 관련성이 높도록
 * 통일합니다.</p>
 *
 * <ul>
 *     <li>COSINE: 코사인 유사도, 범위 -1.0 ~ 1.0</li>
 *     <li>DOT_PRODUCT: 내적 점수</li>
 *     <li>EUCLIDEAN: 1 / (1 + distance)로 변환된 점수</li>
 * </ul>
 */
public final class VectorSimilarity {

    private static final double ZERO_MAGNITUDE_EPSILON = 1.0e-12;

    private VectorSimilarity() {
        throw new AssertionError(
            "VectorSimilarity must not be instantiated"
        );
    }

    /**
     * 지정된 방식으로 검색 점수를 계산합니다.
     *
     * @param left 첫 번째 벡터
     * @param right 두 번째 벡터
     * @param type 유사도 계산 방식
     * @return 값이 클수록 관련성이 높은 검색 점수
     */
    public static double score(
        float[] left,
        float[] right,
        VectorSimilarityType type
    ) {
        VectorSimilarityType resolvedType =
            Objects.requireNonNullElse(
                type,
                VectorSimilarityType.COSINE
            );

        switch (resolvedType) {
            case COSINE:
                return cosine(left, right);

            case DOT_PRODUCT:
                return dotProduct(left, right);

            case EUCLIDEAN:
                return euclideanScore(left, right);

            default:
                throw new IllegalArgumentException(
                    "Unsupported similarity type: "
                        + resolvedType
                );
        }
    }

    /**
     * 코사인 유사도를 계산합니다.
     *
     * <pre>
     * cosine(A, B) =
     *     dot(A, B) / (||A|| * ||B||)
     * </pre>
     *
     * @param left 첫 번째 벡터
     * @param right 두 번째 벡터
     * @return 코사인 유사도
     */
    public static double cosine(
        float[] left,
        float[] right
    ) {
        validatePair(left, right);

        double dot = 0.0;
        double leftSquaredSum = 0.0;
        double rightSquaredSum = 0.0;

        for (int index = 0;
             index < left.length;
             index++) {

            double leftValue = left[index];
            double rightValue = right[index];

            dot += leftValue * rightValue;
            leftSquaredSum += leftValue * leftValue;
            rightSquaredSum += rightValue * rightValue;
        }

        if (leftSquaredSum <= ZERO_MAGNITUDE_EPSILON
            || rightSquaredSum <= ZERO_MAGNITUDE_EPSILON) {

            return 0.0;
        }

        double denominator =
            Math.sqrt(leftSquaredSum)
                * Math.sqrt(rightSquaredSum);

        double similarity = dot / denominator;

        /*
         * 부동소수점 오차로 범위를 미세하게 벗어나는 경우를 방지합니다.
         */
        return clamp(similarity, -1.0, 1.0);
    }

    /**
     * 두 벡터가 이미 L2 정규화된 경우 내적으로 코사인 유사도를
     * 계산합니다.
     *
     * <p>일반 cosine()보다 제곱합 계산이 없어 빠르지만, 두 벡터가
     * 실제 단위 벡터인지 호출자가 보장해야 합니다.</p>
     *
     * @param normalizedLeft L2 정규화된 첫 번째 벡터
     * @param normalizedRight L2 정규화된 두 번째 벡터
     * @return 코사인 유사도
     */
    public static double cosineNormalized(
        float[] normalizedLeft,
        float[] normalizedRight
    ) {
        double result = dotProduct(
            normalizedLeft,
            normalizedRight
        );

        return clamp(result, -1.0, 1.0);
    }

    /**
     * 내적을 계산합니다.
     *
     * @param left 첫 번째 벡터
     * @param right 두 번째 벡터
     * @return 내적 값
     */
    public static double dotProduct(
        float[] left,
        float[] right
    ) {
        validatePair(left, right);

        double result = 0.0;

        for (int index = 0;
             index < left.length;
             index++) {

            result +=
                (double) left[index]
                    * right[index];
        }

        return result;
    }

    /**
     * 유클리드 거리를 계산합니다.
     *
     * <p>값이 작을수록 두 벡터가 가깝습니다.</p>
     *
     * @param left 첫 번째 벡터
     * @param right 두 번째 벡터
     * @return 유클리드 거리
     */
    public static double euclideanDistance(
        float[] left,
        float[] right
    ) {
        validatePair(left, right);

        double squaredSum = 0.0;

        for (int index = 0;
             index < left.length;
             index++) {

            double difference =
                (double) left[index]
                    - right[index];

            squaredSum += difference * difference;
        }

        return Math.sqrt(squaredSum);
    }

    /**
     * 유클리드 거리를 값이 클수록 유사한 검색 점수로 변환합니다.
     *
     * <pre>
     * score = 1 / (1 + distance)
     * </pre>
     *
     * <p>두 벡터가 동일하면 1.0이며, 거리가 커질수록 0.0에
     * 가까워집니다.</p>
     *
     * @param left 첫 번째 벡터
     * @param right 두 번째 벡터
     * @return 유클리드 기반 검색 점수
     */
    public static double euclideanScore(
        float[] left,
        float[] right
    ) {
        double distance =
            euclideanDistance(left, right);

        return 1.0 / (1.0 + distance);
    }

    /**
     * 맨해튼 거리를 계산합니다.
     *
     * <p>현재 VectorSimilarityType에는 포함되지 않지만 테스트나
     * 향후 검색 방식 확장을 위해 제공합니다.</p>
     *
     * @param left 첫 번째 벡터
     * @param right 두 번째 벡터
     * @return 맨해튼 거리
     */
    public static double manhattanDistance(
        float[] left,
        float[] right
    ) {
        validatePair(left, right);

        double result = 0.0;

        for (int index = 0;
             index < left.length;
             index++) {

            result += Math.abs(
                (double) left[index]
                    - right[index]
            );
        }

        return result;
    }

    /**
     * 벡터의 L2 크기를 계산합니다.
     *
     * @param vector 대상 벡터
     * @return 벡터 크기
     */
    public static double magnitude(
        float[] vector
    ) {
        validateVector(vector, "vector");

        double squaredSum = 0.0;

        for (float value : vector) {
            squaredSum += (double) value * value;
        }

        return Math.sqrt(squaredSum);
    }

    /**
     * 벡터를 L2 정규화한 새 배열을 반환합니다.
     *
     * @param vector 정규화할 벡터
     * @return L2 정규화된 새 벡터
     */
    public static float[] normalize(
        float[] vector
    ) {
        validateVector(vector, "vector");

        double magnitude = magnitude(vector);

        if (!Double.isFinite(magnitude)
            || magnitude <= ZERO_MAGNITUDE_EPSILON) {

            throw new IllegalArgumentException(
                "vector magnitude must be greater than zero"
            );
        }

        float[] normalized =
            new float[vector.length];

        for (int index = 0;
             index < vector.length;
             index++) {

            normalized[index] =
                (float) (vector[index] / magnitude);
        }

        return normalized;
    }

    /**
     * 벡터가 L2 단위 벡터인지 확인합니다.
     *
     * @param vector 확인할 벡터
     * @param tolerance 허용 오차
     * @return 단위 벡터 여부
     */
    public static boolean isNormalized(
        float[] vector,
        double tolerance
    ) {
        validateVector(vector, "vector");

        if (!Double.isFinite(tolerance)
            || tolerance < 0.0) {

            throw new IllegalArgumentException(
                "tolerance must be a finite non-negative value"
            );
        }

        return Math.abs(
            magnitude(vector) - 1.0
        ) <= tolerance;
    }

    /**
     * 두 벡터의 차원이 동일한지 확인합니다.
     */
    public static boolean hasSameDimensions(
        float[] left,
        float[] right
    ) {
        return left != null
            && right != null
            && left.length == right.length;
    }

    private static void validatePair(
        float[] left,
        float[] right
    ) {
        validateVector(left, "left");
        validateVector(right, "right");

        if (left.length != right.length) {
            throw new IllegalArgumentException(
                "Vector dimensions must match. left="
                    + left.length
                    + ", right="
                    + right.length
            );
        }
    }

    private static void validateVector(
        float[] vector,
        String fieldName
    ) {
        if (vector == null || vector.length == 0) {
            throw new IllegalArgumentException(
                fieldName + " must not be null or empty"
            );
        }

        for (int index = 0;
             index < vector.length;
             index++) {

            if (!Float.isFinite(vector[index])) {
                throw new IllegalArgumentException(
                    fieldName
                        + " contains invalid value at index "
                        + index
                );
            }
        }
    }

    private static double clamp(
        double value,
        double minimum,
        double maximum
    ) {
        return Math.max(
            minimum,
            Math.min(maximum, value)
        );
    }
}