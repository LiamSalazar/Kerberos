# AWS Terraform Skeleton

This folder is a preparation skeleton for a future production-like deployment.
It must not be applied from this phase.

## Scope

The skeleton prepares:

- VPC with public and private subnets.
- NAT gateways for private ECS task egress.
- ECR repositories for every containerized service.
- ECS Fargate cluster and services.
- Application Load Balancer for HTTPS/WSS entry.
- Private Cloud Map service discovery for AS, TGS and Service.
- Security Groups for public ALB ingress and private service traffic.
- CloudWatch log groups.
- IAM task execution and task roles.
- Secrets Manager secret placeholders.
- Optional RDS PostgreSQL resources behind `enable_rds_postgres`.

## Do Not Apply Yet

Do not run:

```bash
terraform apply
```

Before a real deployment, the project still needs Linux Docker validation,
real container image publishing, ACM certificate setup, Secrets Manager secret
versions, a PostgreSQL storage implementation, and an operational review.

## Local Review Commands

These commands only inspect the Terraform files and do not deploy:

```bash
terraform fmt -check
terraform init -backend=false
terraform validate
```

Use a copied tfvars file only after replacing placeholder domains and reviewing
the generated plan:

```bash
cp terraform.tfvars.example terraform.tfvars
terraform plan
```

## Expected Routing

- `gateway.example.com` routes to `auth-websocket-gateway` on port `2800`.
- `demo.example.com` routes to `auth-web-demo` on port `5173`.
- `login.example.com` routes to `sample-login-app` on port `5174`.
- AS, TGS and Service stay private and are discovered through Cloud Map.

## Known Gaps

- The current runtime has no PostgreSQL storage module yet.
- The Gateway health check is a placeholder and should be replaced with a real
  HTTP health endpoint before real ALB deployment.
- No real secrets are stored here.
- No real AWS credentials are required to read this skeleton.
