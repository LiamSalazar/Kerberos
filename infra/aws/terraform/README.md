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
- Gateway target group health check on `/health` port `2801`.
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
versions, PostgreSQL/RDS validation, and an operational review.

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

## ACM, DNS and WSS

Set `acm_certificate_arn` to a real ACM certificate ARN only after issuing or
importing the certificate in the same region as the ALB. When it is set,
Terraform creates the HTTPS listener and HTTP-to-HTTPS redirect.

Route 53 is optional in this skeleton. If used, create DNS records that point
the Gateway, demo and login hostnames to `alb_dns_name`. WSS is terminated at
ALB/ACM and forwarded to the Gateway target group.

## Private Network

Only the ALB is public. ECS tasks run in private subnets. AS, TGS, Service and
RDS are reachable only through private Security Group rules and Cloud Map.

## Secrets Mapping

The skeleton creates Secrets Manager placeholders for client, TGS, Service and
PostgreSQL password values. It passes their ARNs as `AUTH_SECRET_*` variables.
Secret versions with real values must be created outside this repository before
any real ECS deployment.

## Known Gaps

- PostgreSQL storage exists, but has not been validated against RDS.
- Gateway health uses HTTP `/health` on port `2801`; it still needs ALB
  validation in AWS.
- No real secrets are stored here.
- No real AWS credentials are required to read this skeleton.
