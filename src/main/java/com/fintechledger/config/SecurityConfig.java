package com.fintechledger.config;

import com.fintechledger.user.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Supplier;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.*;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.cors.CorsConfiguration;

@Configuration @EnableMethodSecurity
public class SecurityConfig {
    @Value("${app.jwt-secret}") private String jwtSecret;
    @Value("${app.frontend-origin}") private String frontendOrigin;
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }
    @Bean SecretKey jwtKey() { return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)); }
    @Bean SecurityFilterChain filterChain(HttpSecurity http, UserRepository users, SecretKey key) throws Exception {
        var filter = new OncePerRequestFilter() {
            @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
                String token = null;
                if (request.getCookies() != null) for (Cookie c : request.getCookies()) if ("ledger_session".equals(c.getName())) token = c.getValue();
                if (token != null && org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication() == null) {
                    try {
                        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
                        var user = users.findByEmail(claims.getSubject()).orElse(null);
                        if (user != null) {
                            var auth = new UsernamePasswordAuthenticationToken(user.getEmail(), null, java.util.List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole())));
                            org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
                        }
                    } catch (RuntimeException ignored) { }
                }
                chain.doFilter(request, response);
            }
        };
        http.csrf(c -> {
                    var repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
                    repository.setCookieCustomizer(cookie -> cookie
                            .secure(true)
                            .sameSite("None")
                            .path("/")
                    );

                    c.csrfTokenRepository(repository)
                            .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler());
                })
          .cors(c -> c.configurationSource(req -> { var cors = new CorsConfiguration(); cors.setAllowedOrigins(java.util.List.of(frontendOrigin)); cors.setAllowedMethods(java.util.List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS")); cors.setAllowedHeaders(java.util.List.of("Content-Type","X-XSRF-TOKEN","Idempotency-Key")); cors.setAllowCredentials(true); return cors; }))
          .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
          .authorizeHttpRequests(a -> a.requestMatchers("/", "/api/v1/auth/register","/api/v1/auth/login","/api/v1/auth/csrf","/actuator/health","/v3/api-docs/**","/swagger-ui/**","/swagger-ui.html").permitAll().requestMatchers("/api/v1/admin/**").hasRole("ADMIN").anyRequest().authenticated())
          .addFilterBefore(filter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
    public static String issue(SecretKey key, String subject, String role) { return Jwts.builder().subject(subject).claim("role", role).issuedAt(new Date()).expiration(new Date(System.currentTimeMillis()+86400000)).signWith(key).compact(); }

    private static final class SpaCsrfTokenRequestHandler
            implements CsrfTokenRequestHandler {

        private final CsrfTokenRequestHandler plain =
                new CsrfTokenRequestAttributeHandler();

        private final CsrfTokenRequestHandler xor =
                new XorCsrfTokenRequestAttributeHandler();

        @Override
        public void handle(
                HttpServletRequest request,
                HttpServletResponse response,
                Supplier<CsrfToken> csrfToken) {

            xor.handle(request, response, csrfToken);
            csrfToken.get();
        }

        @Override
        public String resolveCsrfTokenValue(
                HttpServletRequest request,
                CsrfToken csrfToken) {

            String header = request.getHeader(csrfToken.getHeaderName());

            if (StringUtils.hasText(header)) {
                // The frontend sends the masked token returned by the JSON endpoint.
                // Decode it before comparing it with the raw cookie token.
                return xor.resolveCsrfTokenValue(request, csrfToken);
            }

            return xor.resolveCsrfTokenValue(request, csrfToken);
        }
    }
}
