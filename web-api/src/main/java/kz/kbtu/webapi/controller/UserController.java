package kz.kbtu.webapi.controller;

import jakarta.validation.Valid;
import kz.kbtu.webapi.dto.CompanyListItemDto;
import kz.kbtu.webapi.dto.UpdateProfileRequest;
import kz.kbtu.webapi.dto.UserProfileDto;
import kz.kbtu.webapi.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<UserProfileDto> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(userService.getProfile(userDetails.getUsername()));
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfileDto> updateProfile(
        @AuthenticationPrincipal UserDetails userDetails,
        @Valid @RequestBody UpdateProfileRequest request
    ) {
        return ResponseEntity.ok(userService.updateProfile(userDetails.getUsername(), request));
    }

    @GetMapping("/me/subscriptions")
    public ResponseEntity<List<CompanyListItemDto>> getSubscriptions(
        @AuthenticationPrincipal UserDetails userDetails
    ) {
        return ResponseEntity.ok(userService.getSubscriptions(userDetails.getUsername()));
    }

    @PostMapping("/me/subscriptions/{companyId}")
    public ResponseEntity<Void> subscribe(
        @AuthenticationPrincipal UserDetails userDetails,
        @PathVariable Long companyId
    ) {
        userService.subscribe(userDetails.getUsername(), companyId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/me/subscriptions/{companyId}")
    public ResponseEntity<Void> unsubscribe(
        @AuthenticationPrincipal UserDetails userDetails,
        @PathVariable Long companyId
    ) {
        userService.unsubscribe(userDetails.getUsername(), companyId);
        return ResponseEntity.noContent().build();
    }
}
