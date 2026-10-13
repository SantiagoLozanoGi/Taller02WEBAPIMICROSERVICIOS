package edu.unibague.gateway.filter;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void generatesCorrelationIdWhenClientDoesNotSendOne() {
        MockServerWebExchange exchange =
                MockServerWebExchange.from(MockServerHttpRequest.get("/api/users").build());
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain).filter(captor.capture());

        String propagated = captor.getValue().getRequest().getHeaders()
                .getFirst(CorrelationIdFilter.HEADER_NAME);
        assertThat(propagated).isNotBlank();
        assertThatCode(() -> UUID.fromString(propagated)).doesNotThrowAnyException();
        assertThat(exchange.getResponse().getHeaders().getFirst(CorrelationIdFilter.HEADER_NAME))
                .isEqualTo(propagated);
    }

    @Test
    void reusesCorrelationIdSentByClient() {
        String incoming = "correlation-id-de-prueba";
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/users")
                        .header(CorrelationIdFilter.HEADER_NAME, incoming)
                        .build());
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        verify(chain).filter(any(ServerWebExchange.class));
        assertThat(exchange.getResponse().getHeaders().getFirst(CorrelationIdFilter.HEADER_NAME))
                .isEqualTo(incoming);
    }

    @Test
    void reusesCorrelationIdOnResponseForDownstreamRequests() {
        MockServerWebExchange exchange =
                MockServerWebExchange.from(MockServerHttpRequest.get("/api/auth/login").build());
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());

        filter.filter(exchange, chain).block();

        // La misma correlación viaja hacia el servicio y hacia el cliente.
        String responseHeader = exchange.getResponse().getHeaders()
                .getFirst(CorrelationIdFilter.HEADER_NAME);
        ArgumentCaptor<ServerWebExchange> captor = ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain).filter(captor.capture());
        assertThat(captor.getValue().getRequest().getHeaders()
                .getFirst(CorrelationIdFilter.HEADER_NAME)).isEqualTo(responseHeader);
    }
}
