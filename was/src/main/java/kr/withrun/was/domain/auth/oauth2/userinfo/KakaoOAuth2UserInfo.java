package kr.withrun.was.domain.auth.oauth2.userinfo;

import java.util.Map;

public class KakaoOAuth2UserInfo implements OAuth2UserInfo {

    private final Map<String, Object> attributes;

    public KakaoOAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String getProvider() {
        return "kakao";
    }

    @Override
    public String getProviderUserId() {
        Object id = attributes.get("id");
        return id != null ? id.toString() : null;
    }

    @Override
    public String getEmail() {
        Object kakaoAccountObject = attributes.get("kakao_account");
        if (!(kakaoAccountObject instanceof Map<?, ?> kakaoAccount)) {
            return null;
        }

        Object email = kakaoAccount.get("email");
        return email != null ? email.toString() : null;
    }

    @Override
    public String getProfileImageUrl() {
        Object kakaoAccountObject = attributes.get("kakao_account");
        if (!(kakaoAccountObject instanceof Map<?, ?> kakaoAccount)) {
            return null;
        }

        Object profileObject = kakaoAccount.get("profile");
        if (!(profileObject instanceof Map<?, ?> profile)) {
            return null;
        }

        Object profileImageUrl = profile.get("profile_image_url");
        return profileImageUrl != null ? profileImageUrl.toString() : null;
    }
}
