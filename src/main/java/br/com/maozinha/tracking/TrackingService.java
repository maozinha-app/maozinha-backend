package br.com.maozinha.tracking;

import br.com.maozinha.common.exception.NotFoundException;
import br.com.maozinha.interest.Interest;
import br.com.maozinha.interest.InterestRepository;
import br.com.maozinha.interest.InterestStatus;
import br.com.maozinha.job.Job;
import br.com.maozinha.job.JobRepository;
import br.com.maozinha.review.ReviewRepository;
import br.com.maozinha.user.User;
import br.com.maozinha.user.UserRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Monta a lista "Para acompanhar" da tela inicial.
 */
@Service
public class TrackingService {

    private final JobRepository jobRepository;
    private final InterestRepository interestRepository;
    private final ReviewRepository reviewRepository;
    private final UserRepository userRepository;

    public TrackingService(
            JobRepository jobRepository,
            InterestRepository interestRepository,
            ReviewRepository reviewRepository,
            UserRepository userRepository
    ) {
        this.jobRepository = Objects.requireNonNull(
                jobRepository,
                "jobRepository não pode ser nulo"
        );

        this.interestRepository = Objects.requireNonNull(
                interestRepository,
                "interestRepository não pode ser nulo"
        );

        this.reviewRepository = Objects.requireNonNull(
                reviewRepository,
                "reviewRepository não pode ser nulo"
        );

        this.userRepository = Objects.requireNonNull(
                userRepository,
                "userRepository não pode ser nulo"
        );
    }

    @Transactional(readOnly = true)
    public List<TrackingItemResponse> findMyTracking(
            String authenticatedEmail
    ) {

        User me = findActiveUserByEmail(authenticatedEmail);

        List<TrackingItemResponse> items = new ArrayList<>();

        // Serviços que eu publiquei
        List<Job> myJobs = jobRepository
                .findByRequester(me, Pageable.unpaged())
                .getContent();

        for (Job job : myJobs) {

            switch (job.getStatus()) {

                case ABERTO -> {
                    int pending = interestRepository
                            .findByJobAndStatus(
                                    job,
                                    InterestStatus.PENDENTE
                            )
                            .size();

                    // Mesmo sem interessados o serviço aparece, para você
                    // acompanhar o que acabou de publicar.
                    items.add(
                            new TrackingItemResponse(
                                    TrackingType.INTERESSADOS,
                                    job.getId(),
                                    job.getTitle(),
                                    pending,
                                    null,
                                    null
                            )
                    );
                }

                case SELECIONADO, EM_ANDAMENTO -> items.add(
                        new TrackingItemResponse(
                                TrackingType.EM_ANDAMENTO,
                                job.getId(),
                                job.getTitle(),
                                null,
                                null,
                                null
                        )
                );

                case CONCLUIDO -> {
                    List<Interest> selected = interestRepository
                            .findByJobAndStatus(
                                    job,
                                    InterestStatus.SELECIONADO
                            );

                    for (Interest interest : selected) {
                        User worker = interest.getUser();

                        if (!reviewRepository
                                .existsByJobAndReviewerAndReviewed(
                                        job,
                                        me,
                                        worker
                                )) {

                            items.add(
                                    new TrackingItemResponse(
                                            TrackingType.AVALIAR,
                                            job.getId(),
                                            job.getTitle(),
                                            null,
                                            worker.getId(),
                                            worker.getName()
                                    )
                            );
                        }
                    }
                }

                case CANCELADO -> {
                    // Nada a acompanhar.
                }
            }
        }

        // Serviços de outras pessoas em que fui selecionado
        for (Interest interest : interestRepository.findByUser(me)) {

            if (interest.getStatus() != InterestStatus.SELECIONADO) {
                continue;
            }

            Job job = interest.getJob();
            User requester = job.getRequester();

            switch (job.getStatus()) {

                case SELECIONADO, EM_ANDAMENTO -> items.add(
                        new TrackingItemResponse(
                                TrackingType.SELECIONADO,
                                job.getId(),
                                job.getTitle(),
                                null,
                                requester.getId(),
                                requester.getName()
                        )
                );

                case CONCLUIDO -> {
                    if (!reviewRepository
                            .existsByJobAndReviewerAndReviewed(
                                    job,
                                    me,
                                    requester
                            )) {

                        items.add(
                                new TrackingItemResponse(
                                        TrackingType.AVALIAR,
                                        job.getId(),
                                        job.getTitle(),
                                        null,
                                        requester.getId(),
                                        requester.getName()
                                )
                        );
                    }
                }

                default -> {
                    // ABERTO e CANCELADO não entram aqui.
                }
            }
        }

        return items;
    }

    private User findActiveUserByEmail(
            String email
    ) {

        if (email == null || email.isBlank()) {
            throw new NotFoundException(
                    "Usuário não encontrado."
            );
        }

        User user = userRepository
                .findByEmailIgnoreCase(
                        email.trim().toLowerCase(Locale.ROOT)
                )
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
}