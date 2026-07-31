CREATE TABLE jobs (
  id            UUID PRIMARY KEY,
  client_id     TEXT NOT NULL,
  callback_url  TEXT NOT NULL,
  payload       JSONB,
  scheduled_at  TIMESTAMPTZ NOT NULL,
  status        TEXT NOT NULL DEFAULT 'PENDING',
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_jobs_due ON jobs (scheduled_at) WHERE status = 'PENDING';
