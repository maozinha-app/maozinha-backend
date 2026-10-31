package br.com.maozinha.user.dto;

import br.com.maozinha.user.Skill;
import br.com.maozinha.user.User;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Perfil que qualquer usuário logado pode ver.
 * Não tem WhatsApp, e-mail, role nem status.
 */
public record PublicUserProfileResponse(

        Long id,
        String name,
        String city,
        String neighborhood,
        BigDecimal averageRating,
        Integer totalReviews,
        List<String> skills,
        OffsetDateTime createdAt
) {

    public PublicUserProfileResponse {
        skills = skills == null
                ? List.of()
                : List.copyOf(skills);
    }

    public static PublicUserProfileResponse from(User user) {
        List<String> skillNames = user.getSkills()
                .stream()
                .map(Skill::getName)
                .sorted(Comparator.naturalOrder())
                .toList();

        return new PublicUserProfileResponse(
                user.getId(),
                user.getName(),
                user.getCity(),
                user.getNeighborhood(),
                user.getAverageRating(),
                user.getTotalReviews(),
                skillNames,
                user.getCreatedAt()
        );
    }
}