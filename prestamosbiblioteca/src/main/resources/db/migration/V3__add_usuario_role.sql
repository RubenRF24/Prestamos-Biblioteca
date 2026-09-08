-- Se agrega el rol USUARIO (lector que se registra solo, reserva y recibe préstamos).
-- ADMIN gestiona, BIBLIOTECARIO opera la mesa, USUARIO es el lector.
ALTER TABLE app_user DROP CONSTRAINT ck_app_user_role;
ALTER TABLE app_user ADD CONSTRAINT ck_app_user_role CHECK (role IN ('ADMIN', 'BIBLIOTECARIO', 'USUARIO'));
