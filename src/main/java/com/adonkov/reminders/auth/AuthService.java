package com.adonkov.reminders.auth;

import com.adonkov.reminders.user.User;
import com.adonkov.reminders.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthDtos.TokenResponse register(AuthDtos.RegisterRequest request) {
        if (users.existsByEmailIgnoreCase(request.email())) {
            throw AuthException.emailTaken();
        }

        User user = new User(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.displayName());

        users.save(user);
        return issueFor(user);
    }

    @Transactional(readOnly = true)
    public AuthDtos.TokenResponse login(AuthDtos.LoginRequest request) {
        User user = users.findByEmailIgnoreCase(request.email())
                .orElseThrow(AuthException::badCredentials);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw AuthException.badCredentials();
        }
        return issueFor(user);
    }

    private AuthDtos.TokenResponse issueFor(User user) {
        String token = jwtService.issue(user.getId(), user.getEmail());
        return AuthDtos.TokenResponse.bearer(token, jwtService.expiresInSeconds());
    }
}
