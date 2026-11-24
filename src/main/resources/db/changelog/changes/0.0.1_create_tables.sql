--liquibase formatted sql

--changeset order-service:0.0.1_create_tables
CREATE TABLE items (
    id         BIGSERIAL      PRIMARY KEY,
    name       VARCHAR(255)   NOT NULL,
    price      NUMERIC(12, 2) NOT NULL CHECK (price >= 0),
    created_at TIMESTAMP      NOT NULL DEFAULT now(),
    updated_at TIMESTAMP
);

CREATE TABLE orders (
    id          BIGSERIAL      PRIMARY KEY,
    user_id     BIGINT         NOT NULL,
    status      VARCHAR(20)    NOT NULL,
    total_price NUMERIC(14, 2) NOT NULL,
    deleted     BOOLEAN        NOT NULL DEFAULT false,
    created_at  TIMESTAMP      NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP,
    CONSTRAINT chk_orders_status CHECK (status IN ('CREATED', 'PAID', 'SHIPPED'))
);

CREATE TABLE order_items (
    id         BIGSERIAL PRIMARY KEY,
    order_id   BIGINT    NOT NULL REFERENCES orders (id),
    item_id    BIGINT    NOT NULL REFERENCES items (id),
    quantity   INTEGER   NOT NULL CHECK (quantity > 0),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP
);