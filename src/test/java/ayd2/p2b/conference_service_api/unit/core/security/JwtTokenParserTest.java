package ayd2.p2b.conference_service_api.unit.core.security;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.core.security.JwtProperties;
import ayd2.p2b.conference_service_api.core.security.JwtTokenParser;
import ayd2.p2b.conference_service_api.core.security.TokenType;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenParserTest {

    @Test
    void shouldRejectTokenWhenRoleClaimContainsUnknownValue() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("test_secret_key_with_at_least_32_chars");
        JwtTokenParser parser = new JwtTokenParser(properties);

        String token = Jwts.builder()
                .claim("tokenType", TokenType.ACCESS.name())
                .claim("userId", UUID.randomUUID().toString())
                .claim("email", "admin@example.com")
                .claim("roles", List.of("SYSTEM_ADMIN", "UNKNOWN_ROLE"))
                .signWith(Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertThatThrownBy(() -> parser.parseAccessToken(token))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(apiException.getCode()).isEqualTo("auth.token_invalid");
                });
    }
}
