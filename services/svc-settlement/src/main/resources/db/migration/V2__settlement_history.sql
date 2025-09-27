-- Settlement History Table
-- This table tracks all settlement-related activities for audit and history purposes

CREATE TABLE settlement_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    group_id UUID NOT NULL,
    proposal_id UUID,
    payment_id UUID,
    user_id UUID NOT NULL,
    action_type VARCHAR(20) NOT NULL,
    entity_type VARCHAR(20) NOT NULL,
    description VARCHAR(1000),
    amount_cents BIGINT,
    currency VARCHAR(3),
    status VARCHAR(20),
    metadata VARCHAR(2000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Add constraints
ALTER TABLE settlement_history 
    ADD CONSTRAINT chk_action_type CHECK (action_type IN (
        'CREATED', 'UPDATED', 'ACCEPTED', 'REJECTED', 'CANCELLED', 
        'COMPLETED', 'CONFIRMED', 'DISPUTED', 'EXPIRED'
    ));

ALTER TABLE settlement_history 
    ADD CONSTRAINT chk_entity_type CHECK (entity_type IN (
        'PROPOSAL', 'PAYMENT', 'CONFIRMATION', 'DISPUTE'
    ));

-- Add foreign key constraints
ALTER TABLE settlement_history 
    ADD CONSTRAINT fk_settlement_history_group 
    FOREIGN KEY (group_id) REFERENCES groups(id) ON DELETE CASCADE;

ALTER TABLE settlement_history 
    ADD CONSTRAINT fk_settlement_history_proposal 
    FOREIGN KEY (proposal_id) REFERENCES settlement_proposals(id) ON DELETE CASCADE;

ALTER TABLE settlement_history 
    ADD CONSTRAINT fk_settlement_history_payment 
    FOREIGN KEY (payment_id) REFERENCES settlement_payments(id) ON DELETE CASCADE;

-- Add indexes for performance
CREATE INDEX idx_settlement_history_group_id ON settlement_history(group_id);
CREATE INDEX idx_settlement_history_proposal_id ON settlement_history(proposal_id);
CREATE INDEX idx_settlement_history_payment_id ON settlement_history(payment_id);
CREATE INDEX idx_settlement_history_user_id ON settlement_history(user_id);
CREATE INDEX idx_settlement_history_created_at ON settlement_history(created_at);
CREATE INDEX idx_settlement_history_action_type ON settlement_history(action_type);
CREATE INDEX idx_settlement_history_entity_type ON settlement_history(entity_type);

-- Composite indexes for common queries
CREATE INDEX idx_settlement_history_group_created ON settlement_history(group_id, created_at DESC);
CREATE INDEX idx_settlement_history_group_status ON settlement_history(group_id, status);
CREATE INDEX idx_settlement_history_user_group ON settlement_history(user_id, group_id, created_at DESC);
