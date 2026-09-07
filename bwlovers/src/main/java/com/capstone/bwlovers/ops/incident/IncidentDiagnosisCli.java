package com.capstone.bwlovers.ops.incident;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import static com.capstone.bwlovers.ops.incident.IncidentJson.*;

public final class IncidentDiagnosisCli {
    private static final String HELP = """
            장애 진단 CLI (Spring/DB 없이 실행)
            ./gradlew incidentDiagnosis --args='<command> [options]'

            analyze --input incident.json --output 새결과디렉터리
                    [--mode multi|single-log] [--omit memoryUsageMb,memoryLimitMb]
                    [--prompt-version incident-v1|incident-v2]
                    [--dry-run] [--send-discord]
            send --report 결과디렉터리/report.json --output 새전송결과.json
            timer-start --input incident.json --method MANUAL|AI_ASSISTED --trial 1 --session 새세션.json
                        [--measurement-scope END_TO_END|REVIEW_ONLY]
            timer-stop --session 세션.json --notes 검토기록.json
            summarize --input 측정디렉터리 --output 새요약.json
            evaluate --reports 진단디렉터리 --expectations 기대후보.json --output 새검증결과.json

            환경 변수: OPENAI_API_KEY, OPENAI_INCIDENT_MODEL (기본 gpt-4.1-mini), DISCORD_INCIDENT_WEBHOOK_URL
            --dry-run은 마스킹된 입력과 요청만 저장하며 외부 API를 호출하지 않습니다.
            Discord 전송은 --send-discord 또는 send 명령으로만 실행합니다. 실제 설정 변경 권한은 없습니다.
            상세 절차: ../docs/INCIDENT_DIAGNOSIS_MVP.md
            """;

    private IncidentDiagnosisCli() { }

    public static void main(String[] args) {
        try {
            run(args, System.getenv(), IncidentHttp.create());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("실행이 중단됐습니다. 저장된 결과와 Discord 채널을 확인하세요.");
            System.exit(1);
        } catch (IllegalArgumentException e) {
            System.err.println("실패: " + e.getMessage());
            System.exit(1);
        } catch (FileAlreadyExistsException e) {
            System.err.println("출력 경로가 이미 존재합니다. 새 경로를 지정하세요.");
            System.exit(1);
        } catch (IncidentHttp.Failure e) {
            System.err.println(e.getMessage());
            System.exit(1);
        } catch (IOException e) {
            // File paths, URLs and provider response bodies may contain secrets.
            System.err.println("파일 접근 또는 외부 API 호출에 실패했습니다. 출력 파일·환경 변수·연결 상태를 확인하세요.");
            System.exit(1);
        }
    }

    static void run(String[] args, Map<String, String> env, IncidentHttp http) throws IOException, InterruptedException {
        if (args.length == 0 || Set.of("help", "--help").contains(args[0])) { System.out.println(HELP); return; }
        Map<String, String> options = options(Arrays.copyOfRange(args, 1, args.length));
        switch (args[0]) {
            case "analyze" -> {
                allow(options, "input", "output", "mode", "omit", "dry-run", "send-discord", "prompt-version");
                analyze(options, env, http);
            }
            case "send" -> {
                allow(options, "report", "output");
                JsonNode report = read(path(options, "report"));
                require(Set.of("SIMULATION", "HISTORICAL", "LIVE").contains(report.path("sourceType").asText()), "진단 보고서의 출처 구분이 없습니다.");
                text(report, "incidentId", 100);
                text(report, "mode", 20);
                require(report.path("omittedMetrics").isArray(), "진단 보고서의 생략 지표 목록이 없습니다.");
                new IncidentAnalyzer(http, null, model(env), report.path("promptVersion").asText(IncidentAnalyzer.PROMPT_VERSION))
                        .validateDiagnosis(report.path("diagnosis"), report.path("analyzedInput"));
                deliver(http, IncidentDiscord.webhook(env.get("DISCORD_INCIDENT_WEBHOOK_URL")),
                        IncidentInput.redact(report), path(options, "output"));
            }
            case "timer-start" -> {
                allow(options, "input", "method", "trial", "session", "measurement-scope");
                int trial;
                try { trial = Integer.parseInt(required(options, "trial")); }
                catch (NumberFormatException e) { throw new IllegalArgumentException("trial은 정수여야 합니다."); }
                IncidentBenchmark.start(path(options, "input"), required(options, "method"), trial, path(options, "session"),
                        options.getOrDefault("measurement-scope", "END_TO_END"));
                System.out.println("측정을 시작했습니다. 원인 후보와 점검 항목을 작성한 뒤 timer-stop을 실행하세요.");
            }
            case "timer-stop" -> {
                allow(options, "session", "notes");
                IncidentBenchmark.stop(path(options, "session"), path(options, "notes"));
                System.out.println("담당자 검토 완료 시간과 작성한 내용을 측정 파일에 저장했습니다.");
            }
            case "summarize" -> {
                allow(options, "input", "output");
                JsonNode summary = IncidentBenchmark.summarize(path(options, "input"));
                writeNew(path(options, "output"), summary);
                System.out.println(MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(summary));
            }
            case "evaluate" -> {
                allow(options, "reports", "expectations", "output");
                writeNew(path(options, "output"), IncidentEvaluation.evaluate(path(options, "reports"), path(options, "expectations")));
                System.out.println("기대 후보와 실제 AI 후보의 비교 결과를 저장했습니다. 근거의 해석은 담당자가 검토해야 합니다.");
            }
            default -> throw new IllegalArgumentException("지원하지 않는 명령입니다. --help를 확인하세요.");
        }
    }

    private static void analyze(Map<String, String> options, Map<String, String> env, IncidentHttp http) throws IOException, InterruptedException {
        boolean dry = options.containsKey("dry-run"), send = options.containsKey("send-discord");
        require(!(dry && send), "--dry-run과 --send-discord는 함께 사용할 수 없습니다.");
        String mode = options.getOrDefault("mode", "multi");
        Set<String> omitted = options.containsKey("omit") ? new TreeSet<>(Arrays.asList(options.get("omit").split(",", -1))) : Set.of();
        JsonNode raw = read(path(options, "input"));
        ObjectNode input = IncidentInput.prepare(raw, mode, omitted);
        IncidentAnalyzer analyzer = new IncidentAnalyzer(http, env.get("OPENAI_API_KEY"), model(env),
                options.getOrDefault("prompt-version", IncidentAnalyzer.PROMPT_VERSION));
        if (!dry) require(env.containsKey("OPENAI_API_KEY") && !env.get("OPENAI_API_KEY").isBlank(), "OPENAI_API_KEY가 필요합니다. 입력 확인에는 --dry-run을 사용하세요.");
        URI webhook = send ? IncidentDiscord.webhook(env.get("DISCORD_INCIDENT_WEBHOOK_URL")) : null;
        Path output = path(options, "output").toAbsolutePath();
        Files.createDirectories(output.getParent());
        Files.createDirectory(output);
        writeNew(output.resolve("request.json"), analyzer.request(input));
        if (dry) { System.out.println("입력 검증 완료. 마스킹된 request.json을 저장했습니다. 외부 API 호출은 없습니다."); return; }
        Instant started = Instant.now();
        long tick = System.nanoTime();
        IncidentAnalyzer.Result result = analyzer.analyze(input);
        long duration = (System.nanoTime() - tick) / 1_000_000;
        ObjectNode report = IncidentReport.create(raw, input, mode, omitted, result, started, duration, analyzer.promptVersion());
        writeNew(output.resolve("report.json"), report);
        writeTextNew(output.resolve("report.md"), IncidentReport.markdown(report));
        writeNew(output.resolve("discord-preview.json"), IncidentReport.discordPayload(report));
        System.out.println("진단과 Discord 미리보기를 저장했습니다. 담당자 검토가 필요합니다.");
        if (send) deliver(http, webhook, report, output.resolve("delivery.json"));
    }

    private static void deliver(IncidentHttp http, URI webhook, JsonNode report, Path receipt) throws IOException, InterruptedException {
        // Reserve the receipt before the external side effect, so an existing path cannot trigger a duplicate send.
        ObjectNode delivery = object().put("incidentId", report.path("incidentId").asText())
                .put("attemptedAt", Instant.now().toString()).put("status", "ATTEMPTING")
                .put("note", "전송 실패/시간 초과 시 채널을 확인한 뒤 별도 경로로 재시도하세요.");
        writeNew(receipt, delivery);
        try {
            String id = IncidentDiscord.send(http, webhook, report);
            delivery.put("status", "SENT").put("messageId", id);
            System.out.println("Discord 메시지 전송을 확인했습니다.");
        } catch (IOException | InterruptedException | IllegalArgumentException e) {
            delivery.put("status", "FAILED_OR_UNKNOWN");
            delivery.put("failureReason", e instanceof IncidentHttp.Failure ? e.getMessage() : "전송 또는 응답 확인 실패");
            throw e;
        } finally {
            Files.writeString(receipt, MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(delivery) + "\n");
        }
    }

    private static String model(Map<String, String> env) { return env.getOrDefault("OPENAI_INCIDENT_MODEL", "gpt-4.1-mini"); }

    private static Map<String, String> options(String[] args) {
        Map<String, String> options = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            require(args[i].startsWith("--"), "옵션은 --이름 값 형식이어야 합니다.");
            String key = args[i].substring(2);
            require(!options.containsKey(key), "중복 옵션이 있습니다.");
            if (Set.of("dry-run", "send-discord").contains(key)) options.put(key, "true");
            else {
                require(i + 1 < args.length && !args[i + 1].startsWith("--"), "옵션 값이 누락됐습니다.");
                options.put(key, args[++i]);
            }
        }
        return options;
    }

    private static void allow(Map<String, String> options, String... allowed) {
        require(Set.of(allowed).containsAll(options.keySet()), "이 명령에서 지원하지 않는 옵션이 있습니다.");
    }

    private static String required(Map<String, String> options, String key) {
        require(options.containsKey(key) && !options.get(key).isBlank(), "--" + key + " 옵션이 필요합니다.");
        return options.get(key);
    }

    private static Path path(Map<String, String> options, String key) { return Path.of(required(options, key)); }
}
