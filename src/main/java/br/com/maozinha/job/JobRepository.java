package br.com.maozinha.job;

import br.com.maozinha.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobRepository extends JpaRepository<Job, Long> {

    Page<Job> findByStatusAndHiddenByModerationFalse(
            JobStatus status,
            Pageable pageable
    );

    Page<Job> findByRequester(
            User requester,
            Pageable pageable
    );

    Page<Job> findByStatusAndHiddenByModerationFalseAndRequester_Id(
            JobStatus status,
            Long requesterId,
            Pageable pageable
    );
}