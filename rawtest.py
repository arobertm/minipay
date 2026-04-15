import socket
import time

def raw_http(host, port, path, timeout=5):
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        s.settimeout(timeout)
        s.connect((host, port))
        req = f"GET {path} HTTP/1.0\r\nHost: {host}\r\nConnection: close\r\n\r\n"
        s.sendall(req.encode())
        data = b""
        while True:
            try:
                chunk = s.recv(4096)
                if not chunk:
                    break
                data += chunk
            except socket.timeout:
                break
        s.close()
        return data.decode('utf-8', errors='replace')
    except Exception as e:
        return f"ERROR: {e}"

print("=== Testing 8081 /actuator/health ===")
print(raw_http('127.0.0.1', 8081, '/actuator/health'))

print("\n=== Testing 8084 /actuator/health ===")
print(raw_http('127.0.0.1', 8084, '/actuator/health'))

print("\n=== Testing 8090 /health ===")
print(raw_http('127.0.0.1', 8090, '/health'))
