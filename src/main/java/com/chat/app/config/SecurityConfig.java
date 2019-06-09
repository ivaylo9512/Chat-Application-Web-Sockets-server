package com.chat.app.config;
import com.chat.app.security.*;
import com.chat.app.services.UserServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.session.ConcurrentSessionFilter;

import java.util.Collections;

@EnableWebSecurity
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)
@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private UserServiceImpl userService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private AuthorizationProvider authorizationProvider;


    @Override
    public void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(userService).passwordEncoder(new BCryptPasswordEncoder());
    }
    @Bean
    public AuthenticationManager authenticationManagerAuthorization() {
        return new ProviderManager(Collections.singletonList(authorizationProvider));
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.cors()
                .and()
                .csrf()
                .disable().authorizeRequests()
                .antMatchers("**/api/auth/**").authenticated()
                .and()
                .addFilterBefore(authenticationFilter(), ConcurrentSessionFilter.class)
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);

        http.addFilterBefore(authorizationFilter(), UsernamePasswordAuthenticationFilter.class);
        http.headers().cacheControl();
    }

    private AuthorizationFilter authorizationFilter() {
        AuthorizationFilter filter = new AuthorizationFilter();
        filter.setAuthenticationManager(authenticationManagerAuthorization());
        filter.setAuthenticationFailureHandler(new FailureHandler());
        filter.setAuthenticationSuccessHandler((request, response, authentication) -> {});
        return filter;
    }

    private AuthenticationFilter authenticationFilter() throws Exception{
        final AuthenticationFilter authenticationFilter = new AuthenticationFilter();
        authenticationFilter.setFilterProcessesUrl("/login");
        authenticationFilter.setAuthenticationFailureHandler(new FailureHandler());
        authenticationFilter.setAuthenticationManager(authenticationManager());
        return authenticationFilter;
    }

}