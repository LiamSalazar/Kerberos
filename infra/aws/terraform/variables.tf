variable "aws_region" {
  description = "AWS region for the deployment skeleton."
  type        = string
  default     = "us-east-1"
}

variable "name_prefix" {
  description = "Prefix used for AWS resource names."
  type        = string
  default     = "kerberos-auth"
}

variable "environment" {
  description = "Environment label for tags and names."
  type        = string
  default     = "validation"
}

variable "vpc_cidr" {
  description = "CIDR block for the VPC."
  type        = string
  default     = "10.40.0.0/16"
}

variable "availability_zones" {
  description = "Two or more availability zones for public/private subnets."
  type        = list(string)
  default     = ["us-east-1a", "us-east-1b"]
}

variable "public_subnet_cidrs" {
  description = "Public subnet CIDRs for the ALB and NAT gateways."
  type        = list(string)
  default     = ["10.40.0.0/24", "10.40.1.0/24"]
}

variable "private_subnet_cidrs" {
  description = "Private subnet CIDRs for ECS tasks and optional RDS."
  type        = list(string)
  default     = ["10.40.10.0/24", "10.40.11.0/24"]
}

variable "acm_certificate_arn" {
  description = "ACM certificate ARN for HTTPS/WSS. Leave empty until a real certificate exists."
  type        = string
  default     = ""
}

variable "gateway_host_header" {
  description = "Host header routed to the WebSocket Gateway."
  type        = string
  default     = "gateway.example.com"
}

variable "web_demo_host_header" {
  description = "Host header routed to auth-web-demo."
  type        = string
  default     = "demo.example.com"
}

variable "sample_login_host_header" {
  description = "Host header routed to sample-login-app."
  type        = string
  default     = "login.example.com"
}

variable "desired_count" {
  description = "Default desired ECS task count per service."
  type        = number
  default     = 1
}

variable "gateway_desired_count" {
  description = "Desired ECS task count for the public WebSocket Gateway."
  type        = number
  default     = 2
}

variable "container_cpu" {
  description = "Default Fargate CPU units per task."
  type        = number
  default     = 512
}

variable "container_memory" {
  description = "Default Fargate memory MiB per task."
  type        = number
  default     = 1024
}

variable "allowed_origins" {
  description = "Comma-separated browser origins accepted by the Gateway."
  type        = string
  default     = "https://demo.example.com,https://login.example.com"
}

variable "auth_session_ttl_seconds" {
  description = "Opaque Gateway session TTL."
  type        = number
  default     = 300
}

variable "auth_session_max_ttl_seconds" {
  description = "Maximum opaque Gateway session TTL."
  type        = number
  default     = 900
}

variable "enable_rds_postgres" {
  description = "Create an optional RDS PostgreSQL instance for future AUTH_STORAGE_MODE=postgres work."
  type        = bool
  default     = false
}

variable "postgres_db_name" {
  description = "Initial PostgreSQL database name when RDS is enabled."
  type        = string
  default     = "kerberos_auth"
}

variable "postgres_master_username" {
  description = "RDS master username. Password is managed by AWS Secrets Manager when enabled."
  type        = string
  default     = "kerberos_admin"
}
