-- Expense Management Schema
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Expenses table
CREATE TABLE expenses (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    group_id UUID NOT NULL, -- Reference to group
    creator_id UUID NOT NULL, -- Reference to user who created the expense
    currency CHAR(3) NOT NULL,
    amount_cents BIGINT NOT NULL CHECK (amount_cents > 0),
    occurred_at DATE NOT NULL,
    note TEXT,
    category VARCHAR(50),
    fx_rate DECIMAL(18,8), -- Exchange rate snapshot at time of expense creation
    fx_base_currency CHAR(3), -- Base currency for FX rate
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    is_deleted BOOLEAN NOT NULL DEFAULT false
);

-- Expense participants table (who owes what)
CREATE TABLE expense_participants (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    expense_id UUID NOT NULL REFERENCES expenses(id) ON DELETE CASCADE,
    user_id UUID NOT NULL, -- Reference to user
    rule_type VARCHAR(20) NOT NULL, -- equal, percent, shares, fixed
    rule_value DECIMAL(18,8), -- percentage (0-100), shares count, or fixed amount in cents
    calculated_amount_cents BIGINT NOT NULL, -- Final calculated amount this user owes
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE(expense_id, user_id)
);

-- Expense line items (for itemized expenses)
CREATE TABLE expense_line_items (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    expense_id UUID NOT NULL REFERENCES expenses(id) ON DELETE CASCADE,
    description VARCHAR(200) NOT NULL,
    quantity DECIMAL(10,3) NOT NULL DEFAULT 1.0,
    unit_price_cents BIGINT NOT NULL,
    total_price_cents BIGINT NOT NULL,
    category VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Expense attachments (receipts, images, etc.)
CREATE TABLE expense_attachments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    expense_id UUID NOT NULL REFERENCES expenses(id) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    file_url TEXT NOT NULL,
    file_size_bytes BIGINT,
    mime_type VARCHAR(100),
    uploaded_by UUID NOT NULL, -- Reference to user who uploaded
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Expense categories lookup table
CREATE TABLE expense_categories (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(50) UNIQUE NOT NULL,
    icon VARCHAR(50),
    color VARCHAR(7), -- Hex color code
    is_default BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Insert default categories
INSERT INTO expense_categories (name, icon, color, is_default) VALUES
('Food & Drink', '🍽️', '#FF6B6B', true),
('Transportation', '🚗', '#4ECDC4', true),
('Shopping', '🛍️', '#45B7D1', true),
('Entertainment', '🎬', '#96CEB4', true),
('Bills & Utilities', '⚡', '#FFEAA7', true),
('Travel', '✈️', '#DDA0DD', true),
('Health & Medical', '🏥', '#FF7675', true),
('Education', '📚', '#74B9FF', true),
('Home & Garden', '🏠', '#00B894', true),
('Other', '📝', '#636E72', true);

-- Indexes for performance
CREATE INDEX idx_expenses_group_id ON expenses(group_id);
CREATE INDEX idx_expenses_creator_id ON expenses(creator_id);
CREATE INDEX idx_expenses_occurred_at ON expenses(occurred_at);
CREATE INDEX idx_expenses_category ON expenses(category);
CREATE INDEX idx_expenses_created_at ON expenses(created_at);
CREATE INDEX idx_expense_participants_expense_id ON expense_participants(expense_id);
CREATE INDEX idx_expense_participants_user_id ON expense_participants(user_id);
CREATE INDEX idx_expense_line_items_expense_id ON expense_line_items(expense_id);
CREATE INDEX idx_expense_attachments_expense_id ON expense_attachments(expense_id);
CREATE INDEX idx_expense_attachments_uploaded_by ON expense_attachments(uploaded_by);
