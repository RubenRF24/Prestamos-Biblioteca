-- Esquema inicial del sistema de préstamos de biblioteca.

CREATE TABLE app_user (
    id                          BIGSERIAL,
    name                        VARCHAR(150)  NOT NULL,
    email                       VARCHAR(255)  NOT NULL,
    password_hash               VARCHAR(255),
    role                        VARCHAR(20)   NOT NULL,
    status                      VARCHAR(30)   NOT NULL DEFAULT 'ACTIVE',
    activation_token            VARCHAR(100),
    activation_token_expires_at TIMESTAMPTZ,
    blocked                     BOOLEAN       NOT NULL DEFAULT FALSE,
    blocked_until               TIMESTAMPTZ,
    created_at                  TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT pk_app_user PRIMARY KEY (id),
    CONSTRAINT uq_app_user_email UNIQUE (email),
    CONSTRAINT ck_app_user_role CHECK (role IN ('ADMIN', 'BIBLIOTECARIO')),
    CONSTRAINT ck_app_user_status CHECK (status IN ('ACTIVE', 'PENDING_ACTIVATION'))
);

CREATE TABLE book (
    id             BIGSERIAL,
    title          VARCHAR(300)  NOT NULL,
    author         VARCHAR(300)  NOT NULL,
    isbn           VARCHAR(20)   NOT NULL,
    published_year INTEGER,
    status         VARCHAR(20)   NOT NULL DEFAULT 'DISPONIBLE',
    cover_url      VARCHAR(500),
    subjects       TEXT,
    created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT pk_book PRIMARY KEY (id),
    CONSTRAINT uq_book_isbn UNIQUE (isbn),
    CONSTRAINT ck_book_status CHECK (status IN ('DISPONIBLE', 'PRESTADO', 'RESERVADO'))
);

CREATE TABLE loan (
    id                     BIGSERIAL,
    book_id                BIGINT        NOT NULL,
    borrower_id            BIGINT        NOT NULL,
    borrower_name          VARCHAR(150)  NOT NULL,
    borrower_email         VARCHAR(255)  NOT NULL,
    loan_date              TIMESTAMPTZ   NOT NULL,
    due_date               TIMESTAMPTZ   NOT NULL,
    return_date            TIMESTAMPTZ,
    reminder_sent_at       TIMESTAMPTZ,
    overdue_notice_sent_at TIMESTAMPTZ,
    created_at             TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT pk_loan PRIMARY KEY (id),
    CONSTRAINT fk_loan_book FOREIGN KEY (book_id) REFERENCES book (id),
    CONSTRAINT fk_loan_borrower FOREIGN KEY (borrower_id) REFERENCES app_user (id)
);

CREATE TABLE reservation (
    id              BIGSERIAL,
    book_id         BIGINT        NOT NULL,
    requester_id    BIGINT        NOT NULL,
    requester_email VARCHAR(255)  NOT NULL,
    requested_at    TIMESTAMPTZ   NOT NULL,
    notified_at     TIMESTAMPTZ,
    expires_at      TIMESTAMPTZ,
    status          VARCHAR(20)   NOT NULL DEFAULT 'PENDIENTE',
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT pk_reservation PRIMARY KEY (id),
    CONSTRAINT fk_reservation_book FOREIGN KEY (book_id) REFERENCES book (id),
    CONSTRAINT fk_reservation_requester FOREIGN KEY (requester_id) REFERENCES app_user (id),
    CONSTRAINT ck_reservation_status CHECK (status IN ('PENDIENTE', 'NOTIFICADO', 'CANCELADO', 'CUMPLIDO'))
);

CREATE INDEX idx_book_status ON book (status);
CREATE INDEX idx_loan_borrower ON loan (borrower_id);
CREATE INDEX idx_loan_due_date ON loan (due_date);
CREATE INDEX idx_loan_return_date ON loan (return_date);
CREATE INDEX idx_reservation_book_status ON reservation (book_id, status);
