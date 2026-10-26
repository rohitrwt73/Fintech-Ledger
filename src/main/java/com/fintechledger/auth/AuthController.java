package com.fintechledger.auth;

import com.fintechledger.config.SecurityConfig;
import com.fintechledger.common.ApiException;
import com.fintechledger.user.*;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/auth")
public class AuthController {
 record Register(@Email @NotBlank String email,@NotBlank @Size(min=10,max=72) String password,@NotBlank @Size(max=120) String fullName) {}
 record Login(@Email @NotBlank String email,@NotBlank String password) {}
 private final UserRepository users; private final PasswordEncoder encoder; private final String secret; private final boolean secure;
 public AuthController(UserRepository users,PasswordEncoder encoder,@Value("${app.jwt-secret}") String secret,@Value("${app.cookie-secure}") boolean secure){this.users=users;this.encoder=encoder;this.secret=secret;this.secure=secure;}
 @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED) Map<String,Object> register(@Valid @RequestBody Register body,HttpServletResponse response){
  String email=body.email().toLowerCase(); if(users.existsByEmail(email)) throw new ApiException(HttpStatus.CONFLICT,"Email is already registered");
  var user=users.save(new AppUser(email,encoder.encode(body.password()),body.fullName().trim())); setCookie(response,user.getEmail(),user.getRole()); return profile(user);
 }
 @PostMapping("/login") Map<String,Object> login(@Valid @RequestBody Login body,HttpServletResponse response){
  var user=users.findByEmail(body.email().toLowerCase()).filter(u->encoder.matches(body.password(),u.getPasswordHash())).orElseThrow(()->new ApiException(HttpStatus.UNAUTHORIZED,"Invalid email or password"));
  setCookie(response,user.getEmail(),user.getRole()); return profile(user);
 }
 @PostMapping("/logout") Map<String,String> logout(HttpServletResponse response){response.addHeader("Set-Cookie",ResponseCookie.from("ledger_session","").httpOnly(true).secure(secure).sameSite("Strict").path("/").maxAge(0).build().toString());return Map.of("message","Logged out");}
 @GetMapping("/csrf") Map<String,String> csrf(org.springframework.security.web.csrf.CsrfToken token){return Map.of("token",token.getToken());}
 @GetMapping("/me") Map<String,Object> me(org.springframework.security.core.Authentication auth){return users.findByEmail(auth.getName()).map(this::profile).orElseThrow(()->new ApiException(HttpStatus.UNAUTHORIZED,"Not authenticated"));}
 private void setCookie(HttpServletResponse response,String email,String role){var key=Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));response.addHeader("Set-Cookie",ResponseCookie.from("ledger_session",SecurityConfig.issue(key,email,role)).httpOnly(true).secure(secure).sameSite("Strict").path("/").maxAge(86400).build().toString());}
 private Map<String,Object> profile(AppUser u){return Map.of("id",u.getId(),"email",u.getEmail(),"fullName",u.getFullName(),"role",u.getRole());}
}
