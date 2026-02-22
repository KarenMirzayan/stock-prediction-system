package kz.kbtu.webapi.service;

import kz.kbtu.common.entity.RefreshToken;
import kz.kbtu.common.entity.User;
import kz.kbtu.webapi.dto.UserProfileDto;
import kz.kbtu.webapi.dto.auth.*;
import kz.kbtu.webapi.exception.InvalidTokenException;
import kz.kbtu.webapi.exception.UsernameAlreadyExistsException;
import kz.kbtu.webapi.repository.RefreshTokenRepository;
import kz.kbtu.webapi.repository.UserRepository;
import kz.kbtu.webapi.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new UsernameAlreadyExistsException(request.username());
        }

        var user = User.builder()
            .username(request.username())
            .password(passwordEncoder.encode(request.password()))
            .firstName(request.firstName())
            .lastName(request.lastName())
            .build();

        user = userRepository.save(user);
        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        var user = userRepository.findByUsername(request.username())
            .orElseThrow();

        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshRequest request) {
        var refreshToken = refreshTokenRepository.findByToken(request.refreshToken())
            .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        if (refreshToken.isRevoked()) {
            throw new InvalidTokenException("Refresh token has been revoked");
        }

        if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
            throw new InvalidTokenException("Refresh token has expired");
        }

        // Revoke old token
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        // Issue new tokens
        return buildAuthResponse(refreshToken.getUser());
    }

    private AuthResponse buildAuthResponse(User user) {
        var userDetails = org.springframework.security.core.userdetails.User.builder()
            .username(user.getUsername())
            .password(user.getPassword())
            .authorities("ROLE_" + user.getRole().name())
            .build();

        var accessToken = jwtService.generateToken(userDetails);
        var refreshToken = createRefreshToken(user);

        return new AuthResponse(
            accessToken,
            refreshToken.getToken(),
            jwtService.getAccessExpiration(),
            UserProfileDto.from(user)
        );
    }

    private RefreshToken createRefreshToken(User user) {
        var token = RefreshToken.builder()
            .user(user)
            .token(UUID.randomUUID().toString())
            .expiryDate(Instant.now().plusMillis(refreshExpiration))
            .build();

        return refreshTokenRepository.save(token);
    }
}
