# Secrets Management

Fase 20 agrega `SecretsProvider` en `auth-core` para preparar cloud sin subir
secretos reales.

## Proveedores

- `AUTH_SECRET_PROVIDER=env`: resuelve secretos desde variables de entorno.
- `AUTH_SECRET_PROVIDER=aws-secrets-manager`: resuelve secretos desde AWS
  Secrets Manager usando AWS SDK v2 y `AUTH_AWS_REGION`.

## Variables

```text
AUTH_SECRET_PROVIDER=env|aws-secrets-manager
AUTH_AWS_REGION=us-east-1
AUTH_SECRET_CLIENT_SECRET_ID=<env-var-or-secret-arn>
AUTH_SECRET_TGS_SECRET_ID=<env-var-or-secret-arn>
AUTH_SECRET_SERVICE_SECRET_ID=<env-var-or-secret-arn>
AUTH_SECRET_POSTGRES_PASSWORD_ID=<env-var-or-secret-arn>
```

Con `env`, los IDs apuntan a nombres de variables. Con
`aws-secrets-manager`, los IDs pueden ser nombres o ARNs de Secrets Manager.

## Strict Mode

`AUTH_MODE=strict` exige secretos explicitos. Para validacion local se aceptan
`AUTH_DEMO_CLIENT_SECRET`, `AUTH_DEMO_TGS_SECRET`,
`AUTH_DEMO_SERVICE_SECRET` y `AUTH_POSTGRES_PASSWORD`. Para cloud se recomienda
usar los IDs anteriores y no inyectar valores secretos como texto plano.

Si falta un secreto requerido, el runtime falla al arrancar con un mensaje que
nombra la variable o referencia faltante. El mensaje no imprime el valor del
secreto.

## AWS

Antes de un despliegue real:

1. Crear secretos en Secrets Manager.
2. Cargar versiones de secreto fuera del repositorio.
3. Pasar los ARNs como variables de entorno o mediante Terraform.
4. Dar al task role ECS permiso `secretsmanager:GetSecretValue` solo sobre esos
   secretos.
5. Verificar logs de arranque sin imprimir passwords, claves, tickets ni
   ciphertexts.

No se ejecuta AWS real en esta fase.
