package com.capstone.bwlovers.loadtest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class RegistrationLoadTestRunner {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String REQUEST_INDEX_TOKEN = "{{requestIndex}}";
    private static final String REQUEST_NUMBER_TOKEN = "{{requestNumber}}";

    public static void main(String[] args) throws Exception {
        LoadTestConfig config = LoadTestConfig.fromSystemProperties();
        LoadTestSummary summary = run(config);
        System.out.println(summary.render(config));
    }

    static LoadTestSummary run(LoadTestConfig config) {
        ExecutorService executor = Executors.newFixedThreadPool(config.concurrentUsers());
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(config.connectTimeoutMs()))
                .executor(executor)
                .build();

        try {
            List<CompletableFuture<SingleResult>> futures = IntStream.range(0, config.totalRequests())
                    .mapToObj(index -> CompletableFuture.supplyAsync(() -> sendRequest(client, config, index), executor))
                    .toList();

            List<SingleResult> results = futures.stream()
                    .map(CompletableFuture::join)
                    .toList();

            return LoadTestSummary.fromResults(results);
        } finally {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    static ResponseCategory classifyResponse(int statusCode, String responseBody, LoadTestConfig config) {
        String classifiedValue = extractFieldValue(responseBody, config.classifierField());
        if (matchesValue(classifiedValue, config.duplicateValues())) {
            return ResponseCategory.DUPLICATE;
        }
        if (matchesValue(classifiedValue, config.waitingValues())) {
            return ResponseCategory.WAITING;
        }
        if (matchesValue(classifiedValue, config.confirmedValues())) {
            return ResponseCategory.CONFIRMED;
        }

        if (matchesPattern(responseBody, config.duplicatePatterns())) {
            return ResponseCategory.DUPLICATE;
        }
        if (matchesPattern(responseBody, config.waitingPatterns())) {
            return ResponseCategory.WAITING;
        }
        if (matchesPattern(responseBody, config.confirmedPatterns())) {
            return ResponseCategory.CONFIRMED;
        }

        if (config.duplicateStatuses().contains(statusCode)) {
            return ResponseCategory.DUPLICATE;
        }
        if (config.waitingStatuses().contains(statusCode)) {
            return ResponseCategory.WAITING;
        }
        if (config.confirmedStatuses().contains(statusCode)) {
            return ResponseCategory.CONFIRMED;
        }

        return ResponseCategory.OTHER;
    }

    private static SingleResult sendRequest(HttpClient client, LoadTestConfig config, int requestIndex) {
        RequestSpec requestSpec = config.resolve(requestIndex);
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(requestSpec.url()))
                .timeout(Duration.ofMillis(config.requestTimeoutMs()));

        requestSpec.headers().forEach(builder::header);

        boolean hasBody = !requestSpec.body().isBlank();
        HttpRequest.BodyPublisher bodyPublisher = hasBody
                ? HttpRequest.BodyPublishers.ofString(requestSpec.body(), StandardCharsets.UTF_8)
                : HttpRequest.BodyPublishers.noBody();

        HttpRequest request = builder.method(config.method(), bodyPublisher).build();

        long startNanos = System.nanoTime();
        try {
            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );
            long latencyMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
            ResponseCategory category = classifyResponse(response.statusCode(), response.body(), config);
            return SingleResult.success(category, response.statusCode(), latencyMs, response.body());
        } catch (Exception e) {
            long latencyMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
            String message = e.getClass().getSimpleName() + ": " + nullSafe(e.getMessage());
            return SingleResult.failure(latencyMs, message);
        }
    }

    private static String extractFieldValue(String responseBody, String classifierField) {
        if (isBlank(responseBody) || isBlank(classifierField)) {
            return null;
        }

        try {
            JsonNode current = OBJECT_MAPPER.readTree(responseBody);
            for (String field : classifierField.split("\\.")) {
                if (current == null) {
                    return null;
                }
                current = current.get(field);
            }
            if (current == null || current.isNull()) {
                return null;
            }
            return current.asText();
        } catch (IOException ignored) {
            return null;
        }
    }

    private static boolean matchesValue(String value, Set<String> expectedValues) {
        if (value == null || expectedValues.isEmpty()) {
            return false;
        }
        return expectedValues.contains(normalize(value));
    }

    private static boolean matchesPattern(String value, List<Pattern> patterns) {
        if (isBlank(value) || patterns.isEmpty()) {
            return false;
        }

        for (Pattern pattern : patterns) {
            if (pattern.matcher(value).find()) {
                return true;
            }
        }
        return false;
    }

    private static String renderLine(String label, String value) {
        return String.format("%-10s %s", label, value);
    }

    private static Map<String, String> systemPropertiesSnapshot() {
        List<String> propertyNames = List.of(
                "loadTestUrl",
                "loadTestMethod",
                "loadTestConcurrentUsers",
                "loadTestTotalRequests",
                "loadTestBodyFile",
                "loadTestHeadersFile",
                "loadTestContentType",
                "loadTestAuthToken",
                "loadTestConnectTimeoutMs",
                "loadTestRequestTimeoutMs",
                "loadTestClassifierField",
                "loadTestConfirmedValues",
                "loadTestWaitingValues",
                "loadTestDuplicateValues",
                "loadTestConfirmedStatuses",
                "loadTestWaitingStatuses",
                "loadTestDuplicateStatuses",
                "loadTestConfirmedPatterns",
                "loadTestWaitingPatterns",
                "loadTestDuplicatePatterns"
        );

        Map<String, String> properties = new LinkedHashMap<>();
        for (String propertyName : propertyNames) {
            String value = System.getProperty(propertyName);
            if (!isBlank(value)) {
                properties.put(propertyName, value);
            }
        }
        return properties;
    }

    private static int parseInt(Map<String, String> properties, String propertyName, int defaultValue) {
        String value = properties.get(propertyName);
        if (isBlank(value)) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(propertyName + " must be a number: " + value);
        }
    }

    private static String require(Map<String, String> properties, String propertyName) {
        String value = properties.get(propertyName);
        if (isBlank(value)) {
            throw new IllegalArgumentException(propertyName + " is required.");
        }
        return value.trim();
    }

    private static String orDefault(Map<String, String> properties, String propertyName, String defaultValue) {
        String value = properties.get(propertyName);
        return isBlank(value) ? defaultValue : value.trim();
    }

    private static Set<String> parseStringSet(String rawValue) {
        if (isBlank(rawValue)) {
            return Set.of();
        }

        return Arrays.stream(rawValue.split(","))
                .map(String::trim)
                .filter(token -> !token.isEmpty())
                .map(RegistrationLoadTestRunner::normalize)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private static Set<Integer> parseIntegerSet(String rawValue) {
        if (isBlank(rawValue)) {
            return Set.of();
        }

        return Arrays.stream(rawValue.split(","))
                .map(String::trim)
                .filter(token -> !token.isEmpty())
                .map(Integer::parseInt)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private static List<Pattern> parsePatterns(String rawValue) {
        if (isBlank(rawValue)) {
            return List.of();
        }

        return Arrays.stream(rawValue.split(","))
                .map(String::trim)
                .filter(token -> !token.isEmpty())
                .map(token -> Pattern.compile(token, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE))
                .toList();
    }

    private static Map<String, String> readHeadersFile(String headersFile) throws IOException {
        if (isBlank(headersFile)) {
            return new LinkedHashMap<>();
        }

        JsonNode root = OBJECT_MAPPER.readTree(Files.readString(Path.of(headersFile), StandardCharsets.UTF_8));
        if (!root.isObject()) {
            throw new IllegalArgumentException("loadTestHeadersFile must point to a JSON object file.");
        }

        Map<String, String> headers = new LinkedHashMap<>();
        root.fields().forEachRemaining(entry -> headers.put(entry.getKey(), entry.getValue().asText()));
        return headers;
    }

    private static String readBodyFile(String bodyFile) throws IOException {
        if (isBlank(bodyFile)) {
            return "";
        }
        return Files.readString(Path.of(bodyFile), StandardCharsets.UTF_8);
    }

    private static String replaceTokens(String template, int requestIndex) {
        if (template == null) {
            return "";
        }

        return template
                .replace(REQUEST_INDEX_TOKEN, Integer.toString(requestIndex))
                .replace(REQUEST_NUMBER_TOKEN, Integer.toString(requestIndex + 1));
    }

    private static String normalize(String value) {
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }

    static final class LoadTestConfig {
        private final String urlTemplate;
        private final String method;
        private final int concurrentUsers;
        private final int totalRequests;
        private final Map<String, String> headersTemplate;
        private final String bodyTemplate;
        private final int connectTimeoutMs;
        private final int requestTimeoutMs;
        private final String classifierField;
        private final Set<String> confirmedValues;
        private final Set<String> waitingValues;
        private final Set<String> duplicateValues;
        private final Set<Integer> confirmedStatuses;
        private final Set<Integer> waitingStatuses;
        private final Set<Integer> duplicateStatuses;
        private final List<Pattern> confirmedPatterns;
        private final List<Pattern> waitingPatterns;
        private final List<Pattern> duplicatePatterns;

        private LoadTestConfig(
                String urlTemplate,
                String method,
                int concurrentUsers,
                int totalRequests,
                Map<String, String> headersTemplate,
                String bodyTemplate,
                int connectTimeoutMs,
                int requestTimeoutMs,
                String classifierField,
                Set<String> confirmedValues,
                Set<String> waitingValues,
                Set<String> duplicateValues,
                Set<Integer> confirmedStatuses,
                Set<Integer> waitingStatuses,
                Set<Integer> duplicateStatuses,
                List<Pattern> confirmedPatterns,
                List<Pattern> waitingPatterns,
                List<Pattern> duplicatePatterns
        ) {
            this.urlTemplate = urlTemplate;
            this.method = method;
            this.concurrentUsers = concurrentUsers;
            this.totalRequests = totalRequests;
            this.headersTemplate = headersTemplate;
            this.bodyTemplate = bodyTemplate;
            this.connectTimeoutMs = connectTimeoutMs;
            this.requestTimeoutMs = requestTimeoutMs;
            this.classifierField = classifierField;
            this.confirmedValues = confirmedValues;
            this.waitingValues = waitingValues;
            this.duplicateValues = duplicateValues;
            this.confirmedStatuses = confirmedStatuses;
            this.waitingStatuses = waitingStatuses;
            this.duplicateStatuses = duplicateStatuses;
            this.confirmedPatterns = confirmedPatterns;
            this.waitingPatterns = waitingPatterns;
            this.duplicatePatterns = duplicatePatterns;
        }

        static LoadTestConfig fromSystemProperties() throws IOException {
            return fromProperties(systemPropertiesSnapshot());
        }

        static LoadTestConfig fromProperties(Map<String, String> properties) throws IOException {
            String urlTemplate = require(properties, "loadTestUrl");
            String method = orDefault(properties, "loadTestMethod", "POST").toUpperCase(Locale.ROOT);
            int concurrentUsers = parseInt(properties, "loadTestConcurrentUsers", 100);
            int totalRequests = parseInt(properties, "loadTestTotalRequests", concurrentUsers);
            int connectTimeoutMs = parseInt(properties, "loadTestConnectTimeoutMs", 2000);
            int requestTimeoutMs = parseInt(properties, "loadTestRequestTimeoutMs", 5000);
            String classifierField = orDefault(properties, "loadTestClassifierField", "");

            Map<String, String> headers = readHeadersFile(properties.get("loadTestHeadersFile"));
            String bodyTemplate = readBodyFile(properties.get("loadTestBodyFile"));
            String contentType = orDefault(properties, "loadTestContentType", "application/json");
            if (!bodyTemplate.isBlank() && !headers.containsKey("Content-Type")) {
                headers.put("Content-Type", contentType);
            }

            String authToken = properties.get("loadTestAuthToken");
            if (!isBlank(authToken) && !headers.containsKey("Authorization")) {
                headers.put("Authorization", "Bearer " + authToken.trim());
            }

            Set<Integer> confirmedStatuses = parseIntegerSet(orDefault(properties, "loadTestConfirmedStatuses", "200,201"));
            Set<Integer> waitingStatuses = parseIntegerSet(orDefault(properties, "loadTestWaitingStatuses", "202"));
            Set<Integer> duplicateStatuses = parseIntegerSet(orDefault(properties, "loadTestDuplicateStatuses", "409"));

            return new LoadTestConfig(
                    urlTemplate,
                    method,
                    concurrentUsers,
                    totalRequests,
                    Map.copyOf(headers),
                    bodyTemplate,
                    connectTimeoutMs,
                    requestTimeoutMs,
                    classifierField,
                    parseStringSet(properties.get("loadTestConfirmedValues")),
                    parseStringSet(properties.get("loadTestWaitingValues")),
                    parseStringSet(properties.get("loadTestDuplicateValues")),
                    confirmedStatuses,
                    waitingStatuses,
                    duplicateStatuses,
                    parsePatterns(properties.get("loadTestConfirmedPatterns")),
                    parsePatterns(properties.get("loadTestWaitingPatterns")),
                    parsePatterns(properties.get("loadTestDuplicatePatterns"))
            );
        }

        RequestSpec resolve(int requestIndex) {
            Map<String, String> resolvedHeaders = new LinkedHashMap<>();
            headersTemplate.forEach((key, value) -> resolvedHeaders.put(key, replaceTokens(value, requestIndex)));

            return new RequestSpec(
                    replaceTokens(urlTemplate, requestIndex),
                    resolvedHeaders,
                    replaceTokens(bodyTemplate, requestIndex)
            );
        }

        String method() {
            return method;
        }

        int concurrentUsers() {
            return concurrentUsers;
        }

        int totalRequests() {
            return totalRequests;
        }

        int connectTimeoutMs() {
            return connectTimeoutMs;
        }

        int requestTimeoutMs() {
            return requestTimeoutMs;
        }

        String classifierField() {
            return classifierField;
        }

        Set<String> confirmedValues() {
            return confirmedValues;
        }

        Set<String> waitingValues() {
            return waitingValues;
        }

        Set<String> duplicateValues() {
            return duplicateValues;
        }

        Set<Integer> confirmedStatuses() {
            return confirmedStatuses;
        }

        Set<Integer> waitingStatuses() {
            return waitingStatuses;
        }

        Set<Integer> duplicateStatuses() {
            return duplicateStatuses;
        }

        List<Pattern> confirmedPatterns() {
            return confirmedPatterns;
        }

        List<Pattern> waitingPatterns() {
            return waitingPatterns;
        }

        List<Pattern> duplicatePatterns() {
            return duplicatePatterns;
        }
    }

    record RequestSpec(String url, Map<String, String> headers, String body) {
    }

    enum ResponseCategory {
        CONFIRMED,
        WAITING,
        DUPLICATE,
        OTHER,
        FAILED
    }

    record SingleResult(
            ResponseCategory category,
            int statusCode,
            long latencyMs,
            String responseBody,
            String errorMessage
    ) {
        static SingleResult success(ResponseCategory category, int statusCode, long latencyMs, String responseBody) {
            return new SingleResult(category, statusCode, latencyMs, responseBody, null);
        }

        static SingleResult failure(long latencyMs, String errorMessage) {
            return new SingleResult(ResponseCategory.FAILED, -1, latencyMs, "", errorMessage);
        }
    }

    record LoadTestSummary(
            int confirmedCount,
            int waitingCount,
            int duplicateCount,
            int otherCount,
            int failedCount,
            long averageLatencyMs,
            long p95LatencyMs
    ) {
        static LoadTestSummary fromResults(List<SingleResult> results) {
            int confirmedCount = 0;
            int waitingCount = 0;
            int duplicateCount = 0;
            int otherCount = 0;
            int failedCount = 0;
            List<Long> latencies = new ArrayList<>();

            for (SingleResult result : results) {
                latencies.add(result.latencyMs());
                switch (result.category()) {
                    case CONFIRMED -> confirmedCount++;
                    case WAITING -> waitingCount++;
                    case DUPLICATE -> duplicateCount++;
                    case OTHER -> otherCount++;
                    case FAILED -> failedCount++;
                }
            }

            long averageLatencyMs = latencies.isEmpty()
                    ? 0L
                    : Math.round(latencies.stream().mapToLong(Long::longValue).average().orElse(0));

            long p95LatencyMs = percentile(latencies, 95);

            return new LoadTestSummary(
                    confirmedCount,
                    waitingCount,
                    duplicateCount,
                    otherCount,
                    failedCount,
                    averageLatencyMs,
                    p95LatencyMs
            );
        }

        String render(LoadTestConfig config) {
            StringBuilder builder = new StringBuilder();
            builder.append("Load Test").append(System.lineSeparator()).append(System.lineSeparator());
            builder.append(config.concurrentUsers()).append(" Concurrent Users").append(System.lineSeparator());
            if (config.totalRequests() != config.concurrentUsers()) {
                builder.append(config.totalRequests()).append(" Total Requests").append(System.lineSeparator());
            }
            builder.append(System.lineSeparator());
            builder.append("------------").append(System.lineSeparator()).append(System.lineSeparator());
            builder.append(renderLine("Confirmed", Integer.toString(confirmedCount))).append(System.lineSeparator());
            builder.append(renderLine("Waiting", Integer.toString(waitingCount))).append(System.lineSeparator());
            builder.append(renderLine("Duplicate", Integer.toString(duplicateCount))).append(System.lineSeparator());
            if (otherCount > 0) {
                builder.append(renderLine("Other", Integer.toString(otherCount))).append(System.lineSeparator());
            }
            if (failedCount > 0) {
                builder.append(renderLine("Failed", Integer.toString(failedCount))).append(System.lineSeparator());
            }
            builder.append(renderLine("Avg", averageLatencyMs + "ms")).append(System.lineSeparator());
            builder.append(renderLine("P95", p95LatencyMs + "ms"));
            return builder.toString();
        }

        private static long percentile(List<Long> latencies, int percentile) {
            if (latencies.isEmpty()) {
                return 0L;
            }

            List<Long> sorted = latencies.stream()
                    .sorted(Comparator.naturalOrder())
                    .toList();
            int index = (int) Math.ceil((percentile / 100.0) * sorted.size()) - 1;
            int safeIndex = Math.max(0, Math.min(index, sorted.size() - 1));
            return sorted.get(safeIndex);
        }
    }
}
