-- Settlement Service Database Schema

-- Settlement proposals for debt optimization
CREATE TABLE settlement_proposals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id UUID NOT NULL,
    proposer_id UUID NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    currency VARCHAR(3) NOT NULL,
    total_amount_cents BIGINT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    proposal_type VARCHAR(30) NOT NULL DEFAULT 'DEBT_SIMPLIFICATION',
    auto_generated BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_settlement_status CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'EXPIRED', 'COMPLETED', 'CANCELLED')),
    CONSTRAINT chk_proposal_type CHECK (proposal_type IN ('DEBT_SIMPLIFICATION', 'MANUAL_SETTLEMENT', 'GROUP_BALANCE', 'PARTIAL_PAYMENT')),
    CONSTRAINT chk_positive_amount CHECK (total_amount_cents >= 0)
);

-- Individual payments within a settlement proposal
CREATE TABLE settlement_payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    proposal_id UUID NOT NULL REFERENCES settlement_proposals(id) ON DELETE CASCADE,
    payer_id UUID NOT NULL,
    payee_id UUID NOT NULL,
    currency VARCHAR(3) NOT NULL,
    amount_cents BIGINT NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    payment_method VARCHAR(50),
    payment_reference VARCHAR(100),
    due_date TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_payment_status CHECK (status IN ('PENDING', 'CONFIRMED', 'COMPLETED', 'FAILED', 'CANCELLED')),
    CONSTRAINT chk_positive_payment_amount CHECK (amount_cents > 0),
    CONSTRAINT chk_different_users CHECK (payer_id != payee_id),
    
    UNIQUE(proposal_id, payer_id, payee_id)
);

-- Payment confirmations and disputes
CREATE TABLE payment_confirmations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payment_id UUID NOT NULL REFERENCES settlement_payments(id) ON DELETE CASCADE,
    confirmer_id UUID NOT NULL,
    confirmation_type VARCHAR(20) NOT NULL,
    notes TEXT,
    attachment_url VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_confirmation_type CHECK (confirmation_type IN ('PAYER_CONFIRMED', 'PAYEE_CONFIRMED', 'DISPUTED', 'RESOLVED'))
);

-- Settlement templates for recurring settlements
CREATE TABLE settlement_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id UUID NOT NULL,
    creator_id UUID NOT NULL,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    template_type VARCHAR(30) NOT NULL,
    schedule_pattern VARCHAR(50), -- WEEKLY, MONTHLY, etc.
    auto_create BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_template_type CHECK (template_type IN ('RECURRING_SETTLEMENT', 'MILESTONE_SETTLEMENT', 'CUSTOM'))
);

-- Template payment rules
CREATE TABLE template_payment_rules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    template_id UUID NOT NULL REFERENCES settlement_templates(id) ON DELETE CASCADE,
    payer_id UUID NOT NULL,
    payee_id UUID NOT NULL,
    amount_cents BIGINT,
    percentage DECIMAL(5,2), -- For percentage-based rules
    priority INTEGER NOT NULL DEFAULT 1,
    conditions JSONB, -- Flexible conditions
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    CONSTRAINT chk_amount_or_percentage CHECK (
        (amount_cents IS NOT NULL AND percentage IS NULL) OR 
        (amount_cents IS NULL AND percentage IS NOT NULL)
    ),
    CONSTRAINT chk_valid_percentage CHECK (percentage IS NULL OR (percentage >= 0 AND percentage <= 100))
);

-- Debt optimization cache for performance
CREATE TABLE debt_snapshots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id UUID NOT NULL,
    snapshot_date DATE NOT NULL,
    currency VARCHAR(3) NOT NULL,
    total_debt_cents BIGINT NOT NULL DEFAULT 0,
    simplified_transactions_count INTEGER NOT NULL DEFAULT 0,
    original_transactions_count INTEGER NOT NULL DEFAULT 0,
    optimization_data JSONB, -- Detailed optimization results
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE(group_id, snapshot_date, currency)
);

-- Indexes for performance
CREATE INDEX idx_settlement_proposals_group_id ON settlement_proposals(group_id);
CREATE INDEX idx_settlement_proposals_status ON settlement_proposals(status);
CREATE INDEX idx_settlement_proposals_created_at ON settlement_proposals(created_at);
CREATE INDEX idx_settlement_proposals_expires_at ON settlement_proposals(expires_at) WHERE expires_at IS NOT NULL;

CREATE INDEX idx_settlement_payments_proposal_id ON settlement_payments(proposal_id);
CREATE INDEX idx_settlement_payments_payer_id ON settlement_payments(payer_id);
CREATE INDEX idx_settlement_payments_payee_id ON settlement_payments(payee_id);
CREATE INDEX idx_settlement_payments_status ON settlement_payments(status);
CREATE INDEX idx_settlement_payments_due_date ON settlement_payments(due_date) WHERE due_date IS NOT NULL;

CREATE INDEX idx_payment_confirmations_payment_id ON payment_confirmations(payment_id);
CREATE INDEX idx_payment_confirmations_confirmer_id ON payment_confirmations(confirmer_id);

CREATE INDEX idx_settlement_templates_group_id ON settlement_templates(group_id);
CREATE INDEX idx_settlement_templates_active ON settlement_templates(is_active) WHERE is_active = true;

CREATE INDEX idx_template_payment_rules_template_id ON template_payment_rules(template_id);

CREATE INDEX idx_debt_snapshots_group_id ON debt_snapshots(group_id);
CREATE INDEX idx_debt_snapshots_date ON debt_snapshots(snapshot_date);

-- Triggers for updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_settlement_proposals_updated_at 
    BEFORE UPDATE ON settlement_proposals 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_settlement_payments_updated_at 
    BEFORE UPDATE ON settlement_payments 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_settlement_templates_updated_at 
    BEFORE UPDATE ON settlement_templates 
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
