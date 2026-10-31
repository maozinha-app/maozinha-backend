package br.com.maozinha.interest;

import br.com.maozinha.common.exception.NotFoundException;
import br.com.maozinha.interest.dto.CreateInterestResponse;
import br.com.maozinha.interest.dto.InterestResponse;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.net.URI;
import java.util.List;
import java.util.Objects;

@RestController
public class InterestController {

    private final InterestService interestService;
    private final SelectionService selectionService;

    public InterestController(
            InterestService interestService,
            SelectionService selectionService
    ) {

        this.interestService = Objects.requireNonNull(
                interestService,
                "interestService não pode ser nulo"
        );

        this.selectionService = Objects.requireNonNull(
                selectionService,
                "selectionService não pode ser nulo"
        );
    }

    @PostMapping("/api/services/{id}/interests")
    public ResponseEntity<CreateInterestResponse> createInterest(
            Authentication authentication,
            @PathVariable("id")
            @Positive
            Long jobId
    ) {

        String authenticatedEmail =
                getAuthenticatedEmail(
                        authentication
                );

        CreateInterestResponse response =
                interestService.createInterest(
                        jobId,
                        authenticatedEmail
                );

        URI location =
                URI.create(
                        "/api/interests/" + response.id()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .location(location)
                .body(response);
    }

    @PatchMapping("/api/interests/{id}")
    public ResponseEntity<Void> withdrawInterest(
            Authentication authentication,
            @PathVariable("id")
            @Positive
            Long interestId
    ) {

        String authenticatedEmail =
                getAuthenticatedEmail(
                        authentication
                );

        interestService.withdrawInterest(
                interestId,
                authenticatedEmail
        );

        return ResponseEntity.noContent()
                .build();
    }

    @PatchMapping("/api/interests/{id}/select")
    public ResponseEntity<Void> selectInterest(
            Authentication authentication,
            @PathVariable("id")
            @Positive
            Long interestId
    ) {

        String authenticatedEmail =
                getAuthenticatedEmail(
                        authentication
                );

        selectionService.selectInterest(
                interestId,
                authenticatedEmail
        );

        return ResponseEntity.noContent()
                .build();
    }

    @PatchMapping("/api/interests/{id}/reject")
    public ResponseEntity<Void> rejectInterest(
            Authentication authentication,
            @PathVariable("id")
            @Positive
            Long interestId
    ) {

        String authenticatedEmail =
                getAuthenticatedEmail(
                        authentication
                );

        selectionService.rejectInterest(
                interestId,
                authenticatedEmail
        );

        return ResponseEntity.noContent()
                .build();
    }

    @GetMapping("/api/services/{id}/interests")
    public ResponseEntity<List<InterestResponse>> findByJob(
            Authentication authentication,
            @PathVariable("id")
            @Positive
            Long jobId
    ) {

        String authenticatedEmail =
                getAuthenticatedEmail(
                        authentication
                );

        return ResponseEntity.ok(
                interestService.findByJob(
                        jobId,
                        authenticatedEmail
                )
        );
    }

    @GetMapping("/api/users/me/interests")
    public ResponseEntity<List<InterestResponse>> findMyInterests(
            Authentication authentication
    ) {

        String authenticatedEmail =
                getAuthenticatedEmail(
                        authentication
                );

        return ResponseEntity.ok(
                interestService.findMyInterests(
                        authenticatedEmail
                )
        );
    }

    @GetMapping("/api/services/{id}/interests/count")
    public ResponseEntity<Map<String, Long>> countInterests(
            @PathVariable("id")
            @Positive
            Long jobId
    ) {

        return ResponseEntity.ok(
                Map.of(
                        "total",
                        interestService.countActiveInterests(
                                jobId
                        )
                )
        );
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