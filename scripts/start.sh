#!/usr/bin/env bash

set -e

BASE_DIR="$(cd "$(dirname "$0")/.." && pwd)"
LOG_DIR="${BASE_DIR}/logs"
JAR_FILE="${BASE_DIR}/target/ultra-trading-order-1.0.0-SNAPSHOT.jar"

mkdir -p "${LOG_DIR}"

nohup java -jar "${JAR_FILE}" > "${LOG_DIR}/start.log" 2>&1 &

echo "started"
echo "log file: ${LOG_DIR}/start.log"
