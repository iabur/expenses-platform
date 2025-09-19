-- FX Service Database Schema
-- Exchange rates and currency conversion

-- Currency master table
CREATE TABLE currencies (
    code VARCHAR(3) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    symbol VARCHAR(10),
    decimal_places INTEGER DEFAULT 2,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Exchange rates table
CREATE TABLE exchange_rates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    base_currency VARCHAR(3) NOT NULL REFERENCES currencies(code),
    target_currency VARCHAR(3) NOT NULL REFERENCES currencies(code),
    rate DECIMAL(20, 8) NOT NULL,
    rate_date DATE NOT NULL,
    source VARCHAR(50) DEFAULT 'MANUAL',
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    
    UNIQUE(base_currency, target_currency, rate_date)
);

-- Currency conversion history
CREATE TABLE conversion_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    from_currency VARCHAR(3) NOT NULL REFERENCES currencies(code),
    to_currency VARCHAR(3) NOT NULL REFERENCES currencies(code),
    original_amount_cents BIGINT NOT NULL,
    converted_amount_cents BIGINT NOT NULL,
    exchange_rate DECIMAL(20, 8) NOT NULL,
    conversion_date TIMESTAMP DEFAULT NOW(),
    user_id UUID,
    reference_type VARCHAR(50), -- 'EXPENSE', 'SETTLEMENT', etc.
    reference_id UUID,
    
    CHECK (original_amount_cents >= 0),
    CHECK (converted_amount_cents >= 0),
    CHECK (exchange_rate > 0)
);

-- Indexes for performance
CREATE INDEX idx_exchange_rates_base_target ON exchange_rates(base_currency, target_currency);
CREATE INDEX idx_exchange_rates_date ON exchange_rates(rate_date DESC);
CREATE INDEX idx_exchange_rates_active ON exchange_rates(is_active) WHERE is_active = true;
CREATE INDEX idx_conversion_history_date ON conversion_history(conversion_date DESC);
CREATE INDEX idx_conversion_history_user ON conversion_history(user_id);
CREATE INDEX idx_conversion_history_reference ON conversion_history(reference_type, reference_id);

-- Insert common currencies
INSERT INTO currencies (code, name, symbol, decimal_places) VALUES
('USD', 'US Dollar', '$', 2),
('EUR', 'Euro', '€', 2),
('GBP', 'British Pound', '£', 2),
('JPY', 'Japanese Yen', '¥', 0),
('BDT', 'Bangladeshi Taka', '৳', 2),
('INR', 'Indian Rupee', '₹', 2),
('CAD', 'Canadian Dollar', 'C$', 2),
('AUD', 'Australian Dollar', 'A$', 2),
('CHF', 'Swiss Franc', 'Fr', 2),
('CNY', 'Chinese Yuan', '¥', 2),
('SGD', 'Singapore Dollar', 'S$', 2),
('HKD', 'Hong Kong Dollar', 'HK$', 2),
('SEK', 'Swedish Krona', 'kr', 2),
('NOK', 'Norwegian Krone', 'kr', 2),
('DKK', 'Danish Krone', 'kr', 2);

-- Insert basic exchange rates (USD as base)
INSERT INTO exchange_rates (base_currency, target_currency, rate, rate_date, source) VALUES
-- USD to other currencies
('USD', 'EUR', 0.92, CURRENT_DATE, 'MANUAL'),
('USD', 'GBP', 0.79, CURRENT_DATE, 'MANUAL'),
('USD', 'JPY', 149.50, CURRENT_DATE, 'MANUAL'),
('USD', 'BDT', 109.85, CURRENT_DATE, 'MANUAL'),
('USD', 'INR', 83.25, CURRENT_DATE, 'MANUAL'),
('USD', 'CAD', 1.36, CURRENT_DATE, 'MANUAL'),
('USD', 'AUD', 1.53, CURRENT_DATE, 'MANUAL'),
('USD', 'CHF', 0.89, CURRENT_DATE, 'MANUAL'),
('USD', 'CNY', 7.24, CURRENT_DATE, 'MANUAL'),
('USD', 'SGD', 1.35, CURRENT_DATE, 'MANUAL'),

-- Reverse rates (other currencies to USD)
('EUR', 'USD', 1.087, CURRENT_DATE, 'MANUAL'),
('GBP', 'USD', 1.266, CURRENT_DATE, 'MANUAL'),
('JPY', 'USD', 0.0067, CURRENT_DATE, 'MANUAL'),
('BDT', 'USD', 0.0091, CURRENT_DATE, 'MANUAL'),
('INR', 'USD', 0.012, CURRENT_DATE, 'MANUAL'),
('CAD', 'USD', 0.735, CURRENT_DATE, 'MANUAL'),
('AUD', 'USD', 0.654, CURRENT_DATE, 'MANUAL'),
('CHF', 'USD', 1.124, CURRENT_DATE, 'MANUAL'),
('CNY', 'USD', 0.138, CURRENT_DATE, 'MANUAL'),
('SGD', 'USD', 0.741, CURRENT_DATE, 'MANUAL'),

-- Self-reference rates (1:1)
('USD', 'USD', 1.0, CURRENT_DATE, 'MANUAL'),
('EUR', 'EUR', 1.0, CURRENT_DATE, 'MANUAL'),
('BDT', 'BDT', 1.0, CURRENT_DATE, 'MANUAL'),
('GBP', 'GBP', 1.0, CURRENT_DATE, 'MANUAL'),
('JPY', 'JPY', 1.0, CURRENT_DATE, 'MANUAL'),
('INR', 'INR', 1.0, CURRENT_DATE, 'MANUAL');
