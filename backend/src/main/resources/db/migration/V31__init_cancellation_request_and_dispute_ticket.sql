-- FIN-16, PRD §12.5: cancellation-request state machine (policy check →
-- refund/penalty calculation → approval-if-penalized → refund-processed)
-- and a tracked dispute ticket entity — see CancellationRequest.java /
-- DisputeTicket.java for the full reasoning.

CREATE TABLE cancellation_request (
    cancellation_request_id UUID PRIMARY KEY,
    booking_id               UUID NOT NULL,
    consultant_id             UUID NOT NULL,
    refund_amount             NUMERIC(19,4) NOT NULL,
    refund_currency           VARCHAR(10) NOT NULL,
    penalty_amount            NUMERIC(19,4) NOT NULL,
    penalty_currency          VARCHAR(10) NOT NULL,
    status                    VARCHAR(20) NOT NULL,
    created_at                TIMESTAMP WITH TIME ZONE NOT NULL,
    approved_at               TIMESTAMP WITH TIME ZONE,
    refunded_at               TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_cancellation_request_booking_id ON cancellation_request (booking_id);
CREATE INDEX idx_cancellation_request_consultant_id ON cancellation_request (consultant_id);

CREATE TABLE dispute_ticket (
    dispute_ticket_id UUID PRIMARY KEY,
    booking_id         UUID NOT NULL,
    consultant_id       UUID NOT NULL,
    reason              VARCHAR(2000) NOT NULL,
    status              VARCHAR(20) NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_dispute_ticket_booking_id ON dispute_ticket (booking_id);
CREATE INDEX idx_dispute_ticket_consultant_id ON dispute_ticket (consultant_id);
