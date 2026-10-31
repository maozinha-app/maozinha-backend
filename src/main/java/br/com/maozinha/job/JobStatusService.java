package br.com.maozinha.job;

import br.com.maozinha.common.exception.BusinessRuleException;
import br.com.maozinha.common.exception.ForbiddenException;
import br.com.maozinha.common.exception.NotFoundException;
import br.com.maozinha.job.dto.JobResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Troca só o status do serviço, seguindo a JobStateMachine.
 * Diferente do PUT /api/services/{id}, não exige reenviar os outros campos
 * nem valida a data do serviço.
 */
@Service
public class JobStatusService {

    private final JobRepository jobRepository;
    private final JobStateMachine jobStateMachine;

    public JobStatusService(
            JobRepository jobRepository,
            JobStateMachine jobStateMachine
    ) {
        this.jobRepository = Objects.requireNonNull(
                jobRepository,
                "jobRepository não pode ser nulo"
        );

        this.jobStateMachine = Objects.requireNonNull(
                jobStateMachine,
                "jobStateMachine não pode ser nulo"
        );
    }

    @Transactional
    public JobResponse updateStatus(
            Long jobId,
            String authenticatedEmail,
            JobStatus targetStatus
    ) {

        Job job = jobRepository
                .findById(jobId)
                .orElseThrow(() -> new NotFoundException(
                        "Serviço não encontrado."
                ));

        if (!job.getRequester()
                .getEmail()
                .equalsIgnoreCase(authenticatedEmail)) {

            throw new ForbiddenException(
                    "O usuário não possui permissão para alterar este serviço."
            );
        }

        // ABERTO e SELECIONADO são definidos pelo sistema (ao publicar e ao
        // selecionar um interessado), não por esta rota.
        if (targetStatus == JobStatus.ABERTO
                || targetStatus == JobStatus.SELECIONADO) {

            throw new BusinessRuleException(
                    "Este status não pode ser definido manualmente."
            );
        }

        jobStateMachine.validateTransition(
                job.getStatus(),
                targetStatus
        );

        job.changeStatus(targetStatus);

        return JobResponse.from(
                jobRepository.save(job)
        );
    }
}