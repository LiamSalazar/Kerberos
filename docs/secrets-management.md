# Secrets Management

Phase 20 adds `SecretsProvider` in `auth-core` to prepare cloud usage without
committing real secrets.

## Providers

- `AUTH_SECRET_PROVIDER=env`: resolves secrets from environment variables.
- `AUTH_SECRET_PROVIDER=aws-secrets-manager`: resolves secrets from AWS
  Secrets Manager using AWS SDK v2 and `AUTH_AWS_REGION`.

## Variables

```text
AUTH_SECRET_PROVIDER=env|aws-secrets-manager
AUTH_AWS_REGION=us-east-1
AUTH_SECRET_CLIENT_SECRET_ID=<env-var-or-secret-arn>
AUTH_SECRET_TGS_SECRET_ID=<env-var-or-secret-arn>
AUTH_SECRET_SERVICE_SECRET_ID=<env-var-or-secret-arn>
AUTH_SECRET_POSTGRES_PASSWORD_ID=<env-var-or-secret-arn>
```

With `env`, IDs point to variable names. With `aws-secrets-manager`, IDs can be
Secrets Manager names or ARNs.

## Strict Mode

`AUTH_MODE=strict` requires explicit secrets. For local validation,
`AUTH_DEMO_CLIENT_SECRET`, `AUTH_DEMO_TGS_SECRET`,
`AUTH_DEMO_SERVICE_SECRET`, and `AUTH_POSTGRES_PASSWORD` are accepted. For
cloud, use the IDs above and do not inject secret values as plain text.

If a required secret is missing, the runtime fails at startup with a message
that names the missing variable or reference. The message does not print the
secret value.

## AWS

Before a real deployment:

1. Create secrets in Secrets Manager.
2. Load secret versions outside the repository.
3. Pass ARNs as environment variables or through Terraform.
4. Give the ECS task role `secretsmanager:GetSecretValue` permission only for
   those secrets.
5. Verify startup logs without printing passwords, keys, tickets, or
   ciphertexts.

No real AWS execution occurs in this phase.
