#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
APP_HOME="$(cd "${SCRIPT_DIR}/.." && pwd)"
APP_NAME="ultra-trading-order"
LOG_DIR="${APP_HOME}/logs"
RUN_DIR="${APP_HOME}/run"
PID_FILE="${RUN_DIR}/${APP_NAME}.pid"
LOG_FILE="${LOG_DIR}/${APP_NAME}.out"

JAVA_BIN="${JAVA_HOME:-}/bin/java"
if [[ ! -x "${JAVA_BIN}" ]]; then
  JAVA_BIN="java"
fi

DEFAULT_JAVA_OPTS="-Xms512m -Xmx1024m"
JAVA_OPTS="${JAVA_OPTS:-${DEFAULT_JAVA_OPTS}}"

mkdir -p "${LOG_DIR}" "${RUN_DIR}"

find_jar() {
  find "${APP_HOME}/target" -maxdepth 1 -type f -name "${APP_NAME}-*.jar" ! -name "*.original" | sort | tail -n 1
}

check_java() {
  if ! command -v "${JAVA_BIN}" >/dev/null 2>&1; then
    echo "Java not found. Please set JAVA_HOME or add java to PATH." >&2
    exit 1
  fi

  local version_line version major
  version_line="$("${JAVA_BIN}" -version 2>&1 | head -n 1)"
  version="$(echo "${version_line}" | sed -E 's/.*version "([^"]+)".*/\1/')"
  major="$(echo "${version}" | awk -F. '{print ($1 == 1 ? $2 : $1)}')"

  if [[ -z "${major}" || "${major}" -lt 17 ]]; then
    echo "Java 17+ is required. Current version: ${version_line}" >&2
    exit 1
  fi
}

is_running() {
  if [[ -f "${PID_FILE}" ]]; then
    local pid
    pid="$(cat "${PID_FILE}")"
    if [[ -n "${pid}" ]] && kill -0 "${pid}" >/dev/null 2>&1; then
      return 0
    fi
  fi
  return 1
}

start_app() {
  check_java

  if is_running; then
    echo "${APP_NAME} is already running. pid=$(cat "${PID_FILE}")"
    exit 0
  fi

  local jar_file
  jar_file="$(find_jar)"
  if [[ -z "${jar_file}" ]]; then
    echo "Jar not found under ${APP_HOME}/target. Please run: mvn -DskipTests package" >&2
    exit 1
  fi

  echo "Starting ${APP_NAME}"
  echo "Jar: ${jar_file}"
  echo "Log: ${LOG_FILE}"

  nohup "${JAVA_BIN}" ${JAVA_OPTS} -jar "${jar_file}" >> "${LOG_FILE}" 2>&1 &
  echo $! > "${PID_FILE}"
  sleep 2

  if is_running; then
    echo "${APP_NAME} started successfully. pid=$(cat "${PID_FILE}")"
  else
    echo "Failed to start ${APP_NAME}. Check ${LOG_FILE}" >&2
    rm -f "${PID_FILE}"
    exit 1
  fi
}

stop_app() {
  if ! is_running; then
    echo "${APP_NAME} is not running."
    rm -f "${PID_FILE}"
    exit 0
  fi

  local pid
  pid="$(cat "${PID_FILE}")"
  echo "Stopping ${APP_NAME}. pid=${pid}"
  kill "${pid}"

  for _ in {1..20}; do
    if kill -0 "${pid}" >/dev/null 2>&1; then
      sleep 1
    else
      rm -f "${PID_FILE}"
      echo "${APP_NAME} stopped."
      return
    fi
  done

  echo "Force killing ${APP_NAME}. pid=${pid}"
  kill -9 "${pid}" >/dev/null 2>&1 || true
  rm -f "${PID_FILE}"
  echo "${APP_NAME} stopped."
}

status_app() {
  if is_running; then
    echo "${APP_NAME} is running. pid=$(cat "${PID_FILE}")"
  else
    echo "${APP_NAME} is not running."
    exit 1
  fi
}

case "${1:-}" in
  start)
    start_app
    ;;
  stop)
    stop_app
    ;;
  restart)
    stop_app
    start_app
    ;;
  status)
    status_app
    ;;
  *)
    echo "Usage: $0 {start|stop|restart|status}" >&2
    exit 1
    ;;
esac
