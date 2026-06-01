# AWS Terraform Readiness

La carpeta `infra/aws/terraform/` es un AWS deployment blueprint. Esta preparada
para revision y plan, no para apply automatico.

## Validaciones Permitidas

```bash
cd infra/aws/terraform
terraform fmt -recursive
terraform init
terraform validate
terraform plan -var-file=terraform.tfvars
terraform plan -var-file=terraform.tfvars.wss-test
```

Si solo existe el ejemplo HTTPS:

```bash
terraform plan -var-file=terraform.tfvars.https.example
```

No ejecutar:

```bash
terraform apply
```

## Componentes Del Blueprint

- VPC con subnets publicas y privadas.
- ALB publico.
- Listener HTTP temporal.
- Listener HTTPS/WSS 443 cuando `enable_https_listener=true`.
- Redirect HTTP -> HTTPS cuando `redirect_http_to_https=true`.
- ECS/Fargate para AS, TGS, Service, Gateway y frontends.
- ECR para imagenes.
- RDS PostgreSQL preparado.
- Secrets Manager para secretos cloud.
- CloudWatch Logs.
- Service Discovery interno.

## Estado WSS

El plan WSS/HTTPS valida que:

- Existe listener HTTPS 443.
- El ACM ARN es configurable.
- HTTP puede redirigir a HTTPS.
- El target group del Gateway sigue usando HTTP interno en puerto `2800`.
- El health check del Gateway usa `2801` y `/health`.

El ACM usado en evidencia es placeholder. Para despliegue real se requiere un
certificado ACM valido.

## Que Falta Para AWS Real

1. Dominio real.
2. Certificado ACM real.
3. Secretos reales en Secrets Manager.
4. Imagenes publicadas en ECR.
5. Revision de costos.
6. Plan revisado por humanos.
7. Ventana explicita para `terraform apply`.

No se ejecuto `terraform apply` y no se crearon recursos AWS.
