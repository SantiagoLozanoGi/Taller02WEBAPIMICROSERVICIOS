Este directorio debe contener la llave PÚBLICA RSA de demostración:

  public.pem -> llave pública (X.509 PEM), pareja de la llave privada de auth-service

Genérela ejecutando desde la raíz del proyecto:

  sh scripts/generate-rsa-keys.sh

El script copia aquí ÚNICAMENTE la llave pública: user-service solo valida
firmas y nunca debe conocer la llave privada. El archivo .pem está ignorado
por Git y NO debe publicarse.

Si regenera las llaves, todos los tokens emitidos con la llave anterior
dejan de ser válidos: los usuarios deben iniciar sesión de nuevo.
