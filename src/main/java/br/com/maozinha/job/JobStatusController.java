package br.com.maozinha.job;

import br.com.maozinha.common.exception.NotFoundException;
import br.com.maozinha.job.dto.JobResponse;
import br.com.maozinha.job.dto.UpdateJobStatusRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
public class JobStatusController {

    private final JobStatusService jobStatusService;

    public JobStatusController(
            JobStatusService jobStatusService
    ) {
        this.jobStatusService = Objects.requireNonNull(
                jobStatusService,
                "jobStatusService não pode ser nulo"
        );
    }

    // Body: { "status": "EM_ANDAMENTO" | "CONCLUIDO" | "CANCELADO" }
    @PatchMapping("/api/services/{id}/status")
    public ResponseEntity<JobResponse> updateStatus(
            Authentication authentication,
            @PathVariable("id") @Positive Long jobId,
            @Valid @RequestBody UpdateJobStatusRequest request
    ) {

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null
                || authentication.getName().isBlank()) {

            throw new NotFoundException(
                    "Usuário não encontrado."
            );
        }

        return ResponseEntity.ok(
                jobStatusService.updateStatus(
                        jobId,
                        authentication.getName(),
                        request.status()
                )
        );
    }
}