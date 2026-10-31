package br.com.maozinha.job.dto;

import br.com.maozinha.job.JobStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateJobRequest(

        @NotBlank(message = "O título é obrigatório.")
        @Size(
                min = 3,
                max = 120,
                message = "O título deve possuir entre 3 e 120 caracteres."
        )
        String title,

        @NotBlank(message = "A descrição é obrigatória.")
        @Size(
                min = 10,
                max = 2000,
                message = "A descrição deve possuir entre 10 e 2000 caracteres."
        )
        String description,

        @NotNull(message = "O valor é obrigatório.")
        @DecimalMin(
                value = "0.01",
                message = "O valor deve ser maior que zero."
        )
        BigDecimal value,

        @NotNull(message = "A data do serviço é obrigatória.")
        LocalDate serviceDate,

        @NotBlank(message = "A cidade é obrigatória.")
        @Size(
                min = 2,
                max = 80,
                message = "A cidade deve possuir entre 2 e 80 caracteres."
        )
        String city,

        @NotBlank(message = "O bairro é obrigatório.")
        @Size(
                min = 2,
                max = 80,
                message = "O bairro deve possuir entre 2 e 80 caracteres."
        )
        String neighborhood,

        @NotNull(message = "A categoria é obrigatória.")
        Long categoryId,

        @NotNull(message = "O status é obrigatório.")
        JobStatus status

) {
}