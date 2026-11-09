package Microsserv52.gateway;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.time.Instant;
import java.util.List;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.bind.annotation.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.security.web.csrf.CsrfToken;
import com.sun.net.httpserver.HttpServer;

@SpringBootTest(properties = "platform.jwt-secret=test-secret-with-at-least-32-characters")
@Import(BrowserSecurityTests.Probe.class)
class BrowserSecurityTests {
    private static final String SECRET = "test-secret-with-at-least-32-characters";
    @Autowired WebApplicationContext context;
    MockMvc mvc;
    static final HttpServer upstream = upstream();

    static HttpServer upstream() {
        try {
            var server = HttpServer.create(new java.net.InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/auth/login", exchange -> {
                byte[] body = ("{\"accessToken\":\"" + token("ROLE_USER") + "\",\"tokenType\":\"Bearer\",\"expiresIn\":60}").getBytes(java.nio.charset.StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body); exchange.close();
            });
            server.createContext("/accounts/me", exchange -> {
                byte[] body = "{\"id\":\"00000000-0000-4000-8000-000000000001\",\"name\":\"Fixture\",\"email\":\"fixture@example.test\",\"roles\":\"ROLE_USER\",\"enabled\":true}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body); exchange.close();
            });
            server.start(); return server;
        } catch (java.io.IOException error) { throw new IllegalStateException(error); }
    }
    @DynamicPropertySource static void urls(DynamicPropertyRegistry properties) {
        properties.add("AUTH_URL", () -> "http://127.0.0.1:" + upstream.getAddress().getPort());
        properties.add("ACCOUNT_URL", () -> "http://127.0.0.1:" + upstream.getAddress().getPort());
    }

    @BeforeEach void setup() { mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build(); }

    static String token(String role) {
        var encoder = new NimbusJwtEncoder(new ImmutableSecret<>(SECRET.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),
            JwtClaimsSet.builder().subject("00000000-0000-4000-8000-000000000001")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).claim("roles", List.of(role)).build())).getTokenValue();
    }

    @Test void publicCatalogDoesNotRequireCredentials() throws Exception {
        mvc.perform(get("/products")).andExpect(status().isOk());
    }
    @Test void userCannotWriteProducts() throws Exception {
        mvc.perform(post("/products").header("Authorization", "Bearer " + token("ROLE_USER"))).andExpect(status().isForbidden());
    }
    @Test void adminClaimAuthorizesProductWrites() throws Exception {
        mvc.perform(post("/products").header("Authorization", "Bearer " + token("ROLE_ADMIN"))).andExpect(status().isOk());
    }
    @Test void browserWritesRequireCsrf() throws Exception {
        mvc.perform(post("/products").cookie(new Cookie("ms52-session", token("ROLE_ADMIN")))).andExpect(status().isForbidden());
        var csrfResult = mvc.perform(get("/session/csrf")).andExpect(status().isOk()).andReturn();
        var csrfToken = (CsrfToken) csrfResult.getRequest().getAttribute(CsrfToken.class.getName());
        var csrfCookie = csrfResult.getResponse().getHeaders("Set-Cookie").stream()
            .map(org.springframework.mock.web.MockCookie::parse)
            .filter(cookie -> "XSRF-TOKEN".equals(cookie.getName())).findFirst().orElseThrow();
        mvc.perform(post("/products").cookie(new Cookie("ms52-session", token("ROLE_ADMIN")), csrfCookie)
            .header(csrfToken.getHeaderName(), csrfToken.getToken())).andExpect(status().isOk());
    }
    @Test void internalCredentialsNeverReachPublicClients() throws Exception {
        mvc.perform(get("/accounts/internal/credentials").header("Authorization", "Bearer " + token("ROLE_ADMIN"))).andExpect(status().isForbidden());
    }
    @Test void loginRequiresCsrfAndLogoutClearsCookie() throws Exception {
        mvc.perform(post("/session/login").contentType("application/json").content("{\"email\":\"demo@example.test\",\"password\":\"example-password\"}")).andExpect(status().isForbidden());
        mvc.perform(get("/session/csrf")).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("token")));
        mvc.perform(delete("/session")).andExpect(status().isNoContent()).andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.allOf(org.hamcrest.Matchers.containsString("HttpOnly"), org.hamcrest.Matchers.containsString("Max-Age=0"))));
    }
    @Test void realCsrfExchangeCreatesAnHttpOnlySessionWithoutExposingTheToken() throws Exception {
        var csrfResult = mvc.perform(get("/session/csrf")).andExpect(status().isOk()).andReturn();
        var csrfToken = (CsrfToken) csrfResult.getRequest().getAttribute(CsrfToken.class.getName());
        var loginResult = mvc.perform(post("/session/login")
            .cookie(csrfResult.getResponse().getHeaders("Set-Cookie").stream()
                .map(org.springframework.mock.web.MockCookie::parse)
                .filter(cookie -> "XSRF-TOKEN".equals(cookie.getName())).findFirst()
                .orElseThrow(() -> new AssertionError("CSRF cookie was not issued")))
            .header(csrfToken.getHeaderName(), csrfToken.getToken())
            .contentType("application/json").content("{\"email\":\"fixture@example.test\",\"password\":\"fixture-password\"}"))
            .andExpect(status().isNoContent()).andExpect(content().string(""))
            .andExpect(header().string("Set-Cookie", org.hamcrest.Matchers.containsString("HttpOnly"))).andReturn();
        var session = loginResult.getResponse().getHeaders("Set-Cookie").stream()
            .map(org.springframework.mock.web.MockCookie::parse)
            .filter(cookie -> "ms52-session".equals(cookie.getName())).findFirst().orElseThrow();
        org.junit.jupiter.api.Assertions.assertTrue(session.isHttpOnly());
        mvc.perform(get("/session").cookie(session)).andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("fixture@example.test")));
    }
    @RestController static class Probe {
        @GetMapping("/products") String catalog() { return "catalog"; }
        @PostMapping("/products") String create() { return "created"; }
    }
}
