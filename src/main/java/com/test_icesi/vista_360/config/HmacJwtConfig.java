package com.test_icesi.vista_360.config;

import java.nio.charset.StandardCharsets;

import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

/**
 * Decodificador de JWT basado en secreto simétrico (HS256).
 *
 * <p>Se activa únicamente cuando se define {@code app.security.jwt.hmac-secret}, lo que
 * permite ejecutar y probar el servicio en local sin un proveedor OIDC real. En entornos
 * reales se define {@code spring.security.oauth2.resourceserver.jwt.issuer-uri} y Spring
 * autoconfigura el decodificador contra las llaves públicas de la plataforma de identidad.
 */
@Configuration
@ConditionalOnProperty(name = "app.security.jwt.hmac-secret")
public class HmacJwtConfig {

    @Bean
    JwtDecoder jwtDecoder(@org.springframework.beans.factory.annotation.Value(
            "${app.security.jwt.hmac-secret}") String secret) {
        var key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    }
}
