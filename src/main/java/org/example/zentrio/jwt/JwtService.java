package org.example.zentrio.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.example.zentrio.model.AppUser;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;

@Slf4j
@Component
public class JwtService {

    public static final long JWT_TOKEN_VALIDITY = 30 * 24 * 60 * 60;  //5 hour
    public static final String SECRET = "FVPr6Q/fVlHGZkElZubC0Zaxv657dPUfDQ4o9DADjSin7+uST1d2A5klMWrMK8fmSl3doyf2wn5zj56VC+qqCg==";



    private String createToken(Map<String, Object> claim, String subject) {
        return Jwts.builder()
                .claims(claim)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + JWT_TOKEN_VALIDITY * 1000))
                .signWith(getSignKey()).compact();
    }

    private SecretKey getSignKey() {
        byte[] keyBytes = Base64.getDecoder().decode(SECRET);
        return Keys.hmacShaKeyFor(keyBytes);
    }




    //2. generate token for user
    public String generateToken(String identifier) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, identifier);
    }

    //3. retrieving any information from token we will need the secret key
    private Claims extractAllClaim(String token) {
        log.info("Extracting claims from token {}", Jwts.parser()
                .verifyWith(getSignKey())
                .build()
                .parseSignedClaims(token)
                .getPayload());
        return Jwts.parser()
                .verifyWith(getSignKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    //4. extract a specific claim from the JWT token’s claims.
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaim(token);
        return claimsResolver.apply(claims);
    }


    //5. retrieve username from jwt token
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    //6. retrieve expiration date from jwt token
    public Date extractExpirationDate(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    //7. check expired token
    private Boolean isTokenExpired(String token) {
        return extractExpirationDate(token).before(new Date());
    }

//    8. validate token
    public Boolean validateToken(String token, AppUser appUser) {
        final String email = extractEmail(token);
        return ((email.equals(appUser.getEmail())) && !isTokenExpired(token));
    }

    public String generateAccessToken(UUID userId) {
        return Jwts.builder()
                .setSubject(userId.toString())
                .setIssuedAt(new Date())
                .setExpiration(Date.from(Instant.now().plusSeconds(60*15)))
                .signWith(SignatureAlgorithm.HS512, SECRET)
                .compact();
    }
}