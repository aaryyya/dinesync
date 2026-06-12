-- DineSync seed data (intentionally empty — sessions and orders are created at runtime)
-- Sample customer sessions
INSERT INTO customer_sessions (
    session_uuid,
    table_id,
    status
) VALUES
('550e8400-e29b-41d4-a716-446655440001', 1, 'ACTIVE'),
('550e8400-e29b-41d4-a716-446655440002', 2, 'ACTIVE'),
('550e8400-e29b-41d4-a716-446655440003', 3, 'CHECKED_OUT');

-- Sample orders
INSERT INTO orders (
    session_uuid,
    table_id,
    item_name,
    status
) VALUES
('550e8400-e29b-41d4-a716-446655440001', 1, 'Margherita Pizza', 'RECEIVED'),
('550e8400-e29b-41d4-a716-446655440001', 1, 'Garlic Bread', 'PREPARING'),
('550e8400-e29b-41d4-a716-446655440002', 2, 'Veg Burger', 'SERVED'),
('550e8400-e29b-41d4-a716-446655440002', 2, 'French Fries', 'PREPARING'),
('550e8400-e29b-41d4-a716-446655440003', 3, 'Pasta Alfredo', 'SERVED');