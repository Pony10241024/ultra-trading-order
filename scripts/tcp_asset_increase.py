#!/usr/bin/env python3
import argparse
import json
import socket
import struct
import sys
import time
import uuid


def build_gateway_message(msg_type, data):
    return {
        "msgType": msg_type,
        "msgId": str(uuid.uuid4()),
        "timestamp": int(time.time() * 1000),
        "data": json.dumps(data, ensure_ascii=False),
    }


def send_message(sock, message):
    payload = json.dumps(message, ensure_ascii=False).encode("utf-8")
    sock.sendall(struct.pack(">I", len(payload)) + payload)


def recv_exact(sock, size):
    chunks = []
    remaining = size
    while remaining > 0:
        chunk = sock.recv(remaining)
        if not chunk:
            raise ConnectionError("Socket closed while receiving data")
        chunks.append(chunk)
        remaining -= len(chunk)
    return b"".join(chunks)


def receive_message(sock):
    length = struct.unpack(">I", recv_exact(sock, 4))[0]
    payload = recv_exact(sock, length)
    outer = json.loads(payload.decode("utf-8"))
    inner = json.loads(outer["data"]) if outer.get("data") else None
    return outer, inner


def request(sock, msg_type, data):
    message = build_gateway_message(msg_type, data)
    print(f"\n>>> {msg_type}")
    print(json.dumps(data, ensure_ascii=False, indent=2))
    send_message(sock, message)
    outer, inner = receive_message(sock)
    print(f"<<< {outer['msgType']}")
    print(json.dumps(inner, ensure_ascii=False, indent=2))
    if inner is None:
        raise RuntimeError(f"{msg_type} returned empty response body")
    if inner.get("code") != 0:
        raise RuntimeError(f"{msg_type} failed: {inner.get('message')}")
    return inner.get("data")


def main():
    parser = argparse.ArgumentParser(description="TCP asset increase tool for Ultra Trading Order")
    parser.add_argument("--host", default="127.0.0.1", help="TCP host, default: 127.0.0.1")
    parser.add_argument("--port", type=int, default=5502, help="TCP port, default: 5502")
    parser.add_argument("--main-account-id", default="main001", help="Main account id, default: main001")
    parser.add_argument("--trade-account", default="trade001", help="Trade account, default: trade001")
    parser.add_argument("--asset", default="USDT", help="Asset code, default: USDT")
    parser.add_argument("--amount", default="100000", help="Increase amount, default: 100000")
    parser.add_argument("--description", default="tcp asset increase", help="Description, default: tcp asset increase")
    parser.add_argument("--query-balance", action="store_true", help="Query balance again after increase")
    args = parser.parse_args()

    with socket.create_connection((args.host, args.port), timeout=10) as sock:
        sock.settimeout(10)

        balance = request(sock, "ASSET_INCREASE_REQUEST", {
            "mainAccountId": args.main_account_id,
            "tradeAccount": args.trade_account,
            "asset": args.asset,
            "amount": args.amount,
            "description": args.description,
        })

        if args.query_balance:
            balance = request(sock, "ASSET_BALANCE_REQUEST", {
                "mainAccountId": args.main_account_id,
                "tradeAccount": args.trade_account,
                "asset": args.asset,
            })

        print("\nTCP asset increase finished successfully.")
        print(json.dumps(balance, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    try:
        main()
    except Exception as exc:
        print(f"\nTCP asset increase failed: {exc}", file=sys.stderr)
        sys.exit(1)
