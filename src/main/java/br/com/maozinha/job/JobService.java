package br.com.maozinha.job;

import br.com.maozinha.category.Category;
import br.com.maozinha.category.CategoryRepository;
import br.com.maozinha.common.exception.ForbiddenException;
import br.com.maozinha.common.exception.NotFoundException;
import br.com.maozinha.job.dto.CreateJobRequest;
import br.com.maozinha.job.dto.JobResponse;
import br.com.maozinha.job.dto.UpdateJobRequest;
import br.com.maozinha.user.User;
import br.com.maozinha.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Objects;

@Service
public class JobService {

    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    public JobService(
            JobRepository jobRepository,
            UserRepository userRepository,
            CategoryRepository categoryRepository
    ) {

        this.jobRepository = Objects.requireNonNull(
                jobRepository,
                "jobRepository não pode ser nulo"
        );

        this.userRepository = Objects.requireNonNull(
                userRepository,
                "userRepository não pode ser nulo"
        );

        this.categoryRepository = Objects.requireNonNull(
                categoryRepository,
                "categoryRepository não pode ser nulo"
        );
    }

    @Transactional
    public JobResponse create(
            String authenticatedEmail,
            CreateJobRequest request
    ) {

        User requester =
                findActiveUserByEmail(
                        authenticatedEmail
                );

        Category category =
                findCategoryById(
                        request.categoryId()
                );

        Job job = Job.create(
                request.title(),
                request.description(),
                request.value(),
                request.serviceDate(),
                request.city(),
                request.neighborhood(),
                requester,
                category
        );

        Job savedJob =
                jobRepository.saveAndFlush(
                        job
                );

        return JobResponse.from(
                savedJob
        );
    }

    @Transactional(readOnly = true)
    public JobResponse findById(
            Long jobId
    ) {

        Job job =
                findJobById(
                        jobId
                );

        return JobResponse.from(
                job
        );
    }

    @Transactional(readOnly = true)
    public Page<JobResponse> findOpenJobs(
            Pageable pageable
    ) {

        return jobRepository
                .findByStatusAndHiddenByModerationFalse(
                        JobStatus.ABERTO,
                        pageable
                )
                .map(JobResponse::from);
    }

    @Transactional
    public JobResponse update(
            Long jobId,
            String authenticatedEmail,
            UpdateJobRequest request
    ) {

        Job job =
                findJobById(
                        jobId
                );

        validateOwnership(
                job,
                authenticatedEmail
        );

        Category category =
                findCategoryById(
                        request.categoryId()
                );

        job.update(
                request.title(),
                request.description(),
                request.value(),
                request.serviceDate(),
                request.city(),
                request.neighborhood(),
                category,
                request.status()
        );

        Job savedJob =
                jobRepository.saveAndFlush(
                        job
                );

        return JobResponse.from(
                savedJob
        );
    }

    @Transactional
    public void delete(
            Long jobId,
            String authenticatedEmail
    ) {

        Job job =
                findJobById(
                        jobId
                );

        validateOwnership(
                job,
                authenticatedEmail
        );

        job.changeStatus(
                JobStatus.CANCELADO
        );

        jobRepository.save(
                job
        );
    }

    @Transactional(readOnly = true)
    public Page<JobResponse> findMyJobs(
            String authenticatedEmail,
            Pageable pageable
    ) {

        User requester =
                findActiveUserByEmail(
                        authenticatedEmail
                );

        return jobRepository
                .findByRequester(
                        requester,
                        pageable
                )
                .map(JobResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<JobResponse> findOpenJobsByRequester(
            Long requesterId,
            Pageable pageable
    ) {

        return jobRepository
                .findByStatusAndHiddenByModerationFalseAndRequester_Id(
                        JobStatus.ABERTO,
                        requesterId,
                        pageable
                )
                .map(JobResponse::from);
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

    private Category findCategoryById(
            Long categoryId
    ) {

        return categoryRepository
                .findById(categoryId)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Categoria não encontrada."
                        )
                );
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

    private void validateOwnership(
            Job job,
            String authenticatedEmail
    ) {

        if (!job.getRequester()
                .getEmail()
                .equalsIgnoreCase(
                        authenticatedEmail
                )) {

            throw new ForbiddenException(
                    "O usuário não possui permissão para alterar este serviço."
            );
        }
    }
}