INSERT INTO customers (customer_id, name, customer_type, date_joined) VALUES
(1, 'Ada Obi',           'RETAIL',   '2019-01-10'),   -- long tenure
(2, 'Zenith Traders Ltd','BUSINESS', '2025-02-01'),
(3, 'Tunde Bello',       'RETAIL',   '2026-01-05')
    ON CONFLICT (customer_id) DO NOTHING;

INSERT INTO accounts (id, account_number, created_at, customer_id) VALUES
(1, '1001000001', NOW(), 1),
(2, '1001000002', NOW(), 1),   -- second account, same customer
(3, '2002000001', NOW(), 2),
(4, '3003000001', NOW(), 3)
    ON CONFLICT (id) DO NOTHING;