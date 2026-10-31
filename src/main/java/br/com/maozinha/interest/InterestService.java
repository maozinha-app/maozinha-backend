package br.com.maozinha.interest;

import br.com.maozinha.common.exception.BusinessRuleException;
import br.com.maozinha.common.exception.ConflictException;
import br.com.maozinha.common.exception.ForbiddenException;
import br.com.maozinha.common.exception.NotFoundException;
import br.com.maozinha.interest.dto.CreateInterestResponse;
import br.com.maozinha.interest.dto.InterestResponse;
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
public class InterestService {

    private final InterestRepository interestRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;

    public InterestService(
            InterestRepository interestRepository,
            JobRepository jobRepository,
            UserRepository userRepository
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
    }

    @Transactional
    public CreateInterestResponse createInterest(
            Long jobId,
            String authenticatedEmail
    ) {

        User user =
                findActiveUserByEmail(
                        authenticatedEmail
                );

        Job job =
                findJobById(
                        jobId
                );

        if (job.getStatus() != JobStatus.ABERTO) {

            throw new ConflictException(
                    "O serviço não está disponível para receber interessados."
            );
        }

        if (job.getRequester()
                .getId()
                .equals(user.getId())) {

            throw new ConflictException(
                    "O usuário não pode demonstrar interesse no próprio serviço."
            );
        }

        Interest existente =
                interestRepository
                        .findByUserAndJob(
                                user,
                                job
                        )
                        .orElse(null);

        Interest interest;

        if (existente == null) {

            interest =
                    Interest.create(
                            user,
                            job
                    );

        } else if (existente.getStatus() == InterestStatus.RETIRADO) {

            // Voltou atrás: reaproveita o registro (UNIQUE usuário+serviço).
            existente.reopen();
            interest = existente;

        } else {

            throw new ConflictException(
                    "O usuário já demonstrou interesse neste serviço."
            );
        }

        Interest savedInterest =
                interestRepository.saveAndFlush(
                        interest
                );

        return CreateInterestResponse.from(
                savedInterest
        );
    }

    @Transactional
    public void withdrawInterest(
            Long interestId,
            String authenticatedEmail
    ) {

        Interest interest =
                findInterestById(
                        interestId
                );

        User user =
                findActiveUserByEmail(
                        authenticatedEmail
                );

        if (!interest.getUser()
                .getId()
                .equals(user.getId())) {

            throw new ForbiddenException(
                    "O usuário não possui permissão para alterar este interesse."
            );
        }

        // Já retirado: nada a fazer (idempotente).
        if (interest.getStatus() == InterestStatus.RETIRADO) {
            return;
        }

        if (interest.getStatus() == InterestStatus.NAO_SELECIONADO) {

            throw new BusinessRuleException(
                    "Este interesse já foi recusado e não pode ser retirado."
            );
        }

        Job job =
                interest.getJob();

        if (interest.getStatus() == InterestStatus.SELECIONADO) {

            // Depois que o serviço começa, só o dono pode cancelar.
            if (job.getStatus() != JobStatus.SELECIONADO) {

                throw new BusinessRuleException(
                        "Você já foi selecionado e o serviço já começou. Fale com quem publicou para cancelar."
                );
            }
        }

        boolean eraSelecionado =
                interest.getStatus() == InterestStatus.SELECIONADO;

        interest.withdraw();

        interestRepository.save(
                interest
        );

        // Se era o único selecionado, o serviço volta a receber interessados.
        if (eraSelecionado
                && interestRepository
                .findByJobAndStatus(
                        job,
                        InterestStatus.SELECIONADO
                )
                .isEmpty()) {

            job.changeStatus(
                    JobStatus.ABERTO
            );

            jobRepository.save(
                    job
            );
        }
    }


    @Transactional(readOnly = true)
    public List<InterestResponse> findMyInterests(
            String authenticatedEmail
    ) {

        User user =
                findActiveUserByEmail(
                        authenticatedEmail
                );

        return interestRepository
                .findByUser(user)
                .stream()
                .map(InterestResponse::forInterested)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InterestResponse> findByJob(
            Long jobId,
            String authenticatedEmail
    ) {

        Job job =
                findJobById(
                        jobId
                );

        User requester =
                findActiveUserByEmail(
                        authenticatedEmail
                );

        // Só quem publicou o serviço vê a lista de interessados.
        if (!job.getRequester()
                .getId()
                .equals(
                        requester.getId()
                )) {

            throw new ForbiddenException(
                    "O usuário não possui permissão para ver os interessados deste serviço."
            );
        }

        return interestRepository
                .findByJob(job)
                .stream()
                .map(InterestResponse::forRequester)
                .toList();
    }

    @Transactional(readOnly = true)
    public long countActiveInterests(
            Long jobId
    ) {

        Job job =
                findJobById(
                        jobId
                );

        return interestRepository
                .findByJob(job)
                .stream()
                .filter(interest ->
                        interest.getStatus() == InterestStatus.PENDENTE
                                || interest.getStatus() == InterestStatus.SELECIONADO
                )
                .count();
    }

    private User findActiveUserByEmail(
            String email
    ) {

        String normalizedEmail =
                email.trim()
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

    private Job findJobById(
            Long jobId
    ) {

        return jobRepository
                .findById(jobId)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Serviço não encontrado."
                        )
                );
    }

    private Interest findInterestById(
            Long interestId
    ) {

        return interestRepository
                .findById(interestId)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Interesse não encontrado."
                        )
                );
    }
}