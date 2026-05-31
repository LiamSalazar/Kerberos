# Terraform WSS Readiness Validation

Status: PASS

Validated:
- Terraform configuration is valid.
- HTTP listener is created.
- HTTPS listener is created when enable_https_listener=true.
- HTTP redirects to HTTPS when redirect_http_to_https=true.
- ACM certificate ARN is configurable.
- ALB accepts HTTPS/WSS on port 443.
- Gateway target group remains internal HTTP on port 2800.
- Gateway health check uses HTTP port 2801 and path /health.
- HTTPS host routes exist for:
  - auth-web-demo
  - auth-websocket-gateway
  - sample-login-app

Important:
- The tested ACM ARN is a placeholder.
- No terraform apply was executed.
- A real ACM certificate is required before AWS deployment with WSS.
