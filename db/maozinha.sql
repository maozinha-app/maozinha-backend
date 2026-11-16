BEGIN;

DROP SCHEMA IF EXISTS public CASCADE;

CREATE SCHEMA public AUTHORIZATION maozinha_migrator;

REVOKE ALL ON SCHEMA public FROM PUBLIC;
GRANT USAGE ON SCHEMA public TO maozinha_app;

ALTER DEFAULT PRIVILEGES
FOR ROLE maozinha_migrator
IN SCHEMA public
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO maozinha_app;

ALTER DEFAULT PRIVILEGES
FOR ROLE maozinha_migrator
IN SCHEMA public
GRANT USAGE, SELECT ON SEQUENCES TO maozinha_app;

CREATE TABLE public.usuario (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY,
    nome                VARCHAR(120) NOT NULL,
    email               VARCHAR(254) NOT NULL,
    senha_hash          VARCHAR(255) NOT NULL,
    whatsapp            VARCHAR(20) NOT NULL,
    cidade              VARCHAR(80) NOT NULL,
    bairro              VARCHAR(80) NOT NULL,
    papel               VARCHAR(10) NOT NULL DEFAULT 'USUARIO',
    situacao            VARCHAR(10) NOT NULL DEFAULT 'ATIVO',
    reputacao_media     NUMERIC(3,2) NOT NULL DEFAULT 0.00,
    total_avaliacoes    INTEGER NOT NULL DEFAULT 0,
    criado_em           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    atualizado_em       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    versao              BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT pk_usuario
        PRIMARY KEY (id),

    CONSTRAINT ck_usuario_nome
        CHECK (LENGTH(BTRIM(nome)) BETWEEN 2 AND 120),

    CONSTRAINT ck_usuario_email
        CHECK (
            LENGTH(BTRIM(email)) BETWEEN 5 AND 254
            AND POSITION('@' IN email) > 1
        ),

    CONSTRAINT ck_usuario_senha_hash
        CHECK (LENGTH(BTRIM(senha_hash)) >= 20),

    CONSTRAINT ck_usuario_whatsapp
        CHECK (LENGTH(BTRIM(whatsapp)) BETWEEN 8 AND 20),

    CONSTRAINT ck_usuario_cidade
        CHECK (LENGTH(BTRIM(cidade)) BETWEEN 2 AND 80),

    CONSTRAINT ck_usuario_bairro
        CHECK (LENGTH(BTRIM(bairro)) BETWEEN 2 AND 80),

    CONSTRAINT ck_usuario_papel
        CHECK (papel IN ('USUARIO', 'ADMIN')),

    CONSTRAINT ck_usuario_situacao
        CHECK (situacao IN ('ATIVO', 'SUSPENSO', 'EXCLUIDO')),

    CONSTRAINT ck_usuario_reputacao
        CHECK (reputacao_media BETWEEN 0.00 AND 5.00),

    CONSTRAINT ck_usuario_total_avaliacoes
        CHECK (total_avaliacoes >= 0),

    CONSTRAINT ck_usuario_versao
        CHECK (versao >= 0)
);

CREATE UNIQUE INDEX ux_usuario_email
    ON public.usuario (LOWER(BTRIM(email)));

CREATE INDEX ix_usuario_situacao
    ON public.usuario (situacao);

CREATE INDEX ix_usuario_papel
    ON public.usuario (papel);

CREATE INDEX ix_usuario_localizacao
    ON public.usuario (cidade, bairro);

CREATE TABLE public.habilidade (
    id      BIGINT GENERATED ALWAYS AS IDENTITY,
    nome    VARCHAR(60) NOT NULL,

    CONSTRAINT pk_habilidade
        PRIMARY KEY (id),

    CONSTRAINT ck_habilidade_nome
        CHECK (LENGTH(BTRIM(nome)) BETWEEN 2 AND 60)
);

CREATE UNIQUE INDEX ux_habilidade_nome
    ON public.habilidade (LOWER(BTRIM(nome)));

CREATE TABLE public.usuario_habilidade (
    usuario_id       BIGINT NOT NULL,
    habilidade_id    BIGINT NOT NULL,

    CONSTRAINT pk_usuario_habilidade
        PRIMARY KEY (usuario_id, habilidade_id),

    CONSTRAINT fk_usuario_habilidade_usuario
        FOREIGN KEY (usuario_id)
        REFERENCES public.usuario (id)
        ON DELETE CASCADE,

    CONSTRAINT fk_usuario_habilidade_habilidade
        FOREIGN KEY (habilidade_id)
        REFERENCES public.habilidade (id)
        ON DELETE CASCADE
);

CREATE INDEX ix_usuario_habilidade_habilidade
    ON public.usuario_habilidade (habilidade_id, usuario_id);

CREATE TABLE public.categoria (
    id      BIGINT GENERATED ALWAYS AS IDENTITY,
    nome    VARCHAR(60) NOT NULL,

    CONSTRAINT pk_categoria
        PRIMARY KEY (id),

    CONSTRAINT ck_categoria_nome
        CHECK (LENGTH(BTRIM(nome)) BETWEEN 2 AND 60)
);

CREATE UNIQUE INDEX ux_categoria_nome
    ON public.categoria (LOWER(BTRIM(nome)));

CREATE TABLE public.servico (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY,
    titulo                  VARCHAR(120) NOT NULL,
    descricao               VARCHAR(2000) NOT NULL,
    valor                   NUMERIC(10,2) NOT NULL,
    data_servico            DATE NOT NULL,
    cidade                  VARCHAR(80) NOT NULL,
    bairro                  VARCHAR(80) NOT NULL,
    status                  VARCHAR(15) NOT NULL DEFAULT 'ABERTO',
    solicitante_id          BIGINT NOT NULL,
    categoria_id            BIGINT NOT NULL,
    oculto_por_moderacao    BOOLEAN NOT NULL DEFAULT FALSE,
    criado_em               TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    atualizado_em           TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    versao                  BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT pk_servico
        PRIMARY KEY (id),

    CONSTRAINT fk_servico_solicitante
        FOREIGN KEY (solicitante_id)
        REFERENCES public.usuario (id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_servico_categoria
        FOREIGN KEY (categoria_id)
        REFERENCES public.categoria (id)
        ON DELETE RESTRICT,

    CONSTRAINT ck_servico_titulo
        CHECK (LENGTH(BTRIM(titulo)) BETWEEN 3 AND 120),

    CONSTRAINT ck_servico_descricao
        CHECK (LENGTH(BTRIM(descricao)) BETWEEN 10 AND 2000),

    CONSTRAINT ck_servico_valor
        CHECK (valor > 0.00),

    CONSTRAINT ck_servico_cidade
        CHECK (LENGTH(BTRIM(cidade)) BETWEEN 2 AND 80),

    CONSTRAINT ck_servico_bairro
        CHECK (LENGTH(BTRIM(bairro)) BETWEEN 2 AND 80),

    CONSTRAINT ck_servico_status
        CHECK (
            status IN (
                'ABERTO',
                'SELECIONADO',
                'EM_ANDAMENTO',
                'CONCLUIDO',
                'CANCELADO'
            )
        ),

    CONSTRAINT ck_servico_versao
        CHECK (versao >= 0)
);

CREATE INDEX ix_servico_vitrine
    ON public.servico (data_servico, criado_em DESC)
    WHERE status = 'ABERTO'
      AND oculto_por_moderacao = FALSE;

CREATE INDEX ix_servico_status_data
    ON public.servico (status, data_servico);

CREATE INDEX ix_servico_categoria
    ON public.servico (categoria_id, status, data_servico);

CREATE INDEX ix_servico_localizacao
    ON public.servico (cidade, bairro, status);

CREATE INDEX ix_servico_solicitante
    ON public.servico (solicitante_id, status);

CREATE INDEX ix_servico_criado_em
    ON public.servico (criado_em DESC);

CREATE INDEX ix_servico_busca
    ON public.servico
    USING GIN (
        TO_TSVECTOR(
            'portuguese',
            COALESCE(titulo, '') || ' ' || COALESCE(descricao, '')
        )
    );

CREATE TABLE public.interesse (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY,
    data_interesse      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    atualizado_em       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    usuario_id          BIGINT NOT NULL,
    servico_id          BIGINT NOT NULL,
    versao              BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT pk_interesse
        PRIMARY KEY (id),

    CONSTRAINT fk_interesse_usuario
        FOREIGN KEY (usuario_id)
        REFERENCES public.usuario (id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_interesse_servico
        FOREIGN KEY (servico_id)
        REFERENCES public.servico (id)
        ON DELETE CASCADE,

    CONSTRAINT ck_interesse_status
        CHECK (
            status IN (
                'PENDENTE',
                'SELECIONADO',
                'NAO_SELECIONADO',
                'RETIRADO'
            )
        ),

    CONSTRAINT ck_interesse_versao
        CHECK (versao >= 0),

    CONSTRAINT ux_interesse_usuario_servico
        UNIQUE (usuario_id, servico_id)
);

CREATE INDEX ix_interesse_servico_status
    ON public.interesse (servico_id, status);

CREATE INDEX ix_interesse_usuario_status
    ON public.interesse (usuario_id, status);

CREATE INDEX ix_interesse_data
    ON public.interesse (data_interesse DESC);

CREATE TABLE public.avaliacao (
    id                          BIGINT GENERATED ALWAYS AS IDENTITY,
    nota                        SMALLINT NOT NULL,
    comentario                  VARCHAR(500),
    data_avaliacao              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    avaliador_id                BIGINT NOT NULL,
    avaliado_id                 BIGINT NOT NULL,
    servico_id                  BIGINT NOT NULL,
    removida_por_moderacao      BOOLEAN NOT NULL DEFAULT FALSE,
    removida_em                 TIMESTAMPTZ,

    CONSTRAINT pk_avaliacao
        PRIMARY KEY (id),

    CONSTRAINT fk_avaliacao_avaliador
        FOREIGN KEY (avaliador_id)
        REFERENCES public.usuario (id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_avaliacao_avaliado
        FOREIGN KEY (avaliado_id)
        REFERENCES public.usuario (id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_avaliacao_servico
        FOREIGN KEY (servico_id)
        REFERENCES public.servico (id)
        ON DELETE CASCADE,

    CONSTRAINT ck_avaliacao_nota
        CHECK (nota BETWEEN 1 AND 5),

    CONSTRAINT ck_avaliacao_comentario
        CHECK (
            comentario IS NULL
            OR LENGTH(BTRIM(comentario)) BETWEEN 1 AND 500
        ),

    CONSTRAINT ck_avaliacao_usuarios_distintos
        CHECK (avaliador_id <> avaliado_id),

    CONSTRAINT ck_avaliacao_remocao
        CHECK (
            (removida_por_moderacao = FALSE AND removida_em IS NULL)
            OR
            (removida_por_moderacao = TRUE AND removida_em IS NOT NULL)
        ),

    CONSTRAINT ux_avaliacao_servico_par
        UNIQUE (servico_id, avaliador_id, avaliado_id)
);

CREATE INDEX ix_avaliacao_avaliado
    ON public.avaliacao (avaliado_id, data_avaliacao DESC)
    WHERE removida_por_moderacao = FALSE;

CREATE INDEX ix_avaliacao_avaliador
    ON public.avaliacao (avaliador_id, data_avaliacao DESC);

CREATE INDEX ix_avaliacao_servico
    ON public.avaliacao (servico_id);

CREATE INDEX ix_avaliacao_moderacao
    ON public.avaliacao (removida_por_moderacao);

CREATE TABLE public.acao_moderacao (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY,
    administrador_id    BIGINT NOT NULL,
    tipo_alvo           VARCHAR(10) NOT NULL,
    alvo_id             BIGINT NOT NULL,
    acao                VARCHAR(30) NOT NULL,
    motivo              VARCHAR(500) NOT NULL,
    data_acao           TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_acao_moderacao
        PRIMARY KEY (id),

    CONSTRAINT fk_acao_moderacao_administrador
        FOREIGN KEY (administrador_id)
        REFERENCES public.usuario (id)
        ON DELETE RESTRICT,

    CONSTRAINT ck_acao_moderacao_tipo_alvo
        CHECK (
            tipo_alvo IN (
                'USUARIO',
                'SERVICO',
                'AVALIACAO'
            )
        ),

    CONSTRAINT ck_acao_moderacao_acao
        CHECK (
            acao IN (
                'SUSPENDER_USUARIO',
                'REATIVAR_USUARIO',
                'EXCLUIR_USUARIO',
                'OCULTAR_SERVICO',
                'RESTAURAR_SERVICO',
                'REMOVER_AVALIACAO',
                'RESTAURAR_AVALIACAO'
            )
        ),

    CONSTRAINT ck_acao_moderacao_motivo
        CHECK (LENGTH(BTRIM(motivo)) BETWEEN 5 AND 500),

    CONSTRAINT ck_acao_moderacao_alvo
        CHECK (alvo_id > 0)
);

CREATE INDEX ix_acao_moderacao_alvo
    ON public.acao_moderacao (tipo_alvo, alvo_id);

CREATE INDEX ix_acao_moderacao_administrador
    ON public.acao_moderacao (administrador_id, data_acao DESC);

CREATE INDEX ix_acao_moderacao_data
    ON public.acao_moderacao (data_acao DESC);

CREATE TABLE public.token_recuperacao (
    id              BIGINT GENERATED ALWAYS AS IDENTITY,
    usuario_id      BIGINT NOT NULL,
    token_hash     VARCHAR(64) NOT NULL,
    expira_em       TIMESTAMPTZ NOT NULL,
    usado_em        TIMESTAMPTZ,
    criado_em       TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_token_recuperacao
        PRIMARY KEY (id),

    CONSTRAINT fk_token_recuperacao_usuario
        FOREIGN KEY (usuario_id)
        REFERENCES public.usuario (id)
        ON DELETE CASCADE,

    CONSTRAINT ux_token_recuperacao_hash
        UNIQUE (token_hash),

    CONSTRAINT ck_token_recuperacao_expiracao
        CHECK (expira_em > criado_em),

    CONSTRAINT ck_token_recuperacao_uso
        CHECK (usado_em IS NULL OR usado_em >= criado_em)
);

CREATE INDEX ix_token_recuperacao_usuario
    ON public.token_recuperacao (usuario_id);

CREATE INDEX ix_token_recuperacao_expira
    ON public.token_recuperacao (expira_em);

CREATE INDEX ix_token_recuperacao_disponivel
    ON public.token_recuperacao (token_hash, expira_em)
    WHERE usado_em IS NULL;

CREATE TABLE public.refresh_token (
    id              BIGINT GENERATED ALWAYS AS IDENTITY,
    usuario_id      BIGINT NOT NULL,
    token_hash      VARCHAR(64) NOT NULL,
    familia         UUID NOT NULL,
    expira_em       TIMESTAMPTZ NOT NULL,
    revogado_em     TIMESTAMPTZ,
    criado_em       TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_refresh_token
        PRIMARY KEY (id),

    CONSTRAINT fk_refresh_token_usuario
        FOREIGN KEY (usuario_id)
        REFERENCES public.usuario (id)
        ON DELETE CASCADE,

    CONSTRAINT ux_refresh_token_hash
        UNIQUE (token_hash),

    CONSTRAINT ck_refresh_token_expiracao
        CHECK (expira_em > criado_em),

    CONSTRAINT ck_refresh_token_revogacao
        CHECK (revogado_em IS NULL OR revogado_em >= criado_em)
);

CREATE INDEX ix_refresh_token_usuario
    ON public.refresh_token (usuario_id);

CREATE INDEX ix_refresh_token_familia
    ON public.refresh_token (familia);

CREATE INDEX ix_refresh_token_expira
    ON public.refresh_token (expira_em);

CREATE INDEX ix_refresh_token_ativo
    ON public.refresh_token (token_hash, expira_em)
    WHERE revogado_em IS NULL;

CREATE OR REPLACE FUNCTION public.fn_atualizar_timestamp()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.atualizado_em := NOW();
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_usuario_atualizar_timestamp
BEFORE UPDATE ON public.usuario
FOR EACH ROW
EXECUTE FUNCTION public.fn_atualizar_timestamp();

CREATE TRIGGER trg_servico_atualizar_timestamp
BEFORE UPDATE ON public.servico
FOR EACH ROW
EXECUTE FUNCTION public.fn_atualizar_timestamp();

CREATE TRIGGER trg_interesse_atualizar_timestamp
BEFORE UPDATE ON public.interesse
FOR EACH ROW
EXECUTE FUNCTION public.fn_atualizar_timestamp();

CREATE OR REPLACE FUNCTION public.fn_validar_interesse()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_solicitante_id    BIGINT;
    v_status_servico    VARCHAR(15);
    v_oculto            BOOLEAN;
    v_situacao_usuario  VARCHAR(10);
BEGIN
    SELECT
        solicitante_id,
        status,
        oculto_por_moderacao
    INTO
        v_solicitante_id,
        v_status_servico,
        v_oculto
    FROM public.servico
    WHERE id = NEW.servico_id
    FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'O serviço informado não existe.';
    END IF;

    SELECT situacao
    INTO v_situacao_usuario
    FROM public.usuario
    WHERE id = NEW.usuario_id;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'O usuário informado não existe.';
    END IF;

    IF v_situacao_usuario <> 'ATIVO' THEN
        RAISE EXCEPTION 'Somente usuários ativos podem demonstrar interesse.';
    END IF;

    IF v_solicitante_id = NEW.usuario_id THEN
        RAISE EXCEPTION 'O usuário não pode demonstrar interesse no próprio serviço.';
    END IF;

    IF TG_OP = 'INSERT' THEN
        IF v_status_servico <> 'ABERTO' THEN
            RAISE EXCEPTION 'O serviço não está aberto para novos interessados.';
        END IF;

        IF v_oculto = TRUE THEN
            RAISE EXCEPTION 'O serviço está indisponível por moderação.';
        END IF;

        IF NEW.status <> 'PENDENTE' THEN
            RAISE EXCEPTION 'Um novo interesse deve ser criado com status PENDENTE.';
        END IF;
    END IF;

    IF TG_OP = 'UPDATE' THEN
        IF OLD.usuario_id <> NEW.usuario_id
           OR OLD.servico_id <> NEW.servico_id THEN
            RAISE EXCEPTION 'Não é permitido alterar o usuário ou o serviço de um interesse.';
        END IF;

        IF OLD.status = 'RETIRADO' AND NEW.status <> 'RETIRADO' THEN
            RAISE EXCEPTION 'Um interesse retirado não pode ser reativado.';
        END IF;

        IF OLD.status = 'SELECIONADO'
           AND NEW.status <> 'SELECIONADO' THEN
            RAISE EXCEPTION 'Um interesse selecionado não pode ser retirado ou recusado.';
        END IF;

        IF NEW.status = 'RETIRADO'
           AND OLD.status <> 'PENDENTE' THEN
            RAISE EXCEPTION 'Somente interesses pendentes podem ser retirados.';
        END IF;

        IF NEW.status IN ('SELECIONADO', 'NAO_SELECIONADO')
           AND OLD.status <> 'PENDENTE' THEN
            RAISE EXCEPTION 'Somente interesses pendentes podem ser selecionados ou não selecionados.';
        END IF;

        IF NEW.status IN ('SELECIONADO', 'NAO_SELECIONADO')
           AND v_status_servico NOT IN ('ABERTO', 'SELECIONADO') THEN
            RAISE EXCEPTION 'O serviço não permite alteração na seleção de interessados.';
        END IF;
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_interesse_validar
BEFORE INSERT OR UPDATE ON public.interesse
FOR EACH ROW
EXECUTE FUNCTION public.fn_validar_interesse();

CREATE OR REPLACE FUNCTION public.fn_validar_avaliacao()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_solicitante_id        BIGINT;
    v_status_servico        VARCHAR(15);
    v_avaliador_selecionado BOOLEAN;
    v_avaliado_selecionado  BOOLEAN;
BEGIN
    SELECT
        solicitante_id,
        status
    INTO
        v_solicitante_id,
        v_status_servico
    FROM public.servico
    WHERE id = NEW.servico_id;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'O serviço informado não existe.';
    END IF;

    IF v_status_servico <> 'CONCLUIDO' THEN
        RAISE EXCEPTION 'A avaliação somente pode ocorrer após a conclusão do serviço.';
    END IF;

    SELECT EXISTS (
        SELECT 1
        FROM public.interesse
        WHERE servico_id = NEW.servico_id
          AND usuario_id = NEW.avaliador_id
          AND status = 'SELECIONADO'
    )
    INTO v_avaliador_selecionado;

    SELECT EXISTS (
        SELECT 1
        FROM public.interesse
        WHERE servico_id = NEW.servico_id
          AND usuario_id = NEW.avaliado_id
          AND status = 'SELECIONADO'
    )
    INTO v_avaliado_selecionado;

    IF NEW.avaliador_id = v_solicitante_id THEN
        IF v_avaliado_selecionado = FALSE THEN
            RAISE EXCEPTION 'O solicitante somente pode avaliar usuários selecionados para o serviço.';
        END IF;
    ELSIF NEW.avaliado_id = v_solicitante_id THEN
        IF v_avaliador_selecionado = FALSE THEN
            RAISE EXCEPTION 'Somente usuários selecionados podem avaliar o solicitante.';
        END IF;
    ELSE
        RAISE EXCEPTION 'A avaliação deve ocorrer entre o solicitante e um usuário selecionado.';
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_avaliacao_validar
BEFORE INSERT OR UPDATE OF avaliador_id, avaliado_id, servico_id
ON public.avaliacao
FOR EACH ROW
EXECUTE FUNCTION public.fn_validar_avaliacao();

CREATE OR REPLACE FUNCTION public.fn_recalcular_reputacao(
    p_usuario_id BIGINT
)
RETURNS VOID
LANGUAGE plpgsql
AS $$
DECLARE
    v_media   NUMERIC(3,2);
    v_total   INTEGER;
BEGIN
    SELECT
        COALESCE(ROUND(AVG(nota)::NUMERIC, 2), 0.00),
        COUNT(*)::INTEGER
    INTO
        v_media,
        v_total
    FROM public.avaliacao
    WHERE avaliado_id = p_usuario_id
      AND removida_por_moderacao = FALSE;

    UPDATE public.usuario
    SET
        reputacao_media = v_media,
        total_avaliacoes = v_total
    WHERE id = p_usuario_id;
END;
$$;

CREATE OR REPLACE FUNCTION public.fn_sincronizar_reputacao()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        PERFORM public.fn_recalcular_reputacao(OLD.avaliado_id);
        RETURN OLD;
    END IF;

    IF TG_OP = 'INSERT' THEN
        PERFORM public.fn_recalcular_reputacao(NEW.avaliado_id);
        RETURN NEW;
    END IF;

    IF OLD.avaliado_id <> NEW.avaliado_id THEN
        PERFORM public.fn_recalcular_reputacao(OLD.avaliado_id);
    END IF;

    PERFORM public.fn_recalcular_reputacao(NEW.avaliado_id);

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_avaliacao_sincronizar_reputacao
AFTER INSERT OR UPDATE OR DELETE
ON public.avaliacao
FOR EACH ROW
EXECUTE FUNCTION public.fn_sincronizar_reputacao();

CREATE OR REPLACE FUNCTION public.fn_validar_administrador_moderacao()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_papel       VARCHAR(10);
    v_situacao    VARCHAR(10);
    v_alvo_existe BOOLEAN;
BEGIN
    SELECT papel, situacao
    INTO v_papel, v_situacao
    FROM public.usuario
    WHERE id = NEW.administrador_id;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'O administrador informado não existe.';
    END IF;

    IF v_papel <> 'ADMIN' THEN
        RAISE EXCEPTION 'Somente usuários com papel ADMIN podem realizar ações de moderação.';
    END IF;

    IF v_situacao <> 'ATIVO' THEN
        RAISE EXCEPTION 'Somente administradores ativos podem realizar ações de moderação.';
    END IF;

    IF NEW.tipo_alvo = 'USUARIO' THEN
        SELECT EXISTS (
            SELECT 1
            FROM public.usuario
            WHERE id = NEW.alvo_id
        )
        INTO v_alvo_existe;
    ELSIF NEW.tipo_alvo = 'SERVICO' THEN
        SELECT EXISTS (
            SELECT 1
            FROM public.servico
            WHERE id = NEW.alvo_id
        )
        INTO v_alvo_existe;
    ELSIF NEW.tipo_alvo = 'AVALIACAO' THEN
        SELECT EXISTS (
            SELECT 1
            FROM public.avaliacao
            WHERE id = NEW.alvo_id
        )
        INTO v_alvo_existe;
    ELSE
        v_alvo_existe := FALSE;
    END IF;

    IF v_alvo_existe = FALSE THEN
        RAISE EXCEPTION 'O alvo informado para a moderação não existe.';
    END IF;

    IF NEW.acao IN (
        'SUSPENDER_USUARIO',
        'REATIVAR_USUARIO',
        'EXCLUIR_USUARIO'
    ) AND NEW.tipo_alvo <> 'USUARIO' THEN
        RAISE EXCEPTION 'A ação informada somente pode ser aplicada a usuários.';
    END IF;

    IF NEW.acao IN (
        'OCULTAR_SERVICO',
        'RESTAURAR_SERVICO'
    ) AND NEW.tipo_alvo <> 'SERVICO' THEN
        RAISE EXCEPTION 'A ação informada somente pode ser aplicada a serviços.';
    END IF;

    IF NEW.acao IN (
        'REMOVER_AVALIACAO',
        'RESTAURAR_AVALIACAO'
    ) AND NEW.tipo_alvo <> 'AVALIACAO' THEN
        RAISE EXCEPTION 'A ação informada somente pode ser aplicada a avaliações.';
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_acao_moderacao_validar
BEFORE INSERT ON public.acao_moderacao
FOR EACH ROW
EXECUTE FUNCTION public.fn_validar_administrador_moderacao();

CREATE OR REPLACE FUNCTION public.fn_aplicar_acao_moderacao()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.acao = 'SUSPENDER_USUARIO' THEN
        UPDATE public.usuario
        SET situacao = 'SUSPENSO'
        WHERE id = NEW.alvo_id;

    ELSIF NEW.acao = 'REATIVAR_USUARIO' THEN
        UPDATE public.usuario
        SET situacao = 'ATIVO'
        WHERE id = NEW.alvo_id;

    ELSIF NEW.acao = 'EXCLUIR_USUARIO' THEN
        UPDATE public.usuario
        SET situacao = 'EXCLUIDO'
        WHERE id = NEW.alvo_id;

    ELSIF NEW.acao = 'OCULTAR_SERVICO' THEN
        UPDATE public.servico
        SET oculto_por_moderacao = TRUE
        WHERE id = NEW.alvo_id;

    ELSIF NEW.acao = 'RESTAURAR_SERVICO' THEN
        UPDATE public.servico
        SET oculto_por_moderacao = FALSE
        WHERE id = NEW.alvo_id;

    ELSIF NEW.acao = 'REMOVER_AVALIACAO' THEN
        UPDATE public.avaliacao
        SET
            removida_por_moderacao = TRUE,
            removida_em = NOW()
        WHERE id = NEW.alvo_id;

    ELSIF NEW.acao = 'RESTAURAR_AVALIACAO' THEN
        UPDATE public.avaliacao
        SET
            removida_por_moderacao = FALSE,
            removida_em = NULL
        WHERE id = NEW.alvo_id;
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_acao_moderacao_aplicar
AFTER INSERT ON public.acao_moderacao
FOR EACH ROW
EXECUTE FUNCTION public.fn_aplicar_acao_moderacao();

INSERT INTO public.categoria (nome)
VALUES
    ('Casa e montagem'),
    ('Tecnologia'),
    ('Jardinagem'),
    ('Limpeza'),
    ('Pequenos reparos'),
    ('Mudanças'),
    ('Cuidados com animais'),
    ('Eventos e fotografia');

GRANT SELECT, INSERT, UPDATE, DELETE
ON ALL TABLES IN SCHEMA public
TO maozinha_app;

GRANT USAGE, SELECT
ON ALL SEQUENCES IN SCHEMA public
TO maozinha_app;

REVOKE EXECUTE ON FUNCTION public.fn_atualizar_timestamp() FROM PUBLIC;
REVOKE EXECUTE ON FUNCTION public.fn_validar_interesse() FROM PUBLIC;
REVOKE EXECUTE ON FUNCTION public.fn_validar_avaliacao() FROM PUBLIC;
REVOKE EXECUTE ON FUNCTION public.fn_recalcular_reputacao(BIGINT) FROM PUBLIC;
REVOKE EXECUTE ON FUNCTION public.fn_sincronizar_reputacao() FROM PUBLIC;
REVOKE EXECUTE ON FUNCTION public.fn_validar_administrador_moderacao() FROM PUBLIC;
REVOKE EXECUTE ON FUNCTION public.fn_aplicar_acao_moderacao() FROM PUBLIC;

GRANT EXECUTE ON FUNCTION public.fn_atualizar_timestamp()
TO maozinha_app;

GRANT EXECUTE ON FUNCTION public.fn_validar_interesse()
TO maozinha_app;

GRANT EXECUTE ON FUNCTION public.fn_validar_avaliacao()
TO maozinha_app;

GRANT EXECUTE ON FUNCTION public.fn_recalcular_reputacao(BIGINT)
TO maozinha_app;

GRANT EXECUTE ON FUNCTION public.fn_sincronizar_reputacao()
TO maozinha_app;

GRANT EXECUTE ON FUNCTION public.fn_validar_administrador_moderacao()
TO maozinha_app;

GRANT EXECUTE ON FUNCTION public.fn_aplicar_acao_moderacao()
TO maozinha_app;

COMMIT;