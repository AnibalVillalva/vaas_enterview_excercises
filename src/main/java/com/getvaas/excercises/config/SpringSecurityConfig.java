package com.getvaas.excercises.config;

import javax.servlet.Filter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

import ch.qos.logback.classic.helpers.MDCInsertingServletFilter;
import lombok.AllArgsConstructor;

/**
 * Clase que envuelve la configuración de seguridad de la aplicación
 */
@Configuration
@AllArgsConstructor
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class SpringSecurityConfig extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        //TODO: analyze if csrf should be activated
        http.authorizeRequests().antMatchers("**").permitAll()
                .and().csrf().disable();
    }

    @Override
    @Bean
    public AuthenticationManager authenticationManagerBean()
            throws Exception {
        return authenticationManager();
    }

    @Bean
    public Filter MDCInsertingServletFilter() {
        return new MDCInsertingServletFilter();
    }

}
