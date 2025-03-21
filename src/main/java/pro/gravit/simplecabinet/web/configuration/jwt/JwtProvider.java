package pro.gravit.simplecabinet.web.configuration.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pro.gravit.simplecabinet.web.model.user.User;
import pro.gravit.simplecabinet.web.model.user.UserSession;
import pro.gravit.simplecabinet.web.service.KeyManagementService;
import pro.gravit.simplecabinet.web.service.user.UserDetailsService;
import pro.gravit.simplecabinet.web.service.user.UserGroupService;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;

@Component
public class JwtProvider {
    @Autowired
    private JwtParserProvider parserProvider;
    @Autowired
    private KeyManagementService keyManagementService;
    @Autowired
    private UserDetailsService userDetailsService;
    @Autowired
    private UserGroupService userGroupService;

    public GeneratedJWTToken generateToken(UserSession session) {
        LocalDateTime dateTime = LocalDateTime.now().plusDays(15);
        var token = makeBasic(session)
                .expiration(Date.from(dateTime.toInstant(ZoneOffset.UTC)))
                .compact();
        return new GeneratedJWTToken(token, dateTime);
    }

    public GeneratedJWTToken generateNoExpiredJWTToken(UserSession session) {
        LocalDateTime dateTime = LocalDateTime.now().plusYears(5);
        var token = makeBasic(session)
                .compact();
        return new GeneratedJWTToken(token, dateTime);
    }

    private JwtBuilder makeBasic(UserSession session) {
        User user = session.getUser();
        var roles = userGroupService.findByUser(user);
        return Jwts.builder()
                .subject(user.getUsername())
                .issuer("SimpleCabinet")
                .claim("roles", roles.stream().map(e -> e.getGroup().getId()).toList())
                .claim("id", user.getId())
                .claim("sessionId", session.getId())
                .claim("client", session.getClient())
                .signWith(keyManagementService.getPrivateKey(), Jwts.SIG.ES256);
    }

    @SuppressWarnings("unchecked")
    public UserDetailsService.CabinetUserDetails getDetailsFromToken(String token) {
        Claims claims = parserProvider.makeParser()
                .parseSignedClaims(token).getPayload();
        List<String> roles = claims.get("roles", List.class);
        String client = claims.get("client", String.class);
        long userId = claims.get("id", Long.class);
        long sessionId = claims.get("sessionId", Long.class);
        return userDetailsService.create(userId, claims.getSubject(), roles, client, sessionId);
    }

    public boolean validateToken(String token) {
        try {
            parserProvider.makeParser().parseSignedClaims(token);
            return true;
        } catch (Exception ignored) {
        }
        return false;
    }

    public record GeneratedJWTToken(String token, LocalDateTime endTime) {
        public long getExpire() {
            return Duration.between(LocalDateTime.now(), endTime).toMillis();
        }
    }
}
