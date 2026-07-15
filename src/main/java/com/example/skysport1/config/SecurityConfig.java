package com.example.skysport1.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.Order;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;
    private final AuthenticationSuccessHandlerImpl authenticationSuccessHandler;

    // KHÔNG dùng @RequiredArgsConstructor của Lombok ở đây: Lombok không copy
    // annotation @Lazy từ field sang tham số constructor (trừ khi có
    // lombok.config khai báo lombok.copyableAnnotations), nên Spring vẫn coi
    // đây là dependency eager -> vẫn bị circular reference dù đã có @Lazy
    // trên field. Viết constructor tay để @Lazy áp dụng đúng chỗ Spring cần.
    public SecurityConfig(CustomUserDetailsService userDetailsService,
                          @Lazy AuthenticationSuccessHandlerImpl authenticationSuccessHandler) {
        this.userDetailsService = userDetailsService;
        this.authenticationSuccessHandler = authenticationSuccessHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * CHAIN 1: ADMIN ONLY — TẮT CSRF
     */
    @Bean
    @Order(1)
    public SecurityFilterChain adminChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher(new AntPathRequestMatcher("/admin/**"))
                .csrf(csrf -> csrf.disable())  // ✅ TẮT CSRF CHO ADMIN
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/admin/login").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/admin/login")
                        .loginProcessingUrl("/admin/login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .successHandler(authenticationSuccessHandler)
                        .failureUrl("/admin/login?error=true")
                        .permitAll()
                )
                .rememberMe(remember -> remember
                        .key("skySportSecretKey2026")
                        .tokenValiditySeconds(7 * 24 * 60 * 60)
                        .userDetailsService(userDetailsService)
                        .rememberMeParameter("remember-me")
                )
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/admin/logout"))
                        .logoutSuccessUrl("/admin/login?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID", "remember-me")
                        .permitAll()
                )
                .exceptionHandling(ex -> ex
                        .accessDeniedPage("/403")
                );

        return http.build();
    }

    /**
     * CHAIN 2: STAFF + ADMIN — TẮT CSRF
     */
    @Bean
    @Order(2)
    public SecurityFilterChain staffChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher(new AntPathRequestMatcher("/staff/**"))
                .csrf(csrf -> csrf.disable())  // ✅ TẮT CSRF CHO STAFF
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/staff/login").permitAll()
                        .requestMatchers("/staff/**").hasAnyRole("STAFF", "ADMIN")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/staff/login")
                        .loginProcessingUrl("/staff/login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .successHandler(authenticationSuccessHandler)
                        .failureUrl("/staff/login?error=true")
                        .permitAll()
                )
                .rememberMe(remember -> remember
                        .key("skySportSecretKey2026")
                        .tokenValiditySeconds(7 * 24 * 60 * 60)
                        .userDetailsService(userDetailsService)
                        .rememberMeParameter("remember-me")
                )
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/staff/logout"))
                        .logoutSuccessUrl("/staff/login?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID", "remember-me")
                        .permitAll()
                )
                .exceptionHandling(ex -> ex
                        .accessDeniedPage("/403")
                );

        return http.build();
    }

    /**
     * CHAIN 3: CUSTOMER + PUBLIC — GIỮ CSRF
     */
    @Bean
    @Order(3)
    public SecurityFilterChain customerChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/", "/home",
                                "/san-pham", "/san-pham/**",
                                "/login", "/dang-ky",
                                "/403", "/404",
                                "/css/**", "/js/**", "/images/**", "/dist/**", "/plugins/**",
                                "/uploads/**",
                                "/guest/**"
                        ).permitAll()
                        // Giỏ hàng/checkout: cho phép khách vãng lai (chưa đăng nhập) và
                        // Customer, nhưng CHẶN Staff/Admin — trước đây các route này permitAll()
                        // nên tài khoản nội bộ (Staff/Admin) vẫn "mua hàng" được như khách, dễ
                        // gây nhầm lẫn vai trò/dữ liệu (đơn hàng gắn với tài khoản nội bộ). Route
                        // /san-pham/** ở trên vẫn permitAll() (xem sản phẩm không sao), chỉ riêng
                        // hành vi "mua" (giỏ hàng/thanh toán) mới bị chặn với Staff/Admin.
                        .requestMatchers(
                                "/customer/cart", "/customer/cart/**",
                                "/customer/checkout", "/customer/checkout/**"
                        ).access((authentication, context) -> {
                            boolean isStaffOrAdmin = authentication.get().getAuthorities().stream()
                                    .anyMatch(a -> a.getAuthority().equals("ROLE_STAFF")
                                            || a.getAuthority().equals("ROLE_ADMIN"));
                            return new AuthorizationDecision(!isStaffOrAdmin);
                        })
                        .requestMatchers(
                                "/customer/profile/**",
                                "/customer/address/**",
                                "/customer/wishlist/**",
                                "/customer/orders/**",
                                "/customer/reviews/**",
                                "/customer/notifications/**"
                        ).hasRole("CUSTOMER")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .usernameParameter("username")
                        .passwordParameter("password")
                        .successHandler(authenticationSuccessHandler)
                        .failureUrl("/login?error=true")
                        .permitAll()
                )
                .rememberMe(remember -> remember
                        .key("skySportSecretKey2026")
                        .tokenValiditySeconds(7 * 24 * 60 * 60)
                        .userDetailsService(userDetailsService)
                        .rememberMeParameter("remember-me")
                )
                .logout(logout -> logout
                        .logoutRequestMatcher(new AntPathRequestMatcher("/logout"))
                        .logoutSuccessUrl("/login?logout=true")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID", "remember-me")
                        .permitAll()
                )
                .exceptionHandling(ex -> ex
                        .accessDeniedPage("/403")
                )
                .csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())); // ✅ GIỮ CSRF CHO CUSTOMER

        return http.build();
    }
}