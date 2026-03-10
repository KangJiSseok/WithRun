package kr.withrun.was.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import kr.withrun.was.global.common.entity.BaseEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "user_auth_accounts",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_user_auth_accounts_provider_identity",
                columnNames = {"provider", "provider_user_id"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserAuthAccount extends BaseEntity {

    @Id
    @Column(name = "user_id")
    private Long id;

    @Column(name = "provider", nullable = false, length = 16)
    private String provider;

    @Column(name = "provider_user_id", nullable = false, length = 191)
    private String providerUserId;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "profile_image_url")
    private String profileImageUrl;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_auth_accounts_user")
    )
    private User user;
}
