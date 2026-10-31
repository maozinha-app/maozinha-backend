package br.com.maozinha.interest.dto;

import br.com.maozinha.interest.Interest;
import br.com.maozinha.interest.InterestStatus;
import br.com.maozinha.user.Skill;
import br.com.maozinha.user.User;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public record InterestResponse(

        Long id,

        InterestStatus status,

        Long userId,

        String userName,

        Long jobId,

        String jobTitle,

        OffsetDateTime interestDate,

        // Perfil de quem demonstrou interesse (só para quem publicou o serviço)
        String userCity,

        String userNeighborhood,

        BigDecimal userAverageRating,

        Integer userTotalReviews,

        List<String> userSkills,

        // WhatsApp de quem foi selecionado: só vai para quem publicou o serviço
        String userWhatsapp,

        // Quem publicou o serviço (para quem demonstrou interesse)
        String requesterName,

        // WhatsApp de quem publicou: só vai para quem foi selecionado
        String requesterWhatsapp

) {

    public InterestResponse {

        Objects.requireNonNull(
                id,
                "id não pode ser nulo"
        );

        Objects.requireNonNull(
                status,
                "status não pode ser nulo"
        );

        Objects.requireNonNull(
                userId,
                "userId não pode ser nulo"
        );

        Objects.requireNonNull(
                userName,
                "userName não pode ser nulo"
        );

        Objects.requireNonNull(
                jobId,
                "jobId não pode ser nulo"
        );

        Objects.requireNonNull(
                jobTitle,
                "jobTitle não pode ser nulo"
        );

        Objects.requireNonNull(
                interestDate,
                "interestDate não pode ser nulo"
        );

        userSkills = userSkills == null
                ? List.of()
                : List.copyOf(userSkills);
    }

    /**
     * Versão básica, sem perfil e sem contatos.
     */
    public static InterestResponse from(
            Interest interest
    ) {

        return build(
                interest,
                false,
                false,
                false
        );
    }

    /**
     * Para quem publicou o serviço: traz o perfil de quem demonstrou
     * interesse e o WhatsApp dele quando já foi selecionado.
     */
    public static InterestResponse forRequester(
            Interest interest
    ) {

        return build(
                interest,
                true,
                true,
                false
        );
    }

    /**
     * Para quem demonstrou interesse: traz o nome de quem publicou e o
     * WhatsApp dessa pessoa quando o interesse foi selecionado.
     */
    public static InterestResponse forInterested(
            Interest interest
    ) {

        return build(
                interest,
                false,
                false,
                true
        );
    }

    private static InterestResponse build(
            Interest interest,
            boolean includeProfile,
            boolean includeUserWhatsapp,
            boolean includeRequesterWhatsapp
    ) {

        Objects.requireNonNull(
                interest,
                "interest não pode ser nulo"
        );

        User user = interest.getUser();
        User requester = interest.getJob().getRequester();

        boolean selected =
                interest.getStatus() == InterestStatus.SELECIONADO;

        List<String> skills = includeProfile
                ? user.getSkills()
                .stream()
                .map(Skill::getName)
                .sorted(Comparator.naturalOrder())
                .toList()
                : List.of();

        return new InterestResponse(
                interest.getId(),
                interest.getStatus(),
                user.getId(),
                user.getName(),
                interest.getJob().getId(),
                interest.getJob().getTitle(),
                interest.getInterestDate(),
                includeProfile ? user.getCity() : null,
                includeProfile ? user.getNeighborhood() : null,
                includeProfile ? user.getAverageRating() : null,
                includeProfile ? user.getTotalReviews() : null,
                skills,
                includeUserWhatsapp && selected
                        ? user.getWhatsapp()
                        : null,
                includeRequesterWhatsapp
                        ? requester.getName()
                        : null,
                includeRequesterWhatsapp && selected
                        ? requester.getWhatsapp()
                        : null
        );
    }
}