aws_region  = "us-east-1"
name_prefix = "kerberos-auth"
environment = "validation"

vpc_cidr             = "10.40.0.0/16"
availability_zones   = ["us-east-1a", "us-east-1b"]
public_subnet_cidrs  = ["10.40.0.0/24", "10.40.1.0/24"]
private_subnet_cidrs = ["10.40.10.0/24", "10.40.11.0/24"]

# HTTP is valid only for temporary validation.
# Set enable_https_listener=true only after issuing/importing a real ACM certificate.
enable_https_listener  = false
redirect_http_to_https = true
ssl_policy             = "ELBSecurityPolicy-TLS13-1-2-2021-06"
acm_certificate_arn    = ""

gateway_host_header      = "gateway.example.com"
web_demo_host_header     = "demo.example.com"
sample_login_host_header = "login.example.com"

desired_count         = 1
gateway_desired_count = 2
container_cpu         = 512
container_memory      = 1024

allowed_origins              = "https://demo.example.com,https://login.example.com"
auth_session_ttl_seconds     = 300
auth_session_max_ttl_seconds = 900

# Keep false until Docker Linux and RDS validation are completed.
enable_rds_postgres      = false
postgres_db_name         = "kerberos_auth"
postgres_master_username = "kerberos_admin"
