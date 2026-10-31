package br.com.maozinha.interest;

import br.com.maozinha.job.Job;
import br.com.maozinha.user.User;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

@Entity
@Table(
        name = "interesse",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "ux_interesse_usuario_servico",
                        columnNames = {
                                "usuario_id",
                                "servico_id"
                        }
                )
        }
)
public class Interest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "data_interesse",
            nullable = false
    )
    private OffsetDateTime interestDate;

    @Column(
            name = "atualizado_em",
            nullable = false
    )
    private OffsetDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "status",
            nullable = false,
            length = 20
    )
    private InterestStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "usuario_id",
            nullable = false
    )
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "servico_id",
            nullable = false
    )
    private Job job;

    @Version
    @Column(
            name = "versao",
            nullable = false
    )
    private Long version;

    protected Interest() {
    }

    private Interest(
            User user,
            Job job
    ) {

        this.user = Objects.requireNonNull(
                user,
                "Usuário não pode ser nulo."
        );

        this.job = Objects.requireNonNull(
                job,
                "Serviço não pode ser nulo."
        );

        this.status = InterestStatus.PENDENTE;
        this.version = 0L;
    }

    public static Interest create(
            User user,
            Job job
    ) {

        return new Interest(
                user,
                job
        );
    }

    public void select() {

        status =
                InterestStatus.SELECIONADO;
    }

    public void reject() {

        status =
                InterestStatus.NAO_SELECIONADO;
    }

    public void withdraw() {

        status =
                InterestStatus.RETIRADO;
    }
    public void reopen() {

        status =
                InterestStatus.PENDENTE;
    }


    @PrePersist
    private void prePersist() {

        OffsetDateTime now =
                OffsetDateTime.now(
                        ZoneOffset.UTC
                );

        interestDate = now;
        updatedAt = now;
    }

    @PreUpdate
    private void preUpdate() {

        updatedAt =
                OffsetDateTime.now(
                        ZoneOffset.UTC
                );
    }

    public Long getId() {
        return id;
    }

    public OffsetDateTime getInterestDate() {
        return interestDate;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public InterestStatus getStatus() {
        return status;
    }

    public User getUser() {
        return user;
    }

    public Job getJob() {
        return job;
    }

    public Long getVersion() {
        return version;
    }

    @Override
    public boolean equals(
            Object object
    ) {

        if (this == object) {
            return true;
        }

        if (!(object instanceof Interest other)) {
            return false;
        }

        return id != null
                && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}