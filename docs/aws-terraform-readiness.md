# AWS Terraform Readiness

The `infra/aws/terraform/` folder is an AWS deployment blueprint. It is prepared
for review and plan, not for automatic apply.

## Allowed Validations

```bash
cd infra/aws/terraform
terraform fmt -recursive
terraform init
terraform validate
terraform plan -var-file=terraform.tfvars
terraform plan -var-file=terraform.tfvars.wss-test
```

If only the HTTPS example exists:

```bash
terraform plan -var-file=terraform.tfvars.https.example
```

Do not run:

```bash
terraform apply
```

## Blueprint Components

- VPC with public and private subnets.
- Public ALB.
- Temporary HTTP listener.
- HTTPS/WSS 443 listener when `enable_https_listener=true`.
- HTTP -> HTTPS redirect when `redirect_http_to_https=true`.
- ECS/Fargate for AS, TGS, Service, Gateway, and frontends.
- ECR for images.
- Prepared RDS PostgreSQL.
- Secrets Manager for cloud secrets.
- CloudWatch Logs.
- Internal Service Discovery.

## WSS Status

The WSS/HTTPS plan validates that:

- HTTPS listener 443 exists.
- The ACM ARN is configurable.
- HTTP can redirect to HTTPS.
- The Gateway health check uses `2801` and `/health`.

The ACM used in evidence is a placeholder. A valid ACM certificate is required
for real deployment.

## What Is Missing For Real AWS

1. Real domain.
2. Real ACM certificate.
3. Real secrets in Secrets Manager.
4. Images published to ECR.
5. Cost review.
6. Explicit window for `terraform apply`.

`terraform apply` was not executed and no AWS resources were created.
