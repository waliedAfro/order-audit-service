CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,

    aggregate_id UUID NOT NULL,

    aggregate_type VARCHAR(100) NOT NULL,

    event_type VARCHAR(255) NOT NULL,

    topic VARCHAR(255) NOT NULL,

    event_key VARCHAR(255) NOT NULL,

    payload TEXT NOT NULL,

    status VARCHAR(30) NOT NULL,

    retry_count INTEGER NOT NULL DEFAULT 0,

    next_retry_at TIMESTAMP WITH TIME ZONE,

    last_error TEXT,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL,

    processed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_payment_outbox_status_retry
ON outbox_events(status, next_retry_at);