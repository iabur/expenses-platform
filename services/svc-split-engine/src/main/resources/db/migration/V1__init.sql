-- Split Engine Service Database Schema

-- Split calculations for expenses
CREATE TABLE split_calculations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    expense_id UUID NOT NULL,
    group_id UUID NOT NULL,
    total_amount_cents BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    split_method VARCHAR(20) NOT NULL, -- EQUAL, PERCENTAGE, EXACT_AMOUNTS, SHARES
    calculation_status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, CALCULATED, FAILED
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    calculated_at TIMESTAMP WITH TIME ZONE,
    error_message TEXT,
    
    CONSTRAINT chk_split_method CHECK (split_method IN ('EQUAL', 'PERCENTAGE', 'EXACT_AMOUNTS', 'SHARES')),
    CONSTRAINT chk_calculation_status CHECK (calculation_status IN ('PENDING', 'CALCULATED', 'FAILED')),
    CONSTRAINT chk_positive_amount CHECK (total_amount_cents > 0)
);

-- Individual participant splits
CREATE TABLE participant_splits (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    split_calculation_id UUID NOT NULL REFERENCES split_calculations(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    split_rule_type VARCHAR(20) NOT NULL, -- EQUAL, PERCENTAGE, EXACT_AMOUNT, SHARES
    split_rule_value DECIMAL(19,4), -- percentage (0-100), exact amount, or share count
    calculated_amount_cents BIGINT,
    currency VARCHAR(3) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    
    CONSTRAINT chk_split_rule_type CHECK (split_rule_type IN ('EQUAL', 'PERCENTAGE', 'EXACT_AMOUNT', 'SHARES')),
    CONSTRAINT chk_percentage_range CHECK (
        split_rule_type != 'PERCENTAGE' OR 
        (split_rule_value >= 0 AND split_rule_value <= 100)
    ),
    CONSTRAINT chk_positive_rule_value CHECK (
        split_rule_value IS NULL OR split_rule_value >= 0
    )
);

-- Group debt balances (optimized for balance queries)
CREATE TABLE group_balances (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id UUID NOT NULL,
    user_id UUID NOT NULL,
    currency VARCHAR(3) NOT NULL,
    balance_cents BIGINT NOT NULL DEFAULT 0, -- positive = owed money, negative = owes money
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    last_expense_id UUID, -- for tracking what caused the last update
    
    UNIQUE(group_id, user_id, currency)
);

-- Debt relationships between users (for settlement optimization)
CREATE TABLE user_debts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id UUID NOT NULL,
    debtor_id UUID NOT NULL, -- who owes money
    creditor_id UUID NOT NULL, -- who is owed money
    currency VARCHAR(3) NOT NULL,
    amount_cents BIGINT NOT NULL,
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    last_expense_id UUID,
    
    UNIQUE(group_id, debtor_id, creditor_id, currency),
    CONSTRAINT chk_different_users CHECK (debtor_id != creditor_id),
    CONSTRAINT chk_positive_debt CHECK (amount_cents > 0)
);

-- Settlement proposals (debt simplification results)
CREATE TABLE settlement_proposals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    group_id UUID NOT NULL,
    proposal_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE, EXECUTED, EXPIRED
    total_transfers INTEGER NOT NULL,
    total_amount_cents BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP WITH TIME ZONE,
    executed_at TIMESTAMP WITH TIME ZONE,
    created_by UUID,
    
    CONSTRAINT chk_proposal_status CHECK (proposal_status IN ('ACTIVE', 'EXECUTED', 'EXPIRED')),
    CONSTRAINT chk_positive_transfers CHECK (total_transfers >= 0)
);

-- Individual transfers in a settlement proposal
CREATE TABLE settlement_transfers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    settlement_proposal_id UUID NOT NULL REFERENCES settlement_proposals(id) ON DELETE CASCADE,
    from_user_id UUID NOT NULL,
    to_user_id UUID NOT NULL,
    amount_cents BIGINT NOT NULL,
    currency VARCHAR(3) NOT NULL,
    transfer_status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, COMPLETED, FAILED
    suggested_method VARCHAR(50), -- BANK_TRANSFER, CASH, VENMO, etc.
    completed_at TIMESTAMP WITH TIME ZONE,
    
    CONSTRAINT chk_transfer_status CHECK (transfer_status IN ('PENDING', 'COMPLETED', 'FAILED')),
    CONSTRAINT chk_different_transfer_users CHECK (from_user_id != to_user_id),
    CONSTRAINT chk_positive_transfer CHECK (amount_cents > 0)
);

-- Indexes for performance
CREATE INDEX idx_split_calculations_expense ON split_calculations(expense_id);
CREATE INDEX idx_split_calculations_group ON split_calculations(group_id);
CREATE INDEX idx_split_calculations_status ON split_calculations(calculation_status);

CREATE INDEX idx_participant_splits_calculation ON participant_splits(split_calculation_id);
CREATE INDEX idx_participant_splits_user ON participant_splits(user_id);

CREATE INDEX idx_group_balances_group_user ON group_balances(group_id, user_id);
CREATE INDEX idx_group_balances_user ON group_balances(user_id);

CREATE INDEX idx_user_debts_group ON user_debts(group_id);
CREATE INDEX idx_user_debts_debtor ON user_debts(debtor_id);
CREATE INDEX idx_user_debts_creditor ON user_debts(creditor_id);

CREATE INDEX idx_settlement_proposals_group ON settlement_proposals(group_id);
CREATE INDEX idx_settlement_proposals_status ON settlement_proposals(proposal_status);

CREATE INDEX idx_settlement_transfers_proposal ON settlement_transfers(settlement_proposal_id);
CREATE INDEX idx_settlement_transfers_users ON settlement_transfers(from_user_id, to_user_id);
