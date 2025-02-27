package by.sakuuj.articles.article.configs;

import by.sakuuj.articles.security.AuthenticatedUserAuthenticationFilter;
import by.sakuuj.articles.service.PersonService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.RequestCacheConfigurer;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.web.util.matcher.RegexRequestMatcher.regexMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public AuthenticatedUserAuthenticationFilter authenticatedUserAuthenticationFilter(PersonService personService) {
        return new AuthenticatedUserAuthenticationFilter(personService);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity httpSecurity,
            AuthenticatedUserAuthenticationFilter authenticatedUserAuthenticationFilter
    ) throws Exception {

        httpSecurity
                .csrf(AbstractHttpConfigurer::disable)
                .requestCache(RequestCacheConfigurer::disable)
                .sessionManagement(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                regexMatcher("/swagger-ui.*"),
                                regexMatcher("/v3/api-docs.*")
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/topics", "/topics/*").permitAll()
                        .requestMatchers(HttpMethod.GET, "/articles", "/articles/*").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(jwt ->
                                jwt.jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                        )
                )
                .addFilterAfter(
                        authenticatedUserAuthenticationFilter,
                        BearerTokenAuthenticationFilter.class
                );

        return httpSecurity.build();
    }
}
