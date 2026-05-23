FROM python:3.12-alpine
WORKDIR /app
COPY sample-login-app/ /app/
EXPOSE 5174
CMD ["python", "-m", "http.server", "5174", "--bind", "0.0.0.0", "--directory", "/app"]
