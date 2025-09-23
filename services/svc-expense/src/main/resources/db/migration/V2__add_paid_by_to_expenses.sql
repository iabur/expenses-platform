-- Add paid_by column, backfill from creator_id, then enforce NOT NULL
ALTER TABLE expenses ADD COLUMN IF NOT EXISTS paid_by UUID;

UPDATE expenses SET paid_by = creator_id WHERE paid_by IS NULL;

ALTER TABLE expenses ALTER COLUMN paid_by SET NOT NULL;

-- Index to speed up queries by payer if needed later
CREATE INDEX IF NOT EXISTS idx_expenses_paid_by ON expenses(paid_by);

