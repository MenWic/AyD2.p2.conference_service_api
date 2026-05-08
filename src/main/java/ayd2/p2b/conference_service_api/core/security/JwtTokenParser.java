package ayd2.p2b.conference_service_api.core.security;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class JwtTokenParser {

    private final JwtProperties properties;

    public JwtTokenParser(JwtProperties properties) {
        this.properties = properties;
    }

    @SuppressWarnings("unchecked")
    public AuthenticatedUser parseAccessToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String tokenType = claims.get("tokenType", String.class);
            if (!TokenType.ACCESS.name().equals(tokenType)) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "auth.token_invalid", "Invalid token type");
            }

            UUID userId = UUID.fromString(claims.get("userId", String.class));
            String email = claims.get("email", String.class);
            List<String> roles = claims.get("roles", List.class);

            return AuthenticatedUser.builder()
                    .userId(userId)
                    .email(email)
                    .roles(toRoles(roles))
                    .build();
        } catch (ExpiredJwtException ex) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "auth.token_expired", "Token expired");
        } catch (JwtException | IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "auth.token_invalid", "Invalid token");
        }
    }

    private Set<Role> toRoles(List<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return Set.of();
        }
        return roles.stream()
                .map(Role::valueOf)
                .collect(Collectors.toSet());
    }

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }
}
