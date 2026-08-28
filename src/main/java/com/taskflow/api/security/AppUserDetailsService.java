package com.taskflow.api.security;

import com.taskflow.api.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public AppUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) {
        return userRepository
                .findByEmailIgnoreCase(email)
                .map(UserPrincipal::new)
                .orElseThrow(() -> new UsernameNotFoundException("No user with email " + email));
    }
}
