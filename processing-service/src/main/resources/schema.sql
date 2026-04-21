CREATE TABLE IF NOT EXISTS processing_jobs (
    id UUID PRIMARY KEY,
    file_id UUID NOT NULL UNIQUE,
    filename VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'QUEUED',
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    error_message TEXT,
    row_count INT,
    valid_rows INT,
    invalid_rows INT,
    created_at TIMESTAMP NOT NULL
);