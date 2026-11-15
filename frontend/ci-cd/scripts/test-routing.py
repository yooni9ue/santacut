"""Exercise the shared Caddyfile against local stub servers, without TLS or AWS."""
import argparse
import os
from pathlib import Path
import socket
import subprocess
import tempfile
import threading
import time
import urllib.error
import urllib.request
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer


def start_server(label):
    class Handler(BaseHTTPRequestHandler):
        def do_GET(self):
            self.send_response(200)
            self.end_headers()
            self.wfile.write(label.encode())

        def log_message(self, *_args):
            pass

    server = ThreadingHTTPServer(("127.0.0.1", 0), Handler)
    threading.Thread(target=server.serve_forever, daemon=True).start()
    return server


def response_at(port, path):
    try:
        with urllib.request.urlopen(f"http://127.0.0.1:{port}{path}", timeout=2) as response:
            return response.read().decode()
    except urllib.error.HTTPError as error:
        return str(error.code)


def check_routes(caddy, config, backend, frontend, enabled):
    with socket.socket() as listener:
        listener.bind(("127.0.0.1", 0))
        port = listener.getsockname()[1]
    env = os.environ.copy()
    env.update(DOMAIN=f"http://127.0.0.1:{port}", ACME_EMAIL="test@example.com")
    env.pop("FRONTEND_UPSTREAM", None)
    if enabled:
        env["FRONTEND_UPSTREAM"] = f"127.0.0.1:{frontend.server_port}"
    with tempfile.TemporaryFile() as log:
        process = subprocess.Popen(
            [caddy, "run", "--config", str(config), "--adapter", "caddyfile"],
            env=env, stdout=log, stderr=log,
        )
        try:
            for _attempt in range(50):
                try:
                    response_at(port, "/")
                    break
                except OSError:
                    if process.poll() is not None:
                        log.seek(0)
                        raise RuntimeError(log.read().decode())
                    time.sleep(0.1)
            routes = [
                ("/", "frontend" if enabled else "backend"),
                ("/v1/regions", "backend"),
                ("/s/example", "backend"),
                ("/v3/api-docs", "backend"),
                ("/actuator", "404"),
                ("/actuator/health", "404"),
            ]
            for path, expected in routes:
                actual = response_at(port, path)
                assert actual == expected, (enabled, path, actual, expected)
            return len(routes)
        finally:
            process.terminate()
            process.wait(timeout=10)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--caddy", required=True)
    args = parser.parse_args()
    repository = Path(__file__).resolve().parents[3]
    backend, frontend = start_server("backend"), start_server("frontend")
    try:
        with tempfile.TemporaryDirectory(prefix="santacut-routing-") as directory:
            config = Path(directory) / "Caddyfile"
            source = (repository / "backend/ci-cd/deploy/Caddyfile").read_text()
            config.write_text(source.replace("app:8080", f"127.0.0.1:{backend.server_port}"))
            count = sum(check_routes(args.caddy, config, backend, frontend, enabled) for enabled in (False, True))
            print(f"Caddy HTTP routing: {count} checks passed (frontend off/on, API/share/Swagger/internal paths).")
    finally:
        backend.shutdown()
        frontend.shutdown()


if __name__ == "__main__":
    main()
