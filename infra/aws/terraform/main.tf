terraform {
  required_version = ">= 1.6.0"

  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

locals {
  tags = {
    Project     = "Kerberos"
    Environment = var.environment
    ManagedBy   = "terraform"
    Phase       = "19"
  }

  ecr_repositories = toset([
    "auth-as",
    "auth-tgs",
    "auth-service",
    "auth-websocket-gateway",
    "auth-web-demo",
    "sample-login-app"
  ])

  service_discovery_namespace = "${var.name_prefix}.local"

  service_definitions = {
    auth-as = {
      port              = 2000
      desired_count     = var.desired_count
      image             = "${aws_ecr_repository.services["auth-as"].repository_url}:latest"
      public            = false
      target_group_arn  = null
      discovery_service = aws_service_discovery_service.private_services["auth-as"].arn
      environment = {
        AUTH_AS_HOST = "0.0.0.0"
        AUTH_AS_PORT = "2000"
      }
    }

    auth-tgs = {
      port              = 2001
      desired_count     = var.desired_count
      image             = "${aws_ecr_repository.services["auth-tgs"].repository_url}:latest"
      public            = false
      target_group_arn  = null
      discovery_service = aws_service_discovery_service.private_services["auth-tgs"].arn
      environment = {
        AUTH_TGS_HOST = "0.0.0.0"
        AUTH_TGS_PORT = "2001"
      }
    }

    auth-service = {
      port              = 2002
      desired_count     = var.desired_count
      image             = "${aws_ecr_repository.services["auth-service"].repository_url}:latest"
      public            = false
      target_group_arn  = null
      discovery_service = aws_service_discovery_service.private_services["auth-service"].arn
      environment = {
        AUTH_SERVICE_HOST = "0.0.0.0"
        AUTH_SERVICE_PORT = "2002"
      }
    }

    auth-websocket-gateway = {
      port              = 2800
      desired_count     = var.gateway_desired_count
      image             = "${aws_ecr_repository.services["auth-websocket-gateway"].repository_url}:latest"
      public            = true
      target_group_arn  = aws_lb_target_group.public_services["auth-websocket-gateway"].arn
      discovery_service = null
      environment = {
        AUTH_AS_HOST                  = "auth-as.${local.service_discovery_namespace}"
        AUTH_AS_PORT                  = "2000"
        AUTH_TGS_HOST                 = "auth-tgs.${local.service_discovery_namespace}"
        AUTH_TGS_PORT                 = "2001"
        AUTH_SERVICE_HOST             = "auth-service.${local.service_discovery_namespace}"
        AUTH_SERVICE_PORT             = "2002"
        AUTH_WS_HOST                  = "0.0.0.0"
        AUTH_WS_PORT                  = "2800"
        AUTH_ALLOWED_ORIGINS          = var.allowed_origins
        AUTH_REQUIRE_SESSION_VERIFY   = "true"
        AUTH_SESSION_STORAGE_MODE     = "postgres"
        AUTH_SESSION_TTL_SECONDS      = tostring(var.auth_session_ttl_seconds)
        AUTH_SESSION_MAX_TTL_SECONDS  = tostring(var.auth_session_max_ttl_seconds)
      }
    }

    auth-web-demo = {
      port              = 5173
      desired_count     = var.desired_count
      image             = "${aws_ecr_repository.services["auth-web-demo"].repository_url}:latest"
      public            = true
      target_group_arn  = aws_lb_target_group.public_services["auth-web-demo"].arn
      discovery_service = null
      environment       = {}
    }

    sample-login-app = {
      port              = 5174
      desired_count     = var.desired_count
      image             = "${aws_ecr_repository.services["sample-login-app"].repository_url}:latest"
      public            = true
      target_group_arn  = aws_lb_target_group.public_services["sample-login-app"].arn
      discovery_service = null
      environment       = {}
    }
  }
}

resource "aws_vpc" "main" {
  cidr_block           = var.vpc_cidr
  enable_dns_hostnames = true
  enable_dns_support   = true

  tags = merge(local.tags, {
    Name = "${var.name_prefix}-vpc"
  })
}

resource "aws_internet_gateway" "main" {
  vpc_id = aws_vpc.main.id

  tags = merge(local.tags, {
    Name = "${var.name_prefix}-igw"
  })
}

resource "aws_subnet" "public" {
  count                   = length(var.public_subnet_cidrs)
  vpc_id                  = aws_vpc.main.id
  cidr_block              = var.public_subnet_cidrs[count.index]
  availability_zone       = var.availability_zones[count.index]
  map_public_ip_on_launch = true

  tags = merge(local.tags, {
    Name = "${var.name_prefix}-public-${count.index + 1}"
    Tier = "public"
  })
}

resource "aws_subnet" "private" {
  count             = length(var.private_subnet_cidrs)
  vpc_id            = aws_vpc.main.id
  cidr_block        = var.private_subnet_cidrs[count.index]
  availability_zone = var.availability_zones[count.index]

  tags = merge(local.tags, {
    Name = "${var.name_prefix}-private-${count.index + 1}"
    Tier = "private"
  })
}

resource "aws_eip" "nat" {
  count  = length(aws_subnet.public)
  domain = "vpc"

  tags = merge(local.tags, {
    Name = "${var.name_prefix}-nat-eip-${count.index + 1}"
  })
}

resource "aws_nat_gateway" "main" {
  count         = length(aws_subnet.public)
  allocation_id = aws_eip.nat[count.index].id
  subnet_id     = aws_subnet.public[count.index].id

  tags = merge(local.tags, {
    Name = "${var.name_prefix}-nat-${count.index + 1}"
  })

  depends_on = [aws_internet_gateway.main]
}

resource "aws_route_table" "public" {
  vpc_id = aws_vpc.main.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.main.id
  }

  tags = merge(local.tags, {
    Name = "${var.name_prefix}-public-rt"
  })
}

resource "aws_route_table_association" "public" {
  count          = length(aws_subnet.public)
  subnet_id      = aws_subnet.public[count.index].id
  route_table_id = aws_route_table.public.id
}

resource "aws_route_table" "private" {
  count  = length(aws_subnet.private)
  vpc_id = aws_vpc.main.id

  route {
    cidr_block     = "0.0.0.0/0"
    nat_gateway_id = aws_nat_gateway.main[count.index].id
  }

  tags = merge(local.tags, {
    Name = "${var.name_prefix}-private-rt-${count.index + 1}"
  })
}

resource "aws_route_table_association" "private" {
  count          = length(aws_subnet.private)
  subnet_id      = aws_subnet.private[count.index].id
  route_table_id = aws_route_table.private[count.index].id
}

resource "aws_security_group" "alb" {
  name        = "${var.name_prefix}-alb-sg"
  description = "Public ALB ingress for HTTPS/WSS and HTTP redirect."
  vpc_id      = aws_vpc.main.id

  ingress {
    description = "HTTP"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "HTTPS and WSS"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = local.tags
}

resource "aws_security_group" "ecs_tasks" {
  name        = "${var.name_prefix}-ecs-tasks-sg"
  description = "ECS task ingress from ALB and private service-to-service traffic."
  vpc_id      = aws_vpc.main.id

  ingress {
    description     = "Gateway from ALB"
    from_port       = 2800
    to_port         = 2800
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
  }

  ingress {
    description     = "Web demo from ALB"
    from_port       = 5173
    to_port         = 5173
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
  }

  ingress {
    description     = "Sample login app from ALB"
    from_port       = 5174
    to_port         = 5174
    protocol        = "tcp"
    security_groups = [aws_security_group.alb.id]
  }

  ingress {
    description = "Private auth TCP services"
    from_port   = 2000
    to_port     = 2002
    protocol    = "tcp"
    self        = true
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = local.tags
}

resource "aws_security_group" "rds" {
  name        = "${var.name_prefix}-rds-sg"
  description = "Optional PostgreSQL ingress from ECS tasks."
  vpc_id      = aws_vpc.main.id

  ingress {
    description     = "PostgreSQL from ECS tasks"
    from_port       = 5432
    to_port         = 5432
    protocol        = "tcp"
    security_groups = [aws_security_group.ecs_tasks.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = local.tags
}

resource "aws_ecr_repository" "services" {
  for_each = local.ecr_repositories
  name     = "${var.name_prefix}/${each.key}"

  image_scanning_configuration {
    scan_on_push = true
  }

  tags = local.tags
}

resource "aws_cloudwatch_log_group" "services" {
  for_each          = local.ecr_repositories
  name              = "/ecs/${var.name_prefix}/${each.key}"
  retention_in_days = 30

  tags = local.tags
}

resource "aws_secretsmanager_secret" "auth_runtime" {
  for_each = toset([
    "auth-demo-client-secret",
    "auth-demo-tgs-secret",
    "auth-demo-service-secret",
    "postgres-connection-url"
  ])

  name = "${var.name_prefix}/${var.environment}/${each.key}"

  tags = merge(local.tags, {
    RotationPrepared = "false"
  })
}

resource "aws_iam_role" "ecs_task_execution" {
  name = "${var.name_prefix}-ecs-task-execution"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Principal = {
        Service = "ecs-tasks.amazonaws.com"
      }
      Action = "sts:AssumeRole"
    }]
  })

  tags = local.tags
}

resource "aws_iam_role_policy_attachment" "ecs_task_execution" {
  role       = aws_iam_role.ecs_task_execution.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

resource "aws_iam_role_policy" "ecs_task_execution_secrets" {
  name = "${var.name_prefix}-ecs-task-execution-secrets"
  role = aws_iam_role.ecs_task_execution.id

  policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Action = [
        "secretsmanager:GetSecretValue",
        "kms:Decrypt"
      ]
      Resource = "*"
    }]
  })
}

resource "aws_iam_role" "ecs_task" {
  name = "${var.name_prefix}-ecs-task"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Effect = "Allow"
      Principal = {
        Service = "ecs-tasks.amazonaws.com"
      }
      Action = "sts:AssumeRole"
    }]
  })

  tags = local.tags
}

resource "aws_lb" "public" {
  name               = "${var.name_prefix}-alb"
  load_balancer_type = "application"
  internal           = false
  security_groups    = [aws_security_group.alb.id]
  subnets            = aws_subnet.public[*].id

  tags = local.tags
}

resource "aws_lb_target_group" "public_services" {
  for_each = {
    auth-websocket-gateway = 2800
    auth-web-demo          = 5173
    sample-login-app       = 5174
  }

  name        = substr("${var.name_prefix}-${each.key}", 0, 32)
  port        = each.value
  protocol    = "HTTP"
  target_type = "ip"
  vpc_id      = aws_vpc.main.id

  health_check {
    enabled             = true
    path                = "/"
    protocol            = "HTTP"
    matcher             = "200-499"
    healthy_threshold   = 2
    unhealthy_threshold = 3
    interval            = 30
    timeout             = 5
  }

  tags = local.tags
}

resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.public.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type = var.acm_certificate_arn == "" ? "forward" : "redirect"

    target_group_arn = var.acm_certificate_arn == "" ? aws_lb_target_group.public_services["auth-websocket-gateway"].arn : null

    dynamic "redirect" {
      for_each = var.acm_certificate_arn == "" ? [] : [1]
      content {
        port        = "443"
        protocol    = "HTTPS"
        status_code = "HTTP_301"
      }
    }
  }
}

resource "aws_lb_listener" "https" {
  count             = var.acm_certificate_arn == "" ? 0 : 1
  load_balancer_arn = aws_lb.public.arn
  port              = 443
  protocol          = "HTTPS"
  certificate_arn   = var.acm_certificate_arn

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.public_services["auth-websocket-gateway"].arn
  }
}

resource "aws_lb_listener_rule" "https_host_routes" {
  for_each = var.acm_certificate_arn == "" ? {} : {
    auth-websocket-gateway = var.gateway_host_header
    auth-web-demo          = var.web_demo_host_header
    sample-login-app       = var.sample_login_host_header
  }

  listener_arn = aws_lb_listener.https[0].arn
  priority = {
    auth-websocket-gateway = 10
    auth-web-demo          = 20
    sample-login-app       = 30
  }[each.key]

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.public_services[each.key].arn
  }

  condition {
    host_header {
      values = [each.value]
    }
  }
}

resource "aws_service_discovery_private_dns_namespace" "auth" {
  name        = local.service_discovery_namespace
  description = "Private discovery namespace for Kerberos auth services."
  vpc         = aws_vpc.main.id

  tags = local.tags
}

resource "aws_service_discovery_service" "private_services" {
  for_each = toset(["auth-as", "auth-tgs", "auth-service"])
  name     = each.key

  dns_config {
    namespace_id = aws_service_discovery_private_dns_namespace.auth.id

    dns_records {
      ttl  = 10
      type = "A"
    }

    routing_policy = "MULTIVALUE"
  }

  health_check_custom_config {
    failure_threshold = 1
  }

  tags = local.tags
}

resource "aws_ecs_cluster" "main" {
  name = "${var.name_prefix}-cluster"

  setting {
    name  = "containerInsights"
    value = "enabled"
  }

  tags = local.tags
}

resource "aws_ecs_task_definition" "services" {
  for_each                 = local.service_definitions
  family                   = "${var.name_prefix}-${each.key}"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = var.container_cpu
  memory                   = var.container_memory
  execution_role_arn       = aws_iam_role.ecs_task_execution.arn
  task_role_arn            = aws_iam_role.ecs_task.arn

  container_definitions = jsonencode([{
    name      = each.key
    image     = each.value.image
    essential = true

    portMappings = [{
      containerPort = each.value.port
      hostPort      = each.value.port
      protocol      = "tcp"
    }]

    environment = concat(
      [
        { name = "AUTH_MODE", value = "strict" },
        { name = "AUTH_STORAGE_MODE", value = var.enable_rds_postgres ? "postgres" : "sqlite" },
        { name = "AUTH_SQLITE_PATH", value = "/data/auth-demo.sqlite" }
      ],
      [for key, value in each.value.environment : { name = key, value = value }]
    )

    logConfiguration = {
      logDriver = "awslogs"
      options = {
        awslogs-group         = aws_cloudwatch_log_group.services[each.key].name
        awslogs-region        = var.aws_region
        awslogs-stream-prefix = "ecs"
      }
    }
  }])

  tags = local.tags
}

resource "aws_ecs_service" "services" {
  for_each        = local.service_definitions
  name            = each.key
  cluster         = aws_ecs_cluster.main.id
  task_definition = aws_ecs_task_definition.services[each.key].arn
  desired_count   = each.value.desired_count
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = aws_subnet.private[*].id
    security_groups  = [aws_security_group.ecs_tasks.id]
    assign_public_ip = false
  }

  dynamic "load_balancer" {
    for_each = each.value.target_group_arn == null ? [] : [each.value]
    content {
      target_group_arn = load_balancer.value.target_group_arn
      container_name   = each.key
      container_port   = each.value.port
    }
  }

  dynamic "service_registries" {
    for_each = each.value.discovery_service == null ? [] : [each.value]
    content {
      registry_arn = service_registries.value.discovery_service
    }
  }

  depends_on = [
    aws_lb_listener.http,
    aws_iam_role_policy_attachment.ecs_task_execution
  ]

  tags = local.tags
}

resource "aws_db_subnet_group" "postgres" {
  count      = var.enable_rds_postgres ? 1 : 0
  name       = "${var.name_prefix}-postgres-subnets"
  subnet_ids = aws_subnet.private[*].id

  tags = local.tags
}

resource "aws_db_instance" "postgres" {
  count                      = var.enable_rds_postgres ? 1 : 0
  identifier                 = "${var.name_prefix}-postgres"
  engine                     = "postgres"
  engine_version             = "16"
  instance_class             = "db.t4g.micro"
  allocated_storage          = 20
  db_name                    = var.postgres_db_name
  username                   = var.postgres_master_username
  manage_master_user_password = true
  db_subnet_group_name       = aws_db_subnet_group.postgres[0].name
  vpc_security_group_ids     = [aws_security_group.rds.id]
  publicly_accessible        = false
  skip_final_snapshot        = true
  deletion_protection        = true

  tags = local.tags
}
