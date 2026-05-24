INSERT INTO principals (principal_type, id, display_name, secret, enabled)
VALUES
    ('CLIENT', '1', 'Demo Client 1', 'ContraseniaCliente', TRUE),
    ('TGS', '1', 'Demo Ticket Granting Server', 'contraseñaTGS', TRUE)
ON CONFLICT (principal_type, id) DO UPDATE SET
    display_name = EXCLUDED.display_name,
    secret = EXCLUDED.secret,
    enabled = EXCLUDED.enabled;

INSERT INTO services (id, display_name, secret, endpoint, enabled)
VALUES
    ('1', 'MelodyFinder Demo Service', 'contraseñaServidor', 'local://melodyfinder', TRUE)
ON CONFLICT (id) DO UPDATE SET
    display_name = EXCLUDED.display_name,
    secret = EXCLUDED.secret,
    endpoint = EXCLUDED.endpoint,
    enabled = EXCLUDED.enabled;
