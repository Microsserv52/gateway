package Microsserv52.gateway;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

public class CookieBearerTokenFilter extends OncePerRequestFilter {
    static final String COOKIE_NAME = "ms52-session";

    static String cookieToken(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (var cookie : request.getCookies()) {
            if (COOKIE_NAME.equals(cookie.getName()) && !cookie.getValue().isBlank()) return cookie.getValue();
        }
        return null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        boolean publicRead = "GET".equals(request.getMethod()) &&
            (path.equals("/products") || path.startsWith("/products/"));
        boolean sessionAction = path.equals("/session/login") || path.equals("/session/csrf") ||
            (path.equals("/session") && "DELETE".equals(request.getMethod()));
        String token = cookieToken(request);
        if (request.getHeader("Authorization") != null || token == null || publicRead || sessionAction) {
            chain.doFilter(request, response);
            return;
        }
        chain.doFilter(new HttpServletRequestWrapper(request) {
            @Override public String getHeader(String name) {
                return "Authorization".equalsIgnoreCase(name) ? "Bearer " + token : super.getHeader(name);
            }
            @Override public Enumeration<String> getHeaders(String name) {
                return "Authorization".equalsIgnoreCase(name)
                    ? Collections.enumeration(java.util.List.of("Bearer " + token)) : super.getHeaders(name);
            }
            @Override public Enumeration<String> getHeaderNames() {
                var names = Collections.list(super.getHeaderNames());
                if (names.stream().noneMatch("Authorization"::equalsIgnoreCase)) names.add("Authorization");
                return Collections.enumeration(names);
            }
        }, response);
    }
}
