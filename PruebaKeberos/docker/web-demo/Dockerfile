FROM node:20-alpine
WORKDIR /app
COPY auth-web-demo/ /app/
ENV HOST=0.0.0.0
ENV PORT=5173
EXPOSE 5173
CMD ["node", "scripts/dev-server.js"]
