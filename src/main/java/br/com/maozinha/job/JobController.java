package br.com.maozinha.job;

import br.com.maozinha.common.PageResponse;
import br.com.maozinha.common.exception.NotFoundException;
import br.com.maozinha.job.dto.CreateJobRequest;
import br.com.maozinha.job.dto.JobResponse;
import br.com.maozinha.job.dto.UpdateJobRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.Objects;

@RestController
public class JobController {

    private final JobService jobService;

    public JobController(
            JobService jobService
    ) {
        this.jobService = Objects.requireNonNull(
                jobService,
                "jobService não pode ser nulo"
        );
    }

    @PostMapping("/api/services")
    public ResponseEntity<JobResponse> create(
            Authentication authentication,
            @Valid @RequestBody CreateJobRequest request
    ) {

        String authenticatedEmail =
                getAuthenticatedEmail(
                        authentication
                );

        JobResponse response =
                jobService.create(
                        authenticatedEmail,
                        request
                );

        URI location = URI.create(
                "/api/services/" + response.id()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .location(location)
                .body(response);
    }

    @GetMapping("/api/services/{id}")
    public ResponseEntity<JobResponse> findById(
            @PathVariable("id") Long jobId
    ) {

        return ResponseEntity.ok(
                jobService.findById(
                        jobId
                )
        );
    }

    @GetMapping("/api/services")
    public ResponseEntity<PageResponse<JobResponse>> findAll(

            @RequestParam(
                    defaultValue = "0"
            ) int page,

            @RequestParam(
                    defaultValue = "10"
            ) int size,

            @RequestParam(
                    required = false
            ) Long requesterId

    ) {

        Pageable pageable =
                PageRequest.of(
                        page,
                        size
                );

        Page<JobResponse> jobs =
                requesterId == null
                        ? jobService.findOpenJobs(pageable)
                        : jobService.findOpenJobsByRequester(
                        requesterId,
                        pageable
                );

        return ResponseEntity.ok(
                PageResponse.from(
                        jobs
                )
        );
    }


    @GetMapping("/api/users/me/services")
    public ResponseEntity<PageResponse<JobResponse>> findMyServices(
            Authentication authentication,

            @RequestParam(
                    defaultValue = "0"
            ) int page,

            @RequestParam(
                    defaultValue = "100"
            ) int size
    ) {

        Pageable pageable =
                PageRequest.of(
                        page,
                        size,
                        Sort.by(
                                Sort.Direction.DESC,
                                "createdAt"
                        )
                );

        String authenticatedEmail =
                getAuthenticatedEmail(
                        authentication
                );

        return ResponseEntity.ok(
                PageResponse.from(
                        jobService.findMyJobs(
                                authenticatedEmail,
                                pageable
                        )
                )
        );
    }

    @PutMapping("/api/services/{id}")
    public ResponseEntity<JobResponse> update(
            @PathVariable("id") Long jobId,
            Authentication authentication,
            @Valid @RequestBody UpdateJobRequest request
    ) {

        String authenticatedEmail =
                getAuthenticatedEmail(
                        authentication
                );

        JobResponse response =
                jobService.update(
                        jobId,
                        authenticatedEmail,
                        request
                );

        return ResponseEntity.ok(
                response
        );
    }

    @DeleteMapping("/api/services/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable("id") Long jobId,
            Authentication authentication
    ) {

        String authenticatedEmail =
                getAuthenticatedEmail(
                        authentication
                );

        jobService.delete(
                jobId,
                authenticatedEmail
        );

        return ResponseEntity.noContent()
                .build();
    }

    private String getAuthenticatedEmail(
            Authentication authentication
    ) {

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null
                || authentication.getName().isBlank()) {

            throw new NotFoundException(
                    "Usuário não encontrado."
            );
        }

        return authentication.getName();
    }
}