package by.sakuuj.articles.article.mapper.jpa;

import by.sakuuj.articles.article.dto.ArticleRequest;
import by.sakuuj.articles.article.dto.ArticleResponse;
import by.sakuuj.articles.article.mapper.LocalDateTimeMapper;
import by.sakuuj.articles.entity.jpa.entities.ArticleEntity;
import org.mapstruct.InjectionStrategy;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

import java.util.UUID;

@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR,
        uses = {
                TopicMapper.class,
                PersonMapper.class,
                ToReferenceMapper.class,
                LocalDateTimeMapper.class
        }
)
public interface ArticleMapper {

    @Mapping(target = "author", source = "authorId")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "articleTopics", ignore = true)
    @Mapping(target = "modificationAudit", ignore = true)
    ArticleEntity toEntity(ArticleRequest request, UUID authorId);

    @Mapping(target = "topics", source = "entity.articleTopics")
    @Mapping(target = "createdAt", source = "entity.modificationAudit.createdAt")
    @Mapping(target = "updatedAt", source = "entity.modificationAudit.updatedAt")
    ArticleResponse toResponse(ArticleEntity entity);


    @Mapping(target = "id", ignore = true)
    @Mapping(target = "author", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "articleTopics", ignore = true)
    @Mapping(target = "modificationAudit", ignore = true)
    void updateEntity(@MappingTarget ArticleEntity entity, ArticleRequest request);
}
