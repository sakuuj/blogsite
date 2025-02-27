package by.sakuuj.articles.article.service;

import by.sakuuj.articles.article.PagingTestDataBuilder;
import by.sakuuj.articles.article.TopicTestDataBuilder;
import by.sakuuj.articles.article.dto.TopicRequest;
import by.sakuuj.articles.article.dto.TopicResponse;
import by.sakuuj.articles.article.dto.validator.DtoValidator;
import by.sakuuj.articles.article.exception.EntityNotFoundException;
import by.sakuuj.articles.article.exception.EntityVersionDoesNotMatch;
import by.sakuuj.articles.article.exception.IdempotencyTokenExistsException;
import by.sakuuj.articles.article.mapper.jpa.TopicMapper;
import by.sakuuj.articles.article.repository.jpa.TopicRepository;
import by.sakuuj.articles.article.service.authorization.TopicServiceAuthorizer;
import by.sakuuj.articles.entity.jpa.CreationId;
import by.sakuuj.articles.entity.jpa.embeddable.IdempotencyTokenId;
import by.sakuuj.articles.entity.jpa.entities.IdempotencyTokenEntity;
import by.sakuuj.articles.entity.jpa.entities.TopicEntity;
import by.sakuuj.articles.paging.PageView;
import by.sakuuj.articles.paging.RequestedPage;
import by.sakuuj.articles.service.IdempotencyTokenService;
import by.sakuuj.articles.security.AuthenticatedUser;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TopicServiceImplTests {

    @Mock
    private TopicServiceAuthorizer topicServiceAuthorizer;
    @Mock
    private DtoValidator dtoValidator;
    @Mock
    private TopicRepository topicRepository;
    @Mock
    private TopicMapper topicMapper;
    @Mock
    private IdempotencyTokenService idempotencyTokenService;

    @InjectMocks
    private TopicServiceImpl topicServiceImpl;

    @Nested
    class findById_UUID {

        @Test
        void shouldFindById_WhenEntityIsPresent() {

            // given
            TopicTestDataBuilder topicBuilder = TopicTestDataBuilder.aTopic();

            UUID idToFindBy = topicBuilder.getId();
            TopicEntity topicEntity = topicBuilder.build();
            TopicResponse topicResponse = topicBuilder.buildResponse();

            when(topicRepository.findById(any())).thenReturn(Optional.of(topicEntity));
            when(topicMapper.toResponse(any())).thenReturn(topicResponse);

            // when
            Optional<TopicResponse> actual = topicServiceImpl.findById(idToFindBy);

            // then
            assertThat(actual).contains(topicResponse);

            verify(topicRepository).findById(idToFindBy);
            verifyNoMoreInteractions(topicRepository);

            verify(topicMapper).toResponse(topicEntity);
            verifyNoMoreInteractions(topicMapper);

            verifyNoInteractions(
                    topicServiceAuthorizer,
                    dtoValidator,
                    idempotencyTokenService
            );
        }

        @Test
        void shouldNotFindById_WhenEntityIsNotPresent() {

            // given
            TopicTestDataBuilder topicBuilder = TopicTestDataBuilder.aTopic();

            UUID idToFindBy = topicBuilder.getId();

            when(topicRepository.findById(any())).thenReturn(Optional.empty());

            // when
            Optional<TopicResponse> actual = topicServiceImpl.findById(idToFindBy);

            // then
            assertThat(actual).isEmpty();

            verify(topicRepository).findById(idToFindBy);
            verifyNoMoreInteractions(topicRepository);

            verifyNoInteractions(topicMapper);

            verifyNoInteractions(
                    topicServiceAuthorizer,
                    dtoValidator,
                    idempotencyTokenService
            );
        }
    }


    @Nested
    class findAll_RequestedPage {

        @Captor
        ArgumentCaptor<Pageable> pageableArgumentCaptor;

        @Test
        void shouldSetPageAndSortByCreatedAtDesc() {
            // given
            PagingTestDataBuilder pagingBuilder = PagingTestDataBuilder.aPaging();
            RequestedPage requestedPage = pagingBuilder.aRequestedPage();

            when(topicRepository.findAll(any())).thenReturn(pagingBuilder.emptySlice());

            // when
            topicServiceImpl.findAllSortByCreatedAtDesc(requestedPage);

            // then
            verify(topicRepository).findAll(pageableArgumentCaptor.capture());
            Pageable actualPageable = pageableArgumentCaptor.getValue();
            assertThat(actualPageable.getPageSize()).isEqualTo(requestedPage.size());
            assertThat(actualPageable.getPageNumber()).isEqualTo(requestedPage.number());

            Sort sort = actualPageable.getSort();
            Sort.Order createdAtOrder = sort.getOrderFor("modificationAudit.createdAt");
            assertThat(createdAtOrder).isNotNull();
            assertThat(createdAtOrder.isDescending()).isTrue();
        }

        @Test
        void shouldSearchInRepo_ThenMap() {
            // given
            TopicTestDataBuilder topicBuilder = TopicTestDataBuilder.aTopic();

            TopicTestDataBuilder firstTopicBuilder = topicBuilder
                    .withId(UUID.fromString("309affca-2e66-493e-b050-caeebff5a9c9"))
                    .withName("first topic");
            TopicTestDataBuilder secondTopicBuilder = topicBuilder
                    .withId(UUID.fromString("60f41c11-f5b0-4602-8fd1-8e0a7c347449"))
                    .withName("second topic");

            TopicEntity firstTopicEntity = firstTopicBuilder.build();
            TopicResponse firstTopicResponse = firstTopicBuilder.buildResponse();

            TopicEntity secondTopicEntity = secondTopicBuilder.build();
            TopicResponse secondTopicResponse = secondTopicBuilder.buildResponse();

            PagingTestDataBuilder pagingBuilder = PagingTestDataBuilder.aPaging();
            RequestedPage requestedPage = pagingBuilder.aRequestedPage();

            when(topicRepository.findAll(any())).thenReturn(pagingBuilder.aSlice(
                    List.of(firstTopicEntity, secondTopicEntity)
            ));
            when(topicMapper.toResponse(firstTopicEntity)).thenReturn(firstTopicResponse);
            when(topicMapper.toResponse(secondTopicEntity)).thenReturn(secondTopicResponse);

            // when
            PageView<TopicResponse> found = topicServiceImpl.findAllSortByCreatedAtDesc(requestedPage);

            // then
            assertThat(found.content()).containsExactly(firstTopicResponse, secondTopicResponse);

            verify(topicRepository).findAll(any());

            verifyNoMoreInteractions(topicRepository);

            verify(topicMapper).toResponse(firstTopicEntity);
            verify(topicMapper).toResponse(secondTopicEntity);
            verifyNoMoreInteractions(topicMapper);

            verifyNoInteractions(
                    topicServiceAuthorizer,
                    dtoValidator,
                    idempotencyTokenService
            );
        }
    }

    @Nested
    class create_TopicRequest_UUID_UUID_AuthenticatedUser {

        @Test
        void shouldCreate_IfIdempotencyTokenDoesNotExist() {

            // given
            doNothing().when(topicServiceAuthorizer).authorizeCreate(any());
            doNothing().when(dtoValidator).validate(any());

            AuthenticatedUser authenticatedUser = AuthenticatedUser.builder().build();

            TopicTestDataBuilder topicBuilder = TopicTestDataBuilder.aTopic();
            TopicEntity topicEntity = topicBuilder.build();
            TopicRequest topicRequest = topicBuilder.buildRequest();
            UUID expected = topicEntity.getId();

            when(topicMapper.toEntity(any(TopicRequest.class))).thenReturn(topicEntity);
            when(topicRepository.save(any())).thenReturn(topicEntity);

            UUID idempotencyTokenValue = UUID.fromString("d95b3c07-91c0-4443-aaa0-beffb98f452a");
            var idempotencyTokenId = IdempotencyTokenId.builder()
                    .clientId(authenticatedUser.id())
                    .idempotencyTokenValue(idempotencyTokenValue)
                    .build();
            var creationId = CreationId.of(TopicEntity.class, topicEntity.getId());

            when(idempotencyTokenService.findById(any())).thenReturn(Optional.empty());
            doNothing().when(idempotencyTokenService).create(any(), any());

            // when
            UUID actual = topicServiceImpl.create(topicRequest, idempotencyTokenValue, authenticatedUser);

            // then
            assertThat(actual).isEqualTo(expected);

            InOrder inOrder = Mockito.inOrder(
                    topicServiceAuthorizer,
                    dtoValidator,
                    idempotencyTokenService,
                    topicRepository,
                    topicMapper
            );

            inOrder.verify(topicServiceAuthorizer).authorizeCreate(same(authenticatedUser));

            inOrder.verify(dtoValidator).validate(topicRequest);

            inOrder.verify(idempotencyTokenService).findById(idempotencyTokenId);

            inOrder.verify(topicMapper).toEntity(topicRequest);

            inOrder.verify(topicRepository).save(topicEntity);

            inOrder.verify(idempotencyTokenService).create(idempotencyTokenId, creationId);

            inOrder.verifyNoMoreInteractions();
        }

        @Test
        void shouldThrowException_IfIdempotencyTokenExists() {

            // given
            doNothing().when(topicServiceAuthorizer).authorizeCreate(any());
            doNothing().when(dtoValidator).validate(any());

            AuthenticatedUser authenticatedUser = AuthenticatedUser.builder().build();

            TopicTestDataBuilder topicBuilder = TopicTestDataBuilder.aTopic();
            TopicEntity topicEntity = topicBuilder.build();
            TopicRequest topicRequest = topicBuilder.buildRequest();

            UUID idempotencyTokenValue = UUID.fromString("d95b3c07-91c0-4443-aaa0-beffb98f452a");
            var idempotencyTokenId = IdempotencyTokenId.builder()
                    .clientId(authenticatedUser.id())
                    .idempotencyTokenValue(idempotencyTokenValue)
                    .build();
            var creationId = CreationId.of(TopicEntity.class, topicEntity.getId());

            IdempotencyTokenEntity idempotencyToken = IdempotencyTokenEntity.builder()
                    .id(idempotencyTokenId)
                    .creationId(creationId)
                    .build();

            when(idempotencyTokenService.findById(any())).thenReturn(Optional.of(idempotencyToken));

            // when, then
            assertThatThrownBy(() -> topicServiceImpl.create(topicRequest, idempotencyTokenValue, authenticatedUser))
                    .isInstanceOf(IdempotencyTokenExistsException.class);

            InOrder inOrder = Mockito.inOrder(
                    topicServiceAuthorizer,
                    dtoValidator,
                    idempotencyTokenService
            );

            inOrder.verify(topicServiceAuthorizer).authorizeCreate(same(authenticatedUser));

            inOrder.verify(dtoValidator).validate(topicRequest);

            inOrder.verify(idempotencyTokenService).findById(idempotencyTokenId);

            verifyNoInteractions(
                    topicRepository,
                    topicMapper
            );
        }
    }

    @Nested
    class updateById_UUID_TopicRequest_short_AuthenticatedUser {

        @Test
        void shouldThrowException_WhenEntityIsNotFound() {
            // given
            doNothing().when(topicServiceAuthorizer).authorizeUpdate(any(), any());
            doNothing().when(dtoValidator).validate(any());

            AuthenticatedUser authenticatedUser = AuthenticatedUser.builder().build();

            TopicTestDataBuilder topicBuilder = TopicTestDataBuilder.aTopic();
            TopicEntity topicEntity = topicBuilder
                    .build();
            UUID topicId = topicEntity.getId();
            short topicVersion = topicEntity.getVersion();
            TopicRequest topicRequest = topicBuilder.buildRequest();

            when(topicRepository.findById(any())).thenReturn(Optional.empty());

            // when
            assertThatThrownBy(() -> topicServiceImpl.updateById(topicId, topicRequest, topicVersion, authenticatedUser))
                    .isInstanceOf(EntityNotFoundException.class);

            // then
            InOrder inOrder = Mockito.inOrder(
                    topicServiceAuthorizer,
                    dtoValidator,
                    topicRepository
            );

            inOrder.verify(topicServiceAuthorizer).authorizeUpdate(eq(topicId), same(authenticatedUser));

            inOrder.verify(dtoValidator).validate(topicRequest);

            inOrder.verify(topicRepository).findById(topicId);

            inOrder.verifyNoMoreInteractions();

            verifyNoInteractions(
                    idempotencyTokenService,
                    topicMapper
            );
        }

        @Test
        void shouldThrowException_WhenIncorrectVersionDetectedComparingToFoundFromRepo() {
            // given
            doNothing().when(topicServiceAuthorizer).authorizeUpdate(any(), any());
            doNothing().when(dtoValidator).validate(any());

            AuthenticatedUser authenticatedUser = AuthenticatedUser.builder().build();

            TopicTestDataBuilder topicBuilder = TopicTestDataBuilder.aTopic();
            TopicEntity topicEntity = topicBuilder
                    .withVersion((short) 133)
                    .build();
            UUID topicId = topicEntity.getId();
            short incorrectTopicVersion = 777;
            TopicRequest topicRequest = topicBuilder.buildRequest();

            when(topicRepository.findById(any())).thenReturn(Optional.of(topicEntity));

            // when
            assertThatThrownBy(() -> topicServiceImpl.updateById(topicId, topicRequest, incorrectTopicVersion, authenticatedUser))
                    .isInstanceOf(EntityVersionDoesNotMatch.class);

            // then
            InOrder inOrder = Mockito.inOrder(
                    topicServiceAuthorizer,
                    dtoValidator,
                    topicRepository
            );

            inOrder.verify(topicServiceAuthorizer).authorizeUpdate(eq(topicId), same(authenticatedUser));

            inOrder.verify(dtoValidator).validate(topicRequest);

            inOrder.verify(topicRepository).findById(topicId);

            inOrder.verifyNoMoreInteractions();

            verifyNoInteractions(
                    idempotencyTokenService,
                    topicMapper
            );
        }


        @Test
        void shouldUpdateWithMapper_On_CorrectVersion() {
            // given
            doNothing().when(topicServiceAuthorizer).authorizeUpdate(any(), any());
            doNothing().when(dtoValidator).validate(any());

            AuthenticatedUser authenticatedUser = AuthenticatedUser.builder().build();

            TopicTestDataBuilder topicBuilder = TopicTestDataBuilder.aTopic();
            TopicEntity topicEntity = topicBuilder.build();
            UUID topicId = topicEntity.getId();
            short topicVersion = topicEntity.getVersion();
            TopicRequest topicRequest = topicBuilder.buildRequest();

            doNothing().when(topicMapper).updateEntity(any(), any());
            when(topicRepository.findById(any())).thenReturn(Optional.of(topicEntity));


            // when
            topicServiceImpl.updateById(topicId, topicRequest, topicVersion, authenticatedUser);

            // then

            InOrder inOrder = Mockito.inOrder(
                    topicServiceAuthorizer,
                    dtoValidator,
                    topicRepository,
                    topicMapper
            );

            inOrder.verify(topicServiceAuthorizer).authorizeUpdate(eq(topicId), same(authenticatedUser));

            inOrder.verify(dtoValidator).validate(topicRequest);

            inOrder.verify(topicRepository).findById(topicId);

            inOrder.verify(topicMapper).updateEntity(topicEntity, topicRequest);

            inOrder.verifyNoMoreInteractions();

            verifyNoInteractions(
                    idempotencyTokenService
            );
        }
    }


    @Nested
    class deleteById_UUID_AuthenticatedUser {

        @Test
        void shouldDeleteById() {
            // given
            doNothing().when(topicServiceAuthorizer).authorizeDelete(any(), any());

            UUID idToDeleteBy = TopicTestDataBuilder.aTopic().getId();
            AuthenticatedUser authenticatedUser = AuthenticatedUser.builder().build();

            doNothing().when(topicRepository).removeById(any());
            doNothing().when(idempotencyTokenService).deleteByCreationId(any());

            // when
            topicServiceImpl.deleteById(idToDeleteBy, authenticatedUser);

            // then
            InOrder inOrder = Mockito.inOrder(
                    topicServiceAuthorizer,
                    topicRepository,
                    idempotencyTokenService
            );

            inOrder.verify(topicServiceAuthorizer).authorizeDelete(eq(idToDeleteBy), same(authenticatedUser));

            inOrder.verify(topicRepository).removeById(idToDeleteBy);

            inOrder.verify(idempotencyTokenService).deleteByCreationId(CreationId.of(TopicEntity.class, idToDeleteBy));

            inOrder.verifyNoMoreInteractions();

            verifyNoInteractions(
                    dtoValidator,
                    topicMapper
            );
        }
    }
}
