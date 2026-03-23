package kr.withrun.was.domain.auth.security;

// SecurityContext에 저장할 최소 인증 주체입니다.
// Step 4에서는 "현재 요청 사용자의 userId"만 있으면 충분하므로 별도 UserDetails 대신 record로 단순화합니다.
public record AuthenticatedUser(Long userId) {
}
