-- Ledger Double-Entry Bookkeeping Schema
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Accounts table (represents user accounts and group accounts)
CREATE TABLE accounts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    account_code VARCHAR(50) UNIQUE NOT NULL, -- e.g., "user:uuid" or "group:uuid"
    owner_type VARCHAR(20) NOT NULL CHECK (owner_type IN ('user', 'group')),
    owner_id UUID NOT NULL, -- Reference to user or group
    currency CHAR(3) NOT NULL,
    account_name VARCHAR(200) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE(owner_type, owner_id, currency)
);

-- Journal entries (groups related postings together)
CREATE TABLE journal_entries (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    reference_type VARCHAR(20) NOT NULL, -- expense, payment, settlement
    reference_id UUID NOT NULL, -- ID of the expense/payment/settlement
    group_id UUID NOT NULL, -- Reference to group for isolation
    description TEXT NOT NULL,
    value_date DATE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by UUID NOT NULL -- Reference to user who created this entry
);

-- Postings table (individual debit/credit entries)
CREATE TABLE postings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    journal_entry_id UUID NOT NULL REFERENCES journal_entries(id) ON DELETE CASCADE,
    debit_account_id UUID REFERENCES accounts(id),
    credit_account_id UUID REFERENCES accounts(id),
    amount_cents BIGINT NOT NULL CHECK (amount_cents > 0),
    currency CHAR(3) NOT NULL,
    fx_rate DECIMAL(18,8), -- Exchange rate if different from account currency
    narrative TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CHECK (
        (debit_account_id IS NOT NULL AND credit_account_id IS NULL) OR
        (debit_account_id IS NULL AND credit_account_id IS NOT NULL)
    )
);

-- Balance snapshots (materialized view for performance)
CREATE TABLE account_balances (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    account_id UUID NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    balance_cents BIGINT NOT NULL DEFAULT 0,
    currency CHAR(3) NOT NULL,
    last_updated TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE(account_id)
);

-- Settlement proposals (computed minimal transfers)
CREATE TABLE settlement_proposals (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    group_id UUID NOT NULL,
    proposal_data JSONB NOT NULL, -- Array of {fromUserId, toUserId, amount, currency}
    total_transfers INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'active', -- active, superseded
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    created_by UUID NOT NULL
);

-- Audit trail for all balance changes
CREATE TABLE balance_audit_log (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    account_id UUID NOT NULL REFERENCES accounts(id),
    journal_entry_id UUID NOT NULL REFERENCES journal_entries(id),
    balance_before_cents BIGINT NOT NULL,
    balance_after_cents BIGINT NOT NULL,
    change_cents BIGINT NOT NULL,
    currency CHAR(3) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Indexes for performance
CREATE INDEX idx_accounts_owner ON accounts(owner_type, owner_id);
CREATE INDEX idx_accounts_currency ON accounts(currency);
CREATE INDEX idx_journal_entries_reference ON journal_entries(reference_type, reference_id);
CREATE INDEX idx_journal_entries_group_id ON journal_entries(group_id);
CREATE INDEX idx_journal_entries_value_date ON journal_entries(value_date);
CREATE INDEX idx_postings_journal_entry_id ON postings(journal_entry_id);
CREATE INDEX idx_postings_debit_account_id ON postings(debit_account_id);
CREATE INDEX idx_postings_credit_account_id ON postings(credit_account_id);
CREATE INDEX idx_postings_created_at ON postings(created_at);
CREATE INDEX idx_account_balances_account_id ON account_balances(account_id);
CREATE INDEX idx_settlement_proposals_group_id ON settlement_proposals(group_id);
CREATE INDEX idx_settlement_proposals_status ON settlement_proposals(status);
CREATE INDEX idx_balance_audit_log_account_id ON balance_audit_log(account_id);
CREATE INDEX idx_balance_audit_log_journal_entry_id ON balance_audit_log(journal_entry_id);

-- Function to update account balances automatically
CREATE OR REPLACE FUNCTION update_account_balance()
RETURNS TRIGGER AS $$
DECLARE
    account_id_to_update UUID;
    current_balance BIGINT;
    new_balance BIGINT;
    balance_change BIGINT;
BEGIN
    -- Handle both debit and credit accounts
    IF NEW.debit_account_id IS NOT NULL THEN
        account_id_to_update := NEW.debit_account_id;
        balance_change := NEW.amount_cents;
    ELSE
        account_id_to_update := NEW.credit_account_id;
        balance_change := -NEW.amount_cents;
    END IF;
    
    -- Get current balance
    SELECT COALESCE(balance_cents, 0) INTO current_balance
    FROM account_balances 
    WHERE account_id = account_id_to_update;
    
    -- Calculate new balance
    new_balance := COALESCE(current_balance, 0) + balance_change;
    
    -- Upsert balance
    INSERT INTO account_balances (account_id, balance_cents, currency, last_updated)
    VALUES (account_id_to_update, new_balance, NEW.currency, NOW())
    ON CONFLICT (account_id) 
    DO UPDATE SET 
        balance_cents = new_balance,
        last_updated = NOW();
    
    -- Log the balance change
    INSERT INTO balance_audit_log (
        account_id, journal_entry_id, balance_before_cents, 
        balance_after_cents, change_cents, currency
    ) VALUES (
        account_id_to_update, NEW.journal_entry_id, 
        COALESCE(current_balance, 0), new_balance, balance_change, NEW.currency
    );
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger to automatically update balances when postings are inserted
CREATE TRIGGER trigger_update_account_balance
    AFTER INSERT ON postings
    FOR EACH ROW
    EXECUTE FUNCTION update_account_balance();

-- Function to validate journal entries (debits = credits)
CREATE OR REPLACE FUNCTION validate_journal_balance()
RETURNS TRIGGER AS $$
DECLARE
    total_debits BIGINT;
    total_credits BIGINT;
BEGIN
    -- Calculate total debits and credits for this journal entry
    SELECT 
        COALESCE(SUM(CASE WHEN debit_account_id IS NOT NULL THEN amount_cents ELSE 0 END), 0),
        COALESCE(SUM(CASE WHEN credit_account_id IS NOT NULL THEN amount_cents ELSE 0 END), 0)
    INTO total_debits, total_credits
    FROM postings 
    WHERE journal_entry_id = NEW.journal_entry_id;
    
    -- Ensure debits equal credits
    IF total_debits != total_credits THEN
        RAISE EXCEPTION 'Journal entry % is unbalanced: debits=% credits=%', 
            NEW.journal_entry_id, total_debits, total_credits;
    END IF;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger to validate journal balance after each posting
CREATE TRIGGER trigger_validate_journal_balance
    AFTER INSERT OR UPDATE ON postings
    FOR EACH ROW
    EXECUTE FUNCTION validate_journal_balance();
