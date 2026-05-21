const fs = require("node:fs");
const path = require("node:path");

const root = path.resolve(__dirname, "..");
const dist = path.join(root, "dist");
const requiredFiles = [
  "index.html",
  "src/app.js",
  "src/websocket-client.js",
  "src/state.js",
  "src/renderer.js",
  "src/styles.css"
];

for (const relativePath of requiredFiles) {
  const filePath = path.join(root, relativePath);
  if (!fs.existsSync(filePath)) {
    throw new Error(`Required frontend file missing: ${relativePath}`);
  }
  const content = fs.readFileSync(filePath, "utf8");
  if (!content.trim()) {
    throw new Error(`Frontend file is empty: ${relativePath}`);
  }
}

const html = fs.readFileSync(path.join(root, "index.html"), "utf8");
if (!html.includes('type="module" src="./src/app.js"')) {
  throw new Error("index.html must load src/app.js as a module.");
}
if (!html.includes('rel="stylesheet" href="./src/styles.css"')) {
  throw new Error("index.html must load src/styles.css.");
}

const app = fs.readFileSync(path.join(root, "src/app.js"), "utf8");
const client = fs.readFileSync(path.join(root, "src/websocket-client.js"), "utf8");
const state = fs.readFileSync(path.join(root, "src/state.js"), "utf8");
const renderer = fs.readFileSync(path.join(root, "src/renderer.js"), "utf8");
const requiredContractTokens = [
  "WebSocket",
  "START_AUTH_FLOW",
  "GATEWAY_READY",
  "FLOW_EVENT",
  "FLOW_RESULT",
  "FLOW_SUCCESS",
  "FLOW_ERROR",
  "ERROR",
  "PONG"
];

for (const token of requiredContractTokens) {
  const bundle = `${app}\n${client}\n${state}`;
  if (!bundle.includes(token)) {
    throw new Error(`Frontend contract token missing: ${token}`);
  }
}

if (!renderer.includes("SENSITIVE_PATTERN")) {
  throw new Error("renderer.js must keep sensitive display filtering.");
}

fs.rmSync(dist, { recursive: true, force: true });
for (const relativePath of requiredFiles) {
  const source = path.join(root, relativePath);
  const target = path.join(dist, relativePath);
  fs.mkdirSync(path.dirname(target), { recursive: true });
  fs.copyFileSync(source, target);
}

console.log("[auth-web-demo] Build OK: static files validated and copied to dist/.");
