package com.unifacisa.decentralized.security.jwt;

import com.unifacisa.decentralized.web.rest.UserJWTController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * Filters incoming requests and installs a Spring Security principal if a header corresponding to a valid user is
 * found.
 */
public class JWTFilter extends GenericFilterBean {

    private final Logger log = LoggerFactory.getLogger(JWTFilter.class);

    public static final String USERNAME_HEADER = "Username";

    public static final String PASSWORD_HEADER = "Password";

    private TokenProvider tokenProvider;

    public JWTFilter(TokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
        throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        String jwt = resolveToken(httpServletRequest);
        if(jwt == null){
            //pega o login e senha do usuário passados no http header
            String username = httpServletRequest.getHeader(USERNAME_HEADER);
            String password = httpServletRequest.getHeader(PASSWORD_HEADER);

            if(username != null && password != null){
                try{
                    HttpEntity<String> entity = new HttpEntity<>(new HttpHeaders());
                    RestTemplate restTemplate = new RestTemplate();
                    jwt = restTemplate.exchange("http://localhost:8082/api/authenticate?username="+username+"&password="+password,
                        HttpMethod.POST, entity, String.class).getBody();
                    SecurityContextHolder.getContext().setAuthentication(this.tokenProvider.getAuthentication(jwt));
                }catch(Exception ex){
                    log.trace("Authentication exception trace: {}", ex);
                }
            }
        }
        else if (this.tokenProvider.validateToken(jwt)) {
            Authentication authentication = this.tokenProvider.getAuthentication(jwt);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        filterChain.doFilter(servletRequest, servletResponse);
    }

    private String resolveToken(HttpServletRequest request){
        String bearerToken = request.getHeader(JWTConfigurer.AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7, bearerToken.length());
        }
        return null;
    }
}
