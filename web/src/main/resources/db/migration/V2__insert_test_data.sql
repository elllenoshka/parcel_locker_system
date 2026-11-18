CREATE EXTENSION IF NOT EXISTS pgcrypto;


INSERT INTO locker(id, address, tz) VALUES
    ('locker-0001', 'вулиця Сумська, 150', 'Europe/Kyiv'),
    ('locker-0002', 'проспект Науки, 200', 'Europe/Kyiv');

INSERT INTO cell(id, locker_id, size, status) VALUES
    ('c01-01', 'locker-0001', 'S', 'free'),
    ('c01-02', 'locker-0001', 'M', 'free'),
    ('c01-03', 'locker-0001', 'L', 'free'),
    ('c01-04', 'locker-0001', 'L', 'free'),
    ('c02-01', 'locker-0002', 'M', 'free'),
    ('c02-02', 'locker-0002', 'M', 'free'),
    ('c02-03', 'locker-0002', 'L', 'free'),
    ('c02-04', 'locker-0002', 'S', 'free'),
    ('c02-05', 'locker-0002', 'S', 'free');

INSERT INTO app_user(id, login, name, password_hash, role) VALUES
    ('u-staff-1', 'staff1', 'Олександр Коваленко',crypt('staff-pass', gen_salt('bf')), 'staff'),
    ('u-user-1', 'user1', 'Софія Бондаренко',crypt('user-pass', gen_salt('bf')), 'user'),
    ('u-user-2', 'user2', 'Артем Мельник',crypt('user-pass', gen_salt('bf')), 'user');
