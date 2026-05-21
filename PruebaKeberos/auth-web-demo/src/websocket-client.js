export class GatewayWebSocketClient {
  constructor(callbacks) {
    this.callbacks = callbacks;
    this.socket = null;
  }

  connect(url) {
    this.close();
    this.socket = new WebSocket(url);

    this.socket.addEventListener("open", () => {
      this.callbacks.onOpen?.();
    });

    this.socket.addEventListener("message", (event) => {
      try {
        const message = JSON.parse(event.data);
        this.callbacks.onMessage?.(message);
      } catch (error) {
        this.callbacks.onProtocolError?.(
          "JSON invalido recibido desde el gateway WebSocket."
        );
      }
    });

    this.socket.addEventListener("close", () => {
      this.callbacks.onClose?.();
    });

    this.socket.addEventListener("error", () => {
      this.callbacks.onError?.("Gateway no disponible o conexion rechazada.");
    });
  }

  send(message) {
    if (!this.isConnected()) {
      throw new Error("WebSocket no conectado.");
    }
    this.socket.send(JSON.stringify(message));
  }

  close() {
    if (this.socket && this.socket.readyState < WebSocket.CLOSING) {
      this.socket.close();
    }
    this.socket = null;
  }

  isConnected() {
    return this.socket?.readyState === WebSocket.OPEN;
  }
}

export function assertWebSocketSupport() {
  if (!("WebSocket" in window)) {
    throw new Error("Este navegador no soporta WebSocket.");
  }
}
