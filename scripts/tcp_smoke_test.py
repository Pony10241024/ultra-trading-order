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
    parser = argparse.ArgumentParser(description="TCP smoke test for Ultra Trading Order")
    parser.add_argument("--host", default="127.0.0.1", help="TCP host, default: 127.0.0.1")
    parser.add_argument("--port", type=int, default=5502, help="TCP port, default: 5502")
    parser.add_argument("--main-account-id", default="main001", help="Main account id, default: main001")
    parser.add_argument("--trade-account", default="trade001", help="Trade account, default: trade001")
    parser.add_argument("--symbol", default="BTCUSDT", help="Test symbol, default: BTCUSDT")
    parser.add_argument("--asset", default="USDT", help="Funding asset, default: USDT")
    parser.add_argument("--price", default="50000", help="Order price, default: 50000")
    parser.add_argument("--quantity", default="0.01", help="Order quantity, default: 0.01")
    parser.add_argument("--skip-order", action="store_true", help="Do not send order and cancel requests")
    parser.add_argument("--skip-cancel", action="store_true", help="Do not send cancel request")
    args = parser.parse_args()

    with socket.create_connection((args.host, args.port), timeout=10) as sock:
        sock.settimeout(10)

        symbols = request(sock, "SYMBOL_LIST_REQUEST", {})
        if not isinstance(symbols, list) or not symbols:
            raise RuntimeError("SYMBOL_LIST_REQUEST returned empty symbol list")

        request(sock, "SYMBOL_INFO_REQUEST", {"symbol": args.symbol})

        request(sock, "ASSET_BALANCE_REQUEST", {
            "mainAccountId": args.main_account_id,
            "tradeAccount": args.trade_account,
            "asset": args.asset,
        })

        request(sock, "ASSET_BALANCES_REQUEST", {
            "mainAccountId": args.main_account_id,
            "tradeAccount": args.trade_account,
        })

        request(sock, "ORDER_LIST_REQUEST", {
            "tradeAccount": args.trade_account,
            "symbol": args.symbol,
        })

        request(sock, "ORDER_TODAY_REQUEST", {
            "tradeAccount": args.trade_account,
            "symbol": args.symbol,
        })

        order_id = None
        if not args.skip_order:
            order = request(sock, "ORDER_REQUEST", {
                "mainAccountId": args.main_account_id,
                "tradeAccount": args.trade_account,
                "symbol": args.symbol,
                "orderType": "LIMIT",
                "side": "BUY",
                "price": args.price,
                "quantity": args.quantity,
                "clientOrderId": "tcp-smoke-test",
            })
            order_id = order["orderId"]

            request(sock, "TRADE_LIST_REQUEST", {
                "tradeAccount": args.trade_account,
                "orderId": order_id,
            })

            if not args.skip_cancel:
                request(sock, "CANCEL_REQUEST", {
                    "tradeAccount": args.trade_account,
                    "orderId": order_id,
                })

        request(sock, "ASSET_FLOW_REQUEST", {
            "tradeAccount": args.trade_account,
            "asset": args.asset,
            "limit": 20,
        })

        print("\nTCP smoke test finished successfully.")
        if order_id:
            print(f"Last orderId: {order_id}")


if __name__ == "__main__":
    try:
        main()
    except Exception as exc:
        print(f"\nTCP smoke test failed: {exc}", file=sys.stderr)
        sys.exit(1)
