package com.capstone.bwlovers.ops.incident;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

interface IncidentJobStore {
    JsonNode enqueue(List<ObjectNode> events, Instant now);
    Optional<ObjectNode> claim(Instant now);
    boolean save(ObjectNode job, Instant now);
    Optional<ObjectNode> find(String id);
    boolean retry(String id, Instant now);
}
