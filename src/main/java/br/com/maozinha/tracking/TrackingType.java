package br.com.maozinha.tracking;

public enum TrackingType {

    /** Serviço aberto com interessados esperando sua escolha. */
    INTERESSADOS,

    /** Serviço seu já com gente selecionada ou em andamento. */
    EM_ANDAMENTO,

    /** Serviço concluído em que falta você avaliar alguém. */
    AVALIAR,

    /** Você foi selecionado para fazer o serviço de outra pessoa. */
    SELECIONADO
}