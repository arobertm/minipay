-- Initializare baze de date per serviciu
-- Rulat automat la primul pornire a containerului PostgreSQL

CREATE DATABASE minipay_auth;
CREATE DATABASE minipay_users;
CREATE DATABASE minipay_sessions;
CREATE DATABASE minipay_issuer;
CREATE DATABASE minipay_tds;
CREATE DATABASE minipay_settlement;
CREATE DATABASE minipay_audit;
CREATE DATABASE minipay_psd2;

-- Acordam drepturi utilizatorului minipay
GRANT ALL PRIVILEGES ON DATABASE minipay_auth TO minipay;
GRANT ALL PRIVILEGES ON DATABASE minipay_users TO minipay;
GRANT ALL PRIVILEGES ON DATABASE minipay_sessions TO minipay;
GRANT ALL PRIVILEGES ON DATABASE minipay_issuer TO minipay;
GRANT ALL PRIVILEGES ON DATABASE minipay_tds TO minipay;
GRANT ALL PRIVILEGES ON DATABASE minipay_settlement TO minipay;
GRANT ALL PRIVILEGES ON DATABASE minipay_audit TO minipay;
GRANT ALL PRIVILEGES ON DATABASE minipay_psd2 TO minipay;
