package com.capstone.bwlovers.ops.incident;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.capstone.bwlovers.ops.incident.IncidentJson.*;

final class RedisIncidentJobStore implements IncidentJobStore {
    static final int RETENTION_SECONDS = 86400;
    static final int LEASE_MILLIS = 600_000;
    private final StringRedisTemplate redis;
    private final String pending;
    private final String prefix;
    private final int capacity;

    // All keys share a hash tag. No untrusted text is used as a Redis key suffix except a SHA-256 ID.
    RedisIncidentJobStore(StringRedisTemplate redis) { this(redis, "bwlovers:{incident}:", 100); }
    RedisIncidentJobStore(StringRedisTemplate redis, String namespace, int capacity) {
        this.redis = redis; this.pending = namespace + "pending"; this.prefix = namespace + "job:"; this.capacity = capacity;
    }

    private static final String ENQUEUE = """
            local events = cjson.decode(ARGV[1])
            local count, seen = 0, {}
            for _, e in ipairs(events) do
              if not seen[e.jobId] and redis.call('EXISTS', ARGV[5] .. e.jobId) == 0 then count = count + 1 end
              seen[e.jobId] = true
            end
            if count > 0 and redis.call('ZCARD', KEYS[1]) + count > tonumber(ARGV[4]) then return 'FULL' end
            local result = {}
            for _, e in ipairs(events) do
              local key = ARGV[5] .. e.jobId
              local value = redis.call('GET', key)
              local created = false
              if not value then
                value = cjson.encode({jobId=e.jobId,status='QUEUED',attempts=0,event=e})
                redis.call('SET', key, value, 'EX', ARGV[3])
                redis.call('ZADD', KEYS[1], ARGV[2], e.jobId)
                created = true
              end
              table.insert(result, {jobId=e.jobId,status=cjson.decode(value).status,created=created})
            end
            return cjson.encode(result)
            """;

    private static final String CLAIM = """
            local ids = redis.call('ZRANGEBYSCORE', KEYS[1], '-inf', ARGV[1], 'LIMIT', 0, 25)
            for _, id in ipairs(ids) do
              local key = ARGV[4] .. id
              local value = redis.call('GET', key)
              if not value then redis.call('ZREM', KEYS[1], id)
              else
                local job = cjson.decode(value)
                if job.status == 'DELIVERING' or job.attempts >= 3 then
                  if job.status == 'DELIVERING' then job.status = 'DELIVERY_UNKNOWN'
                  else job.status = 'FAILED' end
                  job.failureReason = 'WORKER_LEASE_EXPIRED'
                  job.owner = nil
                  redis.call('SET', key, cjson.encode(job), 'EX', ARGV[5])
                  redis.call('ZREM', KEYS[1], id)
                else
                  job.status = 'PROCESSING'
                  job.owner = ARGV[3]
                  job.attempts = job.attempts + 1
                  redis.call('SET', key, cjson.encode(job), 'EX', ARGV[5])
                  redis.call('ZADD', KEYS[1], tonumber(ARGV[1]) + tonumber(ARGV[2]), id)
                  return cjson.encode(job)
                end
              end
            end
            return nil
            """;

    private static final String SAVE = """
            local value = redis.call('GET', KEYS[2])
            local score = redis.call('ZSCORE', KEYS[1], ARGV[1])
            if not value or not score or tonumber(score) <= tonumber(ARGV[4]) then return 0 end
            local old = cjson.decode(value)
            if old.owner ~= ARGV[2] then return 0 end
            local job = cjson.decode(ARGV[3])
            if job.status == 'SENT' or job.status == 'FAILED' or job.status == 'DELIVERY_UNKNOWN' then
              job.owner = nil
              redis.call('ZREM', KEYS[1], ARGV[1])
            else redis.call('ZADD', KEYS[1], tonumber(ARGV[4]) + tonumber(ARGV[6]), ARGV[1]) end
            redis.call('SET', KEYS[2], cjson.encode(job), 'EX', ARGV[5])
            return 1
            """;

    private static final String RETRY = """
            local value = redis.call('GET', KEYS[2])
            if not value then return 0 end
            local job = cjson.decode(value)
            if job.status ~= 'FAILED' and job.status ~= 'DELIVERY_UNKNOWN' then return 0 end
            if redis.call('ZCARD', KEYS[1]) >= tonumber(ARGV[3]) then return 0 end
            job.status = 'QUEUED'
            job.attempts = 0
            job.failureReason = nil
            job.owner = nil
            redis.call('SET', KEYS[2], cjson.encode(job), 'EX', ARGV[2])
            redis.call('ZADD', KEYS[1], ARGV[1], job.jobId)
            return 1
            """;

    @Override public JsonNode enqueue(List<ObjectNode> events, Instant now) {
        if (events.isEmpty()) return MAPPER.createArrayNode();
        String result = redis.execute(new DefaultRedisScript<>(ENQUEUE, String.class), List.of(pending),
                MAPPER.valueToTree(events).toString(), Long.toString(now.toEpochMilli()), Integer.toString(RETENTION_SECONDS), Integer.toString(capacity), prefix);
        if ("FULL".equals(result)) throw new IncidentQueueFullException();
        if (result == null) throw new IllegalStateException("작업 저장 응답 없음");
        return parse(result);
    }

    @Override public Optional<ObjectNode> claim(Instant now) {
        String result = redis.execute(new DefaultRedisScript<>(CLAIM, String.class), List.of(pending),
                Long.toString(now.toEpochMilli()), Integer.toString(LEASE_MILLIS), UUID.randomUUID().toString(), prefix, Integer.toString(RETENTION_SECONDS));
        return Optional.ofNullable(result).map(text -> (ObjectNode) parse(text));
    }

    @Override public boolean save(ObjectNode job, Instant now) {
        String id = text(job, "jobId", 64);
        Long result = redis.execute(new DefaultRedisScript<>(SAVE, Long.class), List.of(pending, prefix + id), id,
                text(job, "owner", 100), job.toString(), Long.toString(now.toEpochMilli()), Integer.toString(RETENTION_SECONDS), Integer.toString(LEASE_MILLIS));
        return Long.valueOf(1).equals(result);
    }

    @Override public Optional<ObjectNode> find(String id) {
        require(id.matches("[a-f0-9]{64}"), "jobId 형식이 올바르지 않습니다.");
        return Optional.ofNullable(redis.opsForValue().get(prefix + id)).map(value -> (ObjectNode) parse(value));
    }

    @Override public boolean retry(String id, Instant now) {
        require(id.matches("[a-f0-9]{64}"), "jobId 형식이 올바르지 않습니다.");
        Long result = redis.execute(new DefaultRedisScript<>(RETRY, Long.class), List.of(pending, prefix + id),
                Long.toString(now.toEpochMilli()), Integer.toString(RETENTION_SECONDS), Integer.toString(capacity));
        return Long.valueOf(1).equals(result);
    }
}

final class IncidentQueueFullException extends RuntimeException { }
