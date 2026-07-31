package com.j143.clockwork.store.jdbc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.j143.clockwork.core.Job;
import com.j143.clockwork.core.JobStatus;
import com.j143.clockwork.store.JobRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcJobRepository implements JobRepository {
    private static final String CLAIM_SQL = """
            WITH cte AS (
              SELECT id FROM jobs
              WHERE status = 'PENDING' AND scheduled_at <= now()
              ORDER BY scheduled_at
              LIMIT ?
              FOR UPDATE SKIP LOCKED
            )
            UPDATE jobs
            SET status = 'IN_PROGRESS'
            FROM cte
            WHERE jobs.id = cte.id
            RETURNING jobs.*
            """;

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public JdbcJobRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void insert(Job job) {
        String payload = writePayload(job.payload());
        jdbcTemplate.update("""
                        INSERT INTO jobs(id, client_id, callback_url, payload, scheduled_at, status, created_at)
                        VALUES (?, ?, ?, CAST(? AS JSONB), ?, ?, ?)
                        """,
                job.id(),
                job.clientId(),
                job.callbackUrl(),
                payload,
                Timestamp.from(job.scheduledAt()),
                job.status().name(),
                Timestamp.from(job.createdAt()));
    }

    @Override
    @Transactional
    public List<Job> claimDueJobs(int limit) {
        return jdbcTemplate.query(CLAIM_SQL, rowMapper(), limit);
    }

    @Override
    public void updateStatus(UUID id, JobStatus status) {
        jdbcTemplate.update("UPDATE jobs SET status = ? WHERE id = ?", status.name(), id);
    }

    @Override
    public Optional<Job> findById(UUID id) {
        List<Job> jobs = jdbcTemplate.query("SELECT * FROM jobs WHERE id = ?", rowMapper(), id);
        return jobs.stream().findFirst();
    }

    private RowMapper<Job> rowMapper() {
        return (rs, rowNum) -> new Job(
                rs.getObject("id", UUID.class),
                rs.getString("client_id"),
                rs.getString("callback_url"),
                readPayload(rs),
                rs.getTimestamp("scheduled_at").toInstant(),
                JobStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("created_at").toInstant()
        );
    }

    private JsonNode readPayload(ResultSet resultSet) throws SQLException {
        String payload = resultSet.getString("payload");
        if (payload == null) {
            return null;
        }
        try {
            return objectMapper.readTree(payload);
        } catch (JsonProcessingException e) {
            throw new SQLException("Invalid JSON payload in database", e);
        }
    }

    private String writePayload(JsonNode payload) {
        if (payload == null) {
            return "null";
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new DataAccessException("Failed to serialize payload", e) {
            };
        }
    }
}
