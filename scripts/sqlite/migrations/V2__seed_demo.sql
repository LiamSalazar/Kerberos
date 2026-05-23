INSERT OR REPLACE INTO principals (principal_type, id, display_name, secret, enabled)
VALUES
    ('CLIENT', '1', 'Demo Client 1', 'ContraseniaCliente', 1),
    ('TGS', '1', 'Demo Ticket Granting Server', 'contraseñaTGS', 1);

INSERT OR REPLACE INTO services (id, display_name, secret, endpoint, enabled)
VALUES
    ('1', 'MelodyFinder Demo Service', 'contraseñaServidor', 'local://melodyfinder', 1);
