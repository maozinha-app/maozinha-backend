package br.com.maozinha.interest;

import br.com.maozinha.common.config.MaozinhaProperties;
import br.com.maozinha.common.exception.BusinessRuleException;
import br.com.maozinha.common.exception.ConflictException;
import br.com.maozinha.common.exception.NotFoundException;
import br.com.maozinha.job.Job;
import br.com.maozinha.job.JobRepository;
import br.com.maozinha.job.JobStatus;
import br.com.maozinha.user.User;
import br.com.maozinha.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class SelectionService {

    private final InterestRepository interestRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final MaozinhaProperties maozinhaProperties;

    public SelectionService(
            InterestRepository interestRepository,
            JobRepository jobRepository,
            UserRepository userRepository,
            MaozinhaProperties maozinhaProperties
    ) {
        this.interestRepository = Objects.requireNonNull(
                interestRepository,
                "interestRepository não pode ser nulo"
        );

        this.jobRepository = Objects.requireNonNull(
                jobRepository,
                "jobRepository não pode ser nulo"
        );

        this.userRepository = Objects.requireNonNull(
                userRepository,
                "userRepository não pode ser nulo"
        );

        this.maozinhaProperties = Objects.requireNonNull(
                maozinhaProperties,
                "maozinhaProperties não pode ser nulo"
        );
    }

    @Transactional
    public void selectInterest(
            Long interestId,
            String authenticatedEmail
    ) {

        Interest interest =
                findInterestById(
                        interestId
                );

        Job job =
                interest.getJob();

        User requester =
                findActiveUserByEmail(
                        authenticatedEmail
                );

        validateOwnership(
                job,
                requester
        );

        if (job.getStatus() != JobStatus.ABERTO
                && job.getStatus() != JobStatus.SELECIONADO) {

            throw new BusinessRuleException(
                    "Este serviço não aceita mais seleções."
            );
        }

        if (interest.getStatus() != InterestStatus.PENDENTE) {

            throw new BusinessRuleException(
                    "Só é possível selecionar interessados pendentes."
            );
        }

        long selectedCount =
                interestRepository
                        .findByJobAndStatus(
                                job,
                                InterestStatus.SELECIONADO
                        )
                        .size();

        int maxSelected =
                maozinhaProperties
                        .job()
                        .maxSelecionados();

        if (selectedCount >= maxSelected) {

            throw new ConflictException(
                    "Quantidade máxima de interessados selecionados atingida."
            );
        }

        interest.select();

        interestRepository.save(
                interest
        );

        job.changeStatus(
                JobStatus.SELECIONADO
        );

        jobRepository.save(
                job
        );
    }

    @Transactional
    public void rejectInterest(
            Long interestId,
            String authenticatedEmail
    ) {

        Interest interest =
                findInterestById(
                        interestId
                );

        Job job =
                interest.getJob();

        User requester =
                findActiveUserByEmail(
                        authenticatedEmail
                );

        validateOwnership(
                job,
                requester
        );

        interest.reject();

        interestRepository.save(
                interest
        );
    }

    private Interest findInterestById(
            Long interestId
    ) {

        return interestRepository
                .findById(
                        interestId
                )
                .orElseThrow(() ->
                        new NotFoundException(
                                "Interesse não encontrado."
                        )
                );
    }

    private User findActiveUserByEmail(
            String email
    ) {

        String normalizedEmail =
                email
                        .trim()
                        .toLowerCase(
                                Locale.ROOT
                        );

        User user =
                userRepository
                        .findByEmailIgnoreCase(
                                normalizedEmail
                        )
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "Usuário não encontrado."
                                )
                        );

        if (!user.isActive()) {

            throw new NotFoundException(
                    "Usuário não encontrado."
            );
        }

        return user;
    }

    private void validateOwnership(
            Job job,
            User user
    ) {

        if (!job.getRequester()
                .getId()
                .equals(
                        user.getId()
                )) {

            throw new ConflictException(
                    "O usuário não possui permissão para gerenciar os interessados deste serviço."
            );
        }
    }
}