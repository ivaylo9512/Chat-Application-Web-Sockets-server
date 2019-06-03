package security;

import models.UserDetails;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class Jwt {
    private static String jwtSecret = "MyJwtSecret";
    private static int jwtExpirationInMs = 10000000;
    private static byte[] encodedJwtSecret = Base64.getEncoder().encode(jwtSecret.getBytes());

    static String generate(UserDetails user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        Collection<GrantedAuthority> grantedAuth = user.getAuthorities();
        Set<String> authorities = AuthorityUtils.authorityListToSet(grantedAuth);

        Claims claims = Jwts.claims()
                .setSubject(user.getUsername())
                .setId(String.valueOf(user.getId()));
        claims.put("roles", authorities);

        return Jwts.builder()
                .setClaims(claims)
                .signWith(SignatureAlgorithm.HS512, new String(encodedJwtSecret))
                .setIssuedAt(new Date())
                .setExpiration(expiryDate)
                .compact();
    }

    public static UserDetails validate(String token) {
        UserDetails user;
        try {
            Claims body = Jwts.parser()
                    .setSigningKey(new String(encodedJwtSecret))
                    .parseClaimsJws(token)
                    .getBody();

            List<SimpleGrantedAuthority> authorities = ((ArrayList<String>)body.get("roles")).stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

            user = new UserDetails(body.getSubject(), token, authorities, Integer.parseInt(body.getId()));
        } catch (ExpiredJwtException e) {
                throw new BadCredentialsException("Jwt token has expired.");
        } catch (Exception e) {
            throw new BadCredentialsException("Jwt token is incorrect");
        }
        return user;
    }
}