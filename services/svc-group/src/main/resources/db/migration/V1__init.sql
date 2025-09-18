-- Group Management Schema
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Groups table
CREATE TABLE groups (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(200) NOT NULL,
    description TEXT,
    type VARCHAR(20) NOT NULL DEFAULT 'general', -- general, household, trip, project
    default_currency CHAR(3) NOT NULL DEFAULT 'USD',
    avatar_url TEXT,
    created_by UUID NOT NULL, -- Reference to user who created the group
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    is_active BOOLEAN NOT NULL DEFAULT true
);

-- Group members table
CREATE TABLE group_members (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    group_id UUID NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    user_id UUID NOT NULL, -- Reference to user (from user service)
    role VARCHAR(20) NOT NULL DEFAULT 'member', -- owner, admin, member
    joined_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    status VARCHAR(20) NOT NULL DEFAULT 'active', -- active, invited, left, removed
    invited_by UUID, -- Reference to user who invited this member
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    UNIQUE(group_id, user_id)
);

-- Group settings table
CREATE TABLE group_settings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    group_id UUID NOT NULL REFERENCES groups(id) ON DELETE CASCADE UNIQUE,
    simplify_debts BOOLEAN NOT NULL DEFAULT true,
    auto_settle_threshold DECIMAL(15,2) DEFAULT 0.01,
    allow_non_members_to_view BOOLEAN NOT NULL DEFAULT false,
    require_approval_for_expenses BOOLEAN NOT NULL DEFAULT false,
    notification_new_expense BOOLEAN NOT NULL DEFAULT true,
    notification_expense_update BOOLEAN NOT NULL DEFAULT true,
    notification_payment BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Group invitations table
CREATE TABLE group_invitations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    group_id UUID NOT NULL REFERENCES groups(id) ON DELETE CASCADE,
    invited_by UUID NOT NULL, -- Reference to user who sent invitation
    invited_email VARCHAR(255) NOT NULL,
    invited_user_id UUID, -- Reference to user if they exist in system
    role VARCHAR(20) NOT NULL DEFAULT 'member',
    invitation_code VARCHAR(100) UNIQUE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'pending', -- pending, accepted, declined, expired
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

-- Indexes for performance
CREATE INDEX idx_groups_created_by ON groups(created_by);
CREATE INDEX idx_groups_created_at ON groups(created_at);
CREATE INDEX idx_groups_type ON groups(type);
CREATE INDEX idx_group_members_group_id ON group_members(group_id);
CREATE INDEX idx_group_members_user_id ON group_members(user_id);
CREATE INDEX idx_group_members_status ON group_members(status);
CREATE INDEX idx_group_settings_group_id ON group_settings(group_id);
CREATE INDEX idx_group_invitations_group_id ON group_invitations(group_id);
CREATE INDEX idx_group_invitations_invited_email ON group_invitations(invited_email);
CREATE INDEX idx_group_invitations_invitation_code ON group_invitations(invitation_code);
CREATE INDEX idx_group_invitations_status ON group_invitations(status);
