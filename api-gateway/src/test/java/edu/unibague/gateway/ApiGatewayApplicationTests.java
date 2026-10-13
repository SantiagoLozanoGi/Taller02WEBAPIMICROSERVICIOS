package edu.unibague.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Verifica que el contexto del Gateway arranca sin errores: rutas,
 * CORS, actuator y filtro de correlación.
 */
@SpringBootTest
class ApiGatewayApplicationTests {

    @Test
    void contextLoads() {
    }
}
