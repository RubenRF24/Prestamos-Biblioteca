-- Catálogo de demo. ISBNs reales para poder probar el enriquecimiento desde Open Library.
-- El usuario ADMIN NO se siembra aquí: lo crea un seeder Java idempotente usando el
-- PasswordEncoder, para no dejar ningún hash/credencial en el repositorio.

INSERT INTO book (title, author, isbn, published_year, status) VALUES
    ('Clean Code', 'Robert C. Martin', '9780132350884', 2008, 'DISPONIBLE'),
    ('The Pragmatic Programmer', 'Andrew Hunt', '9780201616224', 1999, 'DISPONIBLE'),
    ('Effective Java', 'Joshua Bloch', '9780134685991', 2018, 'DISPONIBLE'),
    ('Refactoring', 'Martin Fowler', '9780134757599', 2018, 'DISPONIBLE'),
    ('Domain-Driven Design', 'Eric Evans', '9780321125217', 2003, 'DISPONIBLE');
