Este directorio debe contener el par de llaves RSA de demostración:

  public.pem   -> llave pública (X.509 PEM)
  private.pem  -> llave privada (PKCS#8 PEM), SOLO para auth-service

Genérelas ejecutando desde la raíz del proyecto:

  sh scripts/generate-rsa-keys.sh

Los archivos .pem están ignorados por Git y NO deben publicarse.
Sin estas llaves el auth-service no puede iniciar porque no puede firmar JWT.

Requisito: OpenSSL en el PATH (incluido en Git for Windows).
