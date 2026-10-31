package br.com.maozinha.user;

import br.com.maozinha.common.exception.NotFoundException;
import br.com.maozinha.user.dto.PublicUserProfileResponse;
import br.com.maozinha.user.dto.UpdateProfileRequest;
import br.com.maozinha.user.dto.UserProfileResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Objects;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = Objects.requireNonNull(
                userRepository,
                "userRepository não pode ser nulo"
        );
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getAuthenticatedUserProfile(
            String authenticatedEmail
    ) {
        User user = findActiveUserByEmail(authenticatedEmail);

        return UserProfileResponse.from(user);
    }

    @Transactional
    public UserProfileResponse updateAuthenticatedUserProfile(
            String authenticatedEmail,
            UpdateProfileRequest request
    ) {
        Objects.requireNonNull(
                request,
                "Os dados de atualização do perfil não podem ser nulos."
        );

        User user = findActiveUserByEmail(authenticatedEmail);

        user.updateProfile(
                request.name(),
                request.whatsapp(),
                request.city(),
                request.neighborhood()
        );

        User savedUser = userRepository.save(user);

        return UserProfileResponse.from(savedUser);
    }

    @Transactional(readOnly = true)
    public PublicUserProfileResponse getPublicProfile(
            Long userId
    ) {
        User user = userRepository
                .findById(userId)
                .filter(User::isActive)
                .orElseThrow(() -> new NotFoundException(
                        "Usuário não encontrado."
                ));

        return PublicUserProfileResponse.from(user);
    }

    private User findActiveUserByEmail(String authenticatedEmail) {
        String normalizedEmail = normalizeAuthenticatedEmail(
                authenticatedEmail
        );

        User user = userRepository
                .findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new NotFoundException(
                        "Usuário não encontrado."
                ));

        if (!user.isActive()) {
            throw new NotFoundException(
                    "Usuário não encontrado."
            );
        }

        return user;
    }

    private String normalizeAuthenticatedEmail(
            String authenticatedEmail
    ) {
        if (authenticatedEmail == null
                || authenticatedEmail.isBlank()) {
            throw new NotFoundException(
                    "Usuário não encontrado."
            );
        }

        return authenticatedEmail
                .trim()
                .toLowerCase(Locale.ROOT);
    }
}