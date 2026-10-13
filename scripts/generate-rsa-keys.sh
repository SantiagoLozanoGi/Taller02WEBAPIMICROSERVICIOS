#!/usr/bin/env sh
set -eu

ROOT_DIR="$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
TMP_DIR="$ROOT_DIR/.keys-tmp"

mkdir -p "$TMP_DIR"

openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -out "$TMP_DIR/private.pem"
openssl rsa -pubout -in "$TMP_DIR/private.pem" -out "$TMP_DIR/public.pem"

# La llave privada SOLO se distribuye al emisor de tokens (auth-service).
cp "$TMP_DIR/private.pem" "$ROOT_DIR/auth-service/src/main/resources/keys/private.pem"
cp "$TMP_DIR/public.pem" "$ROOT_DIR/auth-service/src/main/resources/keys/public.pem"

# El validador (user-service) recibe únicamente la llave pública.
cp "$TMP_DIR/public.pem" "$ROOT_DIR/user-service/src/main/resources/keys/public.pem"

rm -rf "$TMP_DIR"

echo "Llaves RSA de demostración generadas y copiadas a los servicios."
echo "Recuerde: estos archivos .pem estan ignorados por Git y no deben publicarse."
