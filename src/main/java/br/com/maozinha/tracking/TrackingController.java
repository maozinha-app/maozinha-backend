package br.com.maozinha.tracking;

import br.com.maozinha.common.exception.NotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

@RestController
public class TrackingController {

    private final TrackingService trackingService;

    public TrackingController(
            TrackingService trackingService
    ) {
        this.trackingService = Objects.requireNonNull(
                trackingService,
                "trackingService não pode ser nulo"
        );
    }

    @GetMapping("/api/users/me/tracking")
    public ResponseEntity<List<TrackingItemResponse>> findMyTracking(
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

        return ResponseEntity.ok(
                trackingService.findMyTracking(
                        authentication.getName()
                )
        );
    }
}