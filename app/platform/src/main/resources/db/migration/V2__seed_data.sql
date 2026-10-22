-- Initial data. Flyway applies each migration once per database, so changes made
-- through the application survive restarts and are never overwritten by this seed.

INSERT INTO users (name, email) VALUES
    ('Anna Kowalska', 'anna.kowalska@example.com'),
    ('Piotr Nowak', 'piotr.nowak@example.com'),
    ('Maria Wiśniewska', 'maria.wisniewska@example.com');

INSERT INTO incidents (title, description, severity, status, assignee_id, created_at, resolved_at) VALUES
    ('Payment API returns 500',
     'Checkout fails for ~20% of requests since the last deployment.',
     'CRITICAL', 'IN_PROGRESS',
     (SELECT id FROM users WHERE email = 'anna.kowalska@example.com'), now(), NULL),
    ('Slow search responses',
     'p95 latency of /search above 2s during peak hours.',
     'HIGH', 'OPEN',
     (SELECT id FROM users WHERE email = 'piotr.nowak@example.com'), now(), NULL),
    ('Disk usage above 85% on logging node',
     NULL,
     'MEDIUM', 'OPEN',
     NULL, now(), NULL),
    ('Typo in password reset email',
     'Subject line says ''Pasword reset''.',
     'LOW', 'RESOLVED',
     (SELECT id FROM users WHERE email = 'maria.wisniewska@example.com'), now(), now());
