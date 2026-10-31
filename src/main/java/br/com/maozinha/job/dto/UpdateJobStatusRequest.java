package br.com.maozinha.job.dto;

import br.com.maozinha.job.JobStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateJobStatusRequest(

        @NotNull(message = "O status é obrigatório.")
        JobStatus status
) {
}