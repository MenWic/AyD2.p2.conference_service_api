package ayd2.p2b.conference_service_api.unit.feature.congress.update;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.core.security.Role;
import ayd2.p2b.conference_service_api.feature.congress.application.port.CongressInstitutionPort;
import ayd2.p2b.conference_service_api.feature.congress.application.port.CongressRepositoryPort;
import ayd2.p2b.conference_service_api.feature.congress.application.update.UpdateCongressUseCase;
import ayd2.p2b.conference_service_api.feature.congress.domain.model.Congress;
import ayd2.p2b.conference_service_api.feature.congress.dto.internal.CongressRequesterContext;
import ayd2.p2b.conference_service_api.feature.congress.dto.internal.InstitutionSummary;
import ayd2.p2b.conference_service_api.feature.congress.dto.request.UpdateCongressRequest;
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
class UpdateCongressUseCaseTest {

    @Mock
    private CongressRepositoryPort congressRepositoryPort;
    @Mock
    private CongressInstitutionPort congressInstitutionPort;
    @Mock
    private IamUserLookupPort iamUserLookupPort;
    @Mock
    private CongressMapper congressMapper;

    private UpdateCongressUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new UpdateCongressUseCase(
                congressRepositoryPort,
                congressInstitutionPort,
                iamUserLookupPort,
                congressMapper
        );
    }

    @Test
    void shouldRejectUnrelatedCongressAdmin() {
        UUID congressId = UUID.randomUUID();
        UUID institutionId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        Congress existing = sampleCongress(congressId, institutionId, ownerId);

        when(congressRepositoryPort.findById(congressId)).thenReturn(Optional.of(existing));
        when(iamUserLookupPort.isCongressAdminLinkedToInstitution(actorId, institutionId, "token")).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(
                congressId,
                UpdateCongressRequest.builder().name("Nuevo").build(),
                requester(actorId)
        ))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(apiException.getCode()).isEqualTo("auth.forbidden");
                });
    }

    @Test
    void shouldValidateChangedInstitutionIsActiveAndLinked() {
        UUID congressId = UUID.randomUUID();
        UUID oldInstitutionId = UUID.randomUUID();
        UUID newInstitutionId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        Congress existing = sampleCongress(congressId, oldInstitutionId, actorId);

        when(congressRepositoryPort.findById(congressId)).thenReturn(Optional.of(existing));
        when(iamUserLookupPort.isCongressAdminLinkedToInstitution(actorId, oldInstitutionId, "token")).thenReturn(false);
        when(congressInstitutionPort.findActiveInstitutionById(newInstitutionId))
                .thenReturn(Optional.of(InstitutionSummary.builder().id(newInstitutionId).name("URL").build()));
        when(iamUserLookupPort.isCongressAdminLinkedToInstitution(actorId, newInstitutionId, "token")).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(
                congressId,
                UpdateCongressRequest.builder().institutionId(newInstitutionId).build(),
                requester(actorId)
        ))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(apiException.getCode()).isEqualTo("auth.forbidden");
                });
    }

    @Test
    void shouldUpdateUsingNewImmutableInstance() {
        UUID congressId = UUID.randomUUID();
        UUID institutionId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        Congress existing = sampleCongress(congressId, institutionId, actorId);

        when(congressRepositoryPort.findById(congressId)).thenReturn(Optional.of(existing));
        when(iamUserLookupPort.isCongressAdminLinkedToInstitution(actorId, institutionId, "token")).thenReturn(false);
        when(congressInstitutionPort.findActiveInstitutionById(institutionId))
                .thenReturn(Optional.of(InstitutionSummary.builder().id(institutionId).name("USAC").build()));
        when(congressRepositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(congressMapper.toResponse(any(), any())).thenAnswer(invocation -> {
            Congress congress = invocation.getArgument(0);
            return CongressResponse.builder()
                    .id(congress.getId())
                    .name(congress.getName())
                    .location(congress.getLocation())
                    .institutionName(invocation.getArgument(1))
                    .build();
        });

        UpdateCongressRequest request = UpdateCongressRequest.builder()
                .name("Congreso actualizado")
                .location("Antigua Guatemala")
                .price(new BigDecimal("60.00"))
                .build();

        CongressResponse response = useCase.execute(congressId, request, requester(actorId));

        ArgumentCaptor<Congress> captor = ArgumentCaptor.forClass(Congress.class);
        verify(congressRepositoryPort).save(captor.capture());
        Congress saved = captor.getValue();

        assertThat(saved).isNotSameAs(existing);
        assertThat(existing.getName()).isEqualTo("Congreso Original");
        assertThat(saved.getName()).isEqualTo("Congreso actualizado");
        assertThat(saved.getLocation()).isEqualTo("Antigua Guatemala");
        assertThat(saved.getUpdatedBy()).isEqualTo(actorId);
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(response.getInstitutionName()).isEqualTo("USAC");
    }

    private Congress sampleCongress(UUID congressId, UUID institutionId, UUID createdBy) {
        return Congress.builder()
                .id(congressId)
                .institutionId(institutionId)
                .name("Congreso Original")
                .description("Descripcion")
                .startDate(LocalDate.of(2026, 8, 10))
                .endDate(LocalDate.of(2026, 8, 12))
                .location("Guatemala")
                .price(new BigDecimal("50.00"))
                .createdBy(createdBy)
                .build();
    }

    private CongressRequesterContext requester(UUID userId) {
        return CongressRequesterContext.builder()
                .userId(userId)
                .roles(Set.of(Role.CONGRESS_ADMIN))
                .accessToken("token")
                .build();
    }
}
