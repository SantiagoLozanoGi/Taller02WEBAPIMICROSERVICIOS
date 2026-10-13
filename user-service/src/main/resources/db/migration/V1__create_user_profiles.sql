CREATE TABLE user_profiles (
    id         UUID PRIMARY KEY,
    username   VARCHAR(60)  NOT NULL UNIQUE,
    email      VARCHAR(120) NOT NULL UNIQUE,
    first_name VARCHAR(80)  NOT NULL,
    last_name  VARCHAR(80)  NOT NULL,
    phone      VARCHAR(30),
    created_at TIMESTAMPTZ  NOT NULL,
    updated_at TIMESTAMPTZ  NOT NULL
);
