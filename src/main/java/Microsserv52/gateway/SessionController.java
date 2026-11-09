package Microsserv52.gateway;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/session")
public class SessionController {
    private final RestClient auth;
    private final RestClient account;
    private final boolean secure;
    private final String cookiePath;

    public SessionController(@Value("${AUTH_URL:http://auth:8080}") String authUrl,
                             @Value("${ACCOUNT_URL:http://account:8080}") String accountUrl,
                             @Value("${SESSION_COOKIE_SECURE:true}") boolean secure,
                             @Value("${SESSION_COOKIE_PATH:/api}") String cookiePath) {
        var factory = new JdkClientHttpRequestFactory(java.net.http.HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build());
        factory.setReadTimeout(Duration.ofSeconds(10));
        this.auth = RestClient.builder().requestFactory(factory).baseUrl(authUrl).build();
        this.account = RestClient.builder().requestFactory(factory).baseUrl(accountUrl).build();
        this.secure = secure;
        this.cookiePath = cookiePath;
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        return Map.of("token", token.getToken(), "headerName", token.getHeaderName());
    }

    @PostMapping("/login")
    public ResponseEntity<Void> login(@RequestBody Login input) {
        if (input.email() == null || input.password() == null || input.email().isBlank() ||
            input.password().isBlank() || input.email().length() > 254 || input.password().length() > 128) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid credentials");
        }
        try {
            Token token = auth.post().uri("/auth/login").body(input).retrieve().body(Token.class);
            if (token == null || token.accessToken() == null || token.expiresIn() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Invalid authentication response");
            }
            return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE,
                cookie(token.accessToken(), token.expiresIn()).toString()).build();
        } catch (RestClientResponseException error) {
            throw new ResponseStatusException(error.getStatusCode().value() == 401
                ? HttpStatus.UNAUTHORIZED : HttpStatus.BAD_GATEWAY, "Authentication failed");
        } catch (RestClientException error) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Authentication unavailable");
        }
    }

    @GetMapping
    public Profile profile(@AuthenticationPrincipal Jwt jwt) {
        try {
            return account.get().uri("/accounts/me").headers(headers -> headers.setBearerAuth(jwt.getTokenValue()))
                .retrieve().body(Profile.class);
        } catch (RestClientResponseException error) {
            throw new ResponseStatusException(error.getStatusCode().is4xxClientError()
                ? HttpStatus.UNAUTHORIZED : HttpStatus.BAD_GATEWAY, "Account unavailable");
        } catch (RestClientException error) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Account unavailable");
        }
    }

    @DeleteMapping
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie("", 0).toString()).build();
    }

    private ResponseCookie cookie(String value, long age) {
        return ResponseCookie.from(CookieBearerTokenFilter.COOKIE_NAME, value).httpOnly(true)
            .secure(secure).sameSite("Lax").path(cookiePath).maxAge(age).build();
    }

    public record Login(String email, String password) {}
    public record Token(String accessToken, String tokenType, long expiresIn) {}
    public record Profile(UUID id, String name, String email, String roles, boolean enabled) {}
}
