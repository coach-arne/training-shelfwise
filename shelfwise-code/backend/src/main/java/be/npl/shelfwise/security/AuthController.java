package be.npl.shelfwise.security;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService auth;

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

    @PostMapping("/login")
    public AuthService.LoginResult login(@Valid @RequestBody LoginRequest request) {
        return auth.login(request.username().trim(), request.password());
    }

    @GetMapping("/me")
    public AuthService.UserProfile me() {
        return auth.profile(CurrentUser.get());
    }
}
