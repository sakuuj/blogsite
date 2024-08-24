package by.sakuuj.blogsite.article.service;

import by.sakuuj.blogsite.article.entity.jpa.CreationId;
import by.sakuuj.blogsite.article.entity.jpa.embeddable.IdempotencyTokenId;
import by.sakuuj.blogsite.article.entity.jpa.entities.IdempotencyTokenEntity;

import java.util.Optional;

public interface IdempotencyTokenService {

    Optional<IdempotencyTokenEntity> findById(IdempotencyTokenId id);

    void create(IdempotencyTokenId idempotencyTokenId, CreationId creationId);

    void deleteByCreationId(CreationId creationId);

}
