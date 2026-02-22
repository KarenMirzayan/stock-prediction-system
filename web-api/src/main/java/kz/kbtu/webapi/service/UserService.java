package kz.kbtu.webapi.service;

import kz.kbtu.webapi.dto.CompanyListItemDto;
import kz.kbtu.webapi.dto.UpdateProfileRequest;
import kz.kbtu.webapi.dto.UserProfileDto;
import kz.kbtu.webapi.repository.CompanyRepository;
import kz.kbtu.webapi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;

    @Transactional(readOnly = true)
    public UserProfileDto getProfile(String username) {
        var user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        return UserProfileDto.from(user);
    }

    @Transactional
    public UserProfileDto updateProfile(String username, UpdateProfileRequest request) {
        var user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        if (request.firstName() != null) {
            user.setFirstName(request.firstName());
        }
        if (request.lastName() != null) {
            user.setLastName(request.lastName());
        }
        if (request.avatarUrl() != null) {
            user.setAvatarUrl(request.avatarUrl());
        }

        user = userRepository.save(user);
        return UserProfileDto.from(user);
    }

    @Transactional(readOnly = true)
    public List<CompanyListItemDto> getSubscriptions(String username) {
        var user = userRepository.findByUsernameWithSubscriptions(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return user.getSubscribedCompanies().stream()
            .map(c -> CompanyListItemDto.builder()
                .id(c.getId())
                .ticker(c.getTicker())
                .name(c.getName())
                .exchange(c.getExchange())
                .logoUrl(c.getLogoUrl())
                .websiteUrl(c.getWebsiteUrl())
                .marketCap(c.getMarketCap())
                .country(c.getCountry() != null ? c.getCountry().getCode() : null)
                .sectors(c.getSectors().stream().map(s -> s.getName()).toList())
                .build())
            .toList();
    }

    @Transactional
    public void subscribe(String username, Long companyId) {
        var user = userRepository.findByUsernameWithSubscriptions(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
        var company = companyRepository.findById(companyId)
            .orElseThrow(() -> new IllegalArgumentException("Company not found: " + companyId));

        user.getSubscribedCompanies().add(company);
        userRepository.save(user);
    }

    @Transactional
    public void unsubscribe(String username, Long companyId) {
        var user = userRepository.findByUsernameWithSubscriptions(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        user.getSubscribedCompanies().removeIf(c -> c.getId().equals(companyId));
        userRepository.save(user);
    }
}
