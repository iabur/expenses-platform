-- Ledger Service Database Schema
-- Double-entry bookkeeping system for expense tracking

-- Accounts table - represents financial accounts for users and groups
CREATE TABLE accounts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_code VARCHAR(100) UNIQUE NOT NULL,
    owner_type VARCHAR(20) NOT NULL CHECK (owner_type IN ('USER', 'GROUP', 'SYSTEM')),
    owner_id UUID NOT NULL,
    currency VARCHAR(3) NOT NULL,
    account_name VARCHAR(255) NOT NULL,
    description TEXT,
    is_active BOOLEAN DEFAULT true NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    
    -- Unique constraint: one account per owner per currency
    UNIQUE(owner_type, owner_id, currency)
);

-- Account balances table - current balance for each account
CREATE TABLE account_balances (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    account_id UUID UNIQUE NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    balance_cents BIGINT DEFAULT 0 NOT NULL,
    last_updated TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    last_transaction_id UUID,
    version BIGINT DEFAULT 0 NOT NULL,
    
    CHECK (balance_cents IS NOT NULL)
);

-- Journal entries table - represents financial transactions
CREATE TABLE journal_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reference_type VARCHAR(20) NOT NULL CHECK (reference_type IN ('EXPENSE', 'PAYMENT', 'SETTLEMENT', 'ADJUSTMENT')),
    reference_id UUID NOT NULL,
    group_id UUID NOT NULL,
    description TEXT NOT NULL,
    value_date DATE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    created_by UUID NOT NULL
);

-- Postings table - individual debit/credit entries (double-entry bookkeeping)
CREATE TABLE postings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    journal_entry_id UUID NOT NULL REFERENCES journal_entries(id) ON DELETE CASCADE,
    debit_account_id UUID REFERENCES accounts(id),
    credit_account_id UUID REFERENCES accounts(id),
    amount_cents BIGINT NOT NULL CHECK (amount_cents > 0),
    currency VARCHAR(3) NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW() NOT NULL,
    
    -- Constraint: must have exactly one of debit or credit account
    CHECK (
        (debit_account_id IS NOT NULL AND credit_account_id IS NULL) OR
        (debit_account_id IS NULL AND credit_account_id IS NOT NULL)
    )
);

-- Indexes for performance
CREATE INDEX idx_accounts_owner ON accounts(owner_type, owner_id);
CREATE INDEX idx_accounts_currency ON accounts(currency);
CREATE INDEX idx_accounts_active ON accounts(is_active) WHERE is_active = true;
CREATE INDEX idx_accounts_code ON accounts(account_code);

CREATE INDEX idx_account_balances_account ON account_balances(account_id);
CREATE INDEX idx_account_balances_updated ON account_balances(last_updated DESC);

CREATE INDEX idx_journal_entries_reference ON journal_entries(reference_type, reference_id);
CREATE INDEX idx_journal_entries_group ON journal_entries(group_id);
CREATE INDEX idx_journal_entries_date ON journal_entries(value_date DESC);
CREATE INDEX idx_journal_entries_created_by ON journal_entries(created_by);

CREATE INDEX idx_postings_journal ON postings(journal_entry_id);
CREATE INDEX idx_postings_debit_account ON postings(debit_account_id);
CREATE INDEX idx_postings_credit_account ON postings(credit_account_id);
CREATE INDEX idx_postings_currency ON postings(currency);
CREATE INDEX idx_postings_created_at ON postings(created_at DESC);

-- Create system accounts for common scenarios
INSERT INTO accounts (account_code, owner_type, owner_id, currency, account_name, description) VALUES
-- System accounts for different currencies
('SYSTEM:EXPENSES:USD', 'SYSTEM', '00000000-0000-0000-0000-000000000001', 'USD', 'System Expenses USD', 'System account for USD expenses'),
('SYSTEM:EXPENSES:EUR', 'SYSTEM', '00000000-0000-0000-0000-000000000001', 'EUR', 'System Expenses EUR', 'System account for EUR expenses'),
('SYSTEM:EXPENSES:BDT', 'SYSTEM', '00000000-0000-0000-0000-000000000001', 'BDT', 'System Expenses BDT', 'System account for BDT expenses'),
('SYSTEM:SETTLEMENTS:USD', 'SYSTEM', '00000000-0000-0000-0000-000000000002', 'USD', 'System Settlements USD', 'System account for USD settlements'),
('SYSTEM:SETTLEMENTS:EUR', 'SYSTEM', '00000000-0000-0000-0000-000000000002', 'EUR', 'System Settlements EUR', 'System account for EUR settlements'),
('SYSTEM:SETTLEMENTS:BDT', 'SYSTEM', '00000000-0000-0000-0000-000000000002', 'BDT', 'System Settlements BDT', 'System account for BDT settlements');

-- Create corresponding account balances for system accounts
INSERT INTO account_balances (account_id, balance_cents, last_updated) 
SELECT id, 0, NOW() FROM accounts WHERE owner_type = 'SYSTEM';

-- Create function to automatically create account balance when account is created
CREATE OR REPLACE FUNCTION create_account_balance()
RETURNS TRIGGER AS $$
BEGIN
    INSERT INTO account_balances (account_id, balance_cents, last_updated)
    VALUES (NEW.id, 0, NOW());
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger to automatically create account balance
CREATE TRIGGER trigger_create_account_balance
    AFTER INSERT ON accounts
    FOR EACH ROW
    EXECUTE FUNCTION create_account_balance();

-- Create function to update account balance when posting changes
CREATE OR REPLACE FUNCTION update_account_balance()
RETURNS TRIGGER AS $$
BEGIN
    -- Handle INSERT
    IF TG_OP = 'INSERT' THEN
        -- Update debit account balance
        IF NEW.debit_account_id IS NOT NULL THEN
            UPDATE account_balances 
            SET balance_cents = balance_cents + NEW.amount_cents,
                last_updated = NOW(),
                last_transaction_id = NEW.journal_entry_id
            WHERE account_id = NEW.debit_account_id;
        END IF;
        
        -- Update credit account balance
        IF NEW.credit_account_id IS NOT NULL THEN
            UPDATE account_balances 
            SET balance_cents = balance_cents - NEW.amount_cents,
                last_updated = NOW(),
                last_transaction_id = NEW.journal_entry_id
            WHERE account_id = NEW.credit_account_id;
        END IF;
        
        RETURN NEW;
    END IF;
    
    -- Handle UPDATE
    IF TG_OP = 'UPDATE' THEN
        -- Reverse old posting effects
        IF OLD.debit_account_id IS NOT NULL THEN
            UPDATE account_balances 
            SET balance_cents = balance_cents - OLD.amount_cents,
                last_updated = NOW()
            WHERE account_id = OLD.debit_account_id;
        END IF;
        
        IF OLD.credit_account_id IS NOT NULL THEN
            UPDATE account_balances 
            SET balance_cents = balance_cents + OLD.amount_cents,
                last_updated = NOW()
            WHERE account_id = OLD.credit_account_id;
        END IF;
        
        -- Apply new posting effects
        IF NEW.debit_account_id IS NOT NULL THEN
            UPDATE account_balances 
            SET balance_cents = balance_cents + NEW.amount_cents,
                last_updated = NOW(),
                last_transaction_id = NEW.journal_entry_id
            WHERE account_id = NEW.debit_account_id;
        END IF;
        
        IF NEW.credit_account_id IS NOT NULL THEN
            UPDATE account_balances 
            SET balance_cents = balance_cents - NEW.amount_cents,
                last_updated = NOW(),
                last_transaction_id = NEW.journal_entry_id
            WHERE account_id = NEW.credit_account_id;
        END IF;
        
        RETURN NEW;
    END IF;
    
    -- Handle DELETE
    IF TG_OP = 'DELETE' THEN
        -- Reverse posting effects
        IF OLD.debit_account_id IS NOT NULL THEN
            UPDATE account_balances 
            SET balance_cents = balance_cents - OLD.amount_cents,
                last_updated = NOW()
            WHERE account_id = OLD.debit_account_id;
        END IF;
        
        IF OLD.credit_account_id IS NOT NULL THEN
            UPDATE account_balances 
            SET balance_cents = balance_cents + OLD.amount_cents,
                last_updated = NOW()
            WHERE account_id = OLD.credit_account_id;
        END IF;
        
        RETURN OLD;
    END IF;
    
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

-- Trigger to automatically update account balances
CREATE TRIGGER trigger_update_account_balance
    AFTER INSERT OR UPDATE OR DELETE ON postings
    FOR EACH ROW
    EXECUTE FUNCTION update_account_balance();