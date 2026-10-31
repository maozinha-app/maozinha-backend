package br.com.maozinha.tracking;

public record TrackingItemResponse(

        TrackingType type,

        Long jobId,

        String jobTitle,

        /** Quantidade de interessados (só em INTERESSADOS). */
        Integer quantity,

        /** Outra pessoa envolvida (em AVALIAR e SELECIONADO). */
        Long personId,

        String personName
) {
}