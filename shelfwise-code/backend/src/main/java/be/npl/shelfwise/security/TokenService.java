package be.npl.shelfwise.security;

import be.npl.shelfwise.config.AppProperties;
import java.time.Duration;
import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtEncoder encoder;
    private final AppProperties properties;

    public String issue(String subject, Long accountId, Role role, Long branchId) {
        Instant now = Instant.now();
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .subject(subject)
                .issuedAt(now)
                .expiresAt(now.plus(Duration.ofHours(properties.jwtExpiryHours())))
                .claim(CurrentUser.CLAIM_UID, accountId)
                .claim(CurrentUser.CLAIM_ROLE, role.name());
        if (branchId != null) {
            claims.claim(CurrentUser.CLAIM_BRANCH, branchId);
        }
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
    }
}
