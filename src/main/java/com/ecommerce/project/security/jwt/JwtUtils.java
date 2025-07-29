package com.ecommerce.project.security.jwt;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.security.Key;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtUtils {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    @Value("${spring.app.jwtExpirationMs}")
    private Long jwtExpirationMs;

    @Value("${spring.app.jwtSecret}")
    private String jwtSecret;

    public String getJwtFromHeader (HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        logger.debug("Bearer Token: {}", bearerToken);

        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    public String generateTokenFromUsername (UserDetails userDetails) {
        String username = userDetails.getUsername();
        Instant now = Instant.now();
        Instant expirationInstant = now.plusMillis(jwtExpirationMs);

        return Jwts.builder()
                .subject(username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expirationInstant))
                .signWith(key())
                .compact();
    }

    public String getUserNameFromToken (String token) {
        return Jwts.parser()
                .verifyWith((SecretKey) key())
                .build().parseSignedClaims(token)
                .getPayload().getSubject();
    }

    public Key key () {
        return Keys.hmacShaKeyFor(
                Decoders.BASE64.decode(jwtSecret)
        );
    }

    public boolean validateJwtToken (String authToken) {
        try {
            System.out.println("validateJwtToken");
            Jwts.parser()
                    .verifyWith((SecretKey) key())
                    .build().parseSignedClaims(authToken);
            return true;
        } catch (MalformedJwtException exception) {
            logger.info("JWT token is invalid: {}", exception.getMessage());
        } catch (ExpiredJwtException exception) {
            logger.info("JWT token is expired: {}", exception.getMessage());
        } catch (UnsupportedJwtException exception) {
            logger.info("JWT claims string is unsupported: {}", exception.getMessage());
        } catch (IllegalArgumentException exception) {
            logger.info("JWT claims string is empty: {}", exception.getMessage());
        }
        return false;
    }
}
