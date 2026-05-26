#!/usr/bin/env sh
set -eu

cd "$(dirname "$0")/.."
PORT="${AUTH_SAMPLE_LOGIN_PORT:-5174}"

if command -v python3 >/dev/null 2>&1; then
  echo "sample-login-app escuchando en http://127.0.0.1:$PORT"
  exec python3 -m http.server "$PORT" --bind 127.0.0.1 --directory sample-login-app
fi

if command -v python >/dev/null 2>&1; then
  echo "sample-login-app escuchando en http://127.0.0.1:$PORT"
  exec python -m http.server "$PORT" --bind 127.0.0.1 --directory sample-login-app
fi

echo "Python no esta disponible para servir sample-login-app." >&2
exit 1
