package ayd2.p2b.conference_service_api.unit.feature.congress.create;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.core.security.Role;
import ayd2.p2b.conference_service_api.feature.congress.application.exception.CongressExceptions;
import ayd2.p2b.conference_service_api.feature.congress.application.create.CreateCongressUseCase;
import ayd2.p2b.conference_service_api.feature.congress.application.port.CongressInstitutionPort;
import ayd2.p2b.conference_service_api.feature.congress.application.port.CongressRepositoryPort;
import ayd2.p2b.conference_service_api.feature.congress.domain.model.Congress;
import ayd2.p2b.conference_service_api.feature.congress.dto.internal.CongressRequesterContext;
import ayd2.p2b.conference_service_api.feature.congress.dto.internal.InstitutionSummary;
import ayd2.p2b.conference_service_api.feature.congress.dto.request.CreateCongressRequest;
import ayd2.p2b.conference_service_api.feature.congress.dto.response.CongressResponse;
import ayd2.p2b.conference_service_api.feature.congress.mapper.CongressMapper;
import ayd2.p2b.conference_service_api.integration.port.IamUserLookupPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateCongressUseCaseTest {

    @Mock
    private CongressRepositoryPort congressRepositoryPort;
    @Mock
    private CongressInstitutionPort congressInstitutionPort;
    @Mock
    private IamUserLookupPort iamUserLookupPort;
    @Mock
    private CongressMapper congressMapper;

    private CreateCongressUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateCongressUseCase(
                congressRepositoryPort,
                congressInstitutionPort,
                iamUserLookupPort,
                congressMapper
        );
    }

    @Test
    void shouldRejectMissingOrInactiveInstitution() {
        UUID institutionId = UUID.randomUUID();
        when(congressInstitutionPort.findActiveInstitutionById(institutionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(sampleRequest(institutionId), requester(Set.of(Role.CONGRESS_ADMIN))))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(apiException.getCode()).isEqualTo("resource.not_found");
                });
    }

    @Test
    void shouldRejectRequesterWithoutCongressAdminRole() {
        assertThatThrownBy(() -> useCase.execute(sampleRequest(UUID.randomUUID()), requester(Set.of(Role.PARTICIPANT))))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(apiException.getCode()).isEqualTo("auth.forbidden");
                });
    }

    @Test
    void shouldRejectUnlinkedCongressAdmin() {
        UUID institutionId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        when(congressInstitutionPort.findActiveInstitutionById(institutionId))
                .thenReturn(Optional.of(InstitutionSummary.builder().id(institutionId).name("USAC").build()));
        when(iamUserLookupPort.isCongressAdminLinkedToInstitution(actorId, institutionId, "token")).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(sampleRequest(institutionId), requester(actorId, Set.of(Role.CONGRESS_ADMIN))))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(apiException.getCode()).isEqualTo("auth.forbidden");
                });
    }

    @Test
    void shouldSaveCreatedByAndReturnInstitutionName() {
        UUID institutionId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        InstitutionSummary institutionSummary = InstitutionSummary.builder()
                .id(institutionId)
                .name("USAC")
                .build();

        when(congressInstitutionPort.findActiveInstitutionById(institutionId)).thenReturn(Optional.of(institutionSummary));
        when(iamUserLookupPort.isCongressAdminLinkedToInstitution(actorId, institutionId, "token")).thenReturn(true);
        when(congressRepositoryPort.save(any())).thenAnswer(invocation -> {
            Congress congress = invocation.getArgument(0);
            return congress.toBuilder()
                    .id(UUID.randomUUID())
                    .build();
        });
        when(congressMapper.toResponse(any(), any())).thenAnswer(invocation -> {
            Congress congress = invocation.getArgument(0);
            String institutionName = invocation.getArgument(1);
            return CongressResponse.builder()
                    .id(congress.getId())
                    .name(congress.getName())
                    .institutionName(institutionName)
                    .createdBy(congress.getCreatedBy())
                    .build();
        });

        CongressResponse response = useCase.execute(sampleRequest(institutionId), requester(actorId, Set.of(Role.CONGRESS_ADMIN)));

        ArgumentCaptor<Congress> captor = ArgumentCaptor.forClass(Congress.class);
        verify(congressRepositoryPort).save(captor.capture());
        Congress saved = captor.getValue();

        assertThat(saved.getCreatedBy()).isEqualTo(actorId);
        assertThat(saved.getName()).isEqualTo("Congreso A2");
        assertThat(saved.getDescription()).isEqualTo("Descripcion A2");
        assertThat(saved.getLocation()).isEqualTo("Guatemala");
        assertThat(response.getInstitutionName()).isEqualTo("USAC");
    }

    @Test
    void shouldPropagateIamUnavailableAsServiceUnavailable() {
        UUID institutionId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        when(congressInstitutionPort.findActiveInstitutionById(institutionId))
                .thenReturn(Optional.of(InstitutionSummary.builder().id(institutionId).name("USAC").build()));
        when(iamUserLookupPort.isCongressAdminLinkedToInstitution(actorId, institutionId, "token"))
                .thenThrow(CongressExceptions.iamUnavailable());

        assertThatThrownBy(() -> useCase.execute(sampleRequest(institutionId), requester(actorId, Set.of(Role.CONGRESS_ADMIN))))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(apiException.getCode()).isEqualTo("integration.iam_unavailable");
                });
    }

    private CreateCongressRequest sampleRequest(UUID institutionId) {
        return CreateCongressRequest.builder()
                .name("  Congreso A2  ")
                .description("  Descripcion A2  ")
                .startDate(LocalDate.of(2026, 7, 10))
                .endDate(LocalDate.of(2026, 7, 12))
                .location("  Guatemala  ")
                .price(new BigDecimal("40.00"))
                .institutionId(institutionId)
                .build();
    }

    private CongressRequesterContext requester(Set<Role> roles) {
        return requester(UUID.randomUUID(), roles);
    }

    private CongressRequesterContext requester(UUID userId, Set<Role> roles) {
        return CongressRequesterContext.builder()
                .userId(userId)
                .roles(roles)
                .accessToken("token")
                .build();
    }
}
