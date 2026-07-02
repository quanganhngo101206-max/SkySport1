package com.example.skysport1.config;

import com.example.skysport1.entity.Account;
import com.example.skysport1.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Load Account từ DB để Spring Security dùng khi xác thực.
 * Authority = "ROLE_" + tên role (ADMIN, STAFF, CUSTOMER).
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final AccountRepository accountRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Account account = accountRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản: " + username));

        String roleName = account.getRole() != null ? account.getRole().getName() : "CUSTOMER";

        return new org.springframework.security.core.userdetails.User(
                account.getUsername(),
                account.getPassword(),
                account.getStatus() == 1,       // enabled
                true,                            // accountNonExpired
                true,                            // credentialsNonExpired
                account.getIsNonLocked(),        // accountNonLocked
                List.of(new SimpleGrantedAuthority("ROLE_" + roleName))
        );
    }
}