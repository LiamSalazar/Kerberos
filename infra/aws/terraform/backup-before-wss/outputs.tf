output "alb_dns_name" {
  description = "Public ALB DNS name for HTTPS/WSS routing."
  value       = aws_lb.public.dns_name
}

output "ecs_cluster_name" {
  description = "ECS cluster name."
  value       = aws_ecs_cluster.main.name
}

output "ecr_repository_urls" {
  description = "ECR repository URLs keyed by service name."
  value       = { for name, repo in aws_ecr_repository.services : name => repo.repository_url }
}

output "private_service_discovery_namespace" {
  description = "Private Cloud Map namespace for AS/TGS/Service."
  value       = aws_service_discovery_private_dns_namespace.auth.name
}

output "secret_arns" {
  description = "Secrets Manager secret ARNs prepared for later secret versions."
  value       = { for name, secret in aws_secretsmanager_secret.auth_runtime : name => secret.arn }
}

output "postgres_endpoint" {
  description = "Optional RDS PostgreSQL endpoint when enable_rds_postgres=true."
  value       = var.enable_rds_postgres ? aws_db_instance.postgres[0].endpoint : null
}

output "https_listener_enabled" {
  description = "Whether Terraform will create an HTTPS/WSS ALB listener."
  value       = local.https_listener_enabled
}

output "http_redirect_enabled" {
  description = "Whether Terraform will redirect HTTP traffic to HTTPS."
  value       = local.http_redirect_enabled
}
