package ayd2.p2b.conference_service_api.unit.feature.congress.delete;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.core.security.Role;
import ayd2.p2b.conference_service_api.feature.congress.application.delete.DeleteCongressUseCase;
import ayd2.p2b.conference_service_api.feature.congress.application.port.CongressDeletionGuardPort;
import ayd2.p2b.conference_service_api.feature.congress.application.port.CongressInstitutionPort;
import ayd2.p2b.conference_service_api.feature.congress.application.port.CongressRepositoryPort;
import ayd2.p2b.conference_service_api.feature.congress.domain.model.Congress;
import ayd2.p2b.conference_service_api.feature.congress.dto.internal.CongressRequesterContext;
import ayd2.p2b.conference_service_api.feature.congress.dto.internal.InstitutionSummary;
import ayd2.p2b.conference_service_api.feature.congress.dto.response.CongressResponse;
import ayd2.p2b.conference_service_api.feature.congress.mapper.CongressMapper;
import ayd2.p2b.conference_service_api.integration.port.IamUserLookupPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteCongressUseCaseTest {

    @Mock
    private CongressRepositoryPort congressRepositoryPort;
    @Mock
    private CongressInstitutionPort congressInstitutionPort;
    @Mock
    private CongressDeletionGuardPort congressDeletionGuardPort;
    @Mock
    private IamUserLookupPort iamUserLookupPort;
    @Mock
    private CongressMapper congressMapper;

    private DeleteCongressUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new DeleteCongressUseCase(
                congressRepositoryPort,
                congressInstitutionPort,
                congressDeletionGuardPort,
                iamUserLookupPort,
                congressMapper
        );
    }

    @Test
    void shouldReturnConflictWhenDependenciesExist() {
        UUID congressId = UUID.randomUUID();
        UUID institutionId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Congress existing = sampleCongress(congressId, institutionId, ownerId);
        CongressRequesterContext requester = requester(ownerId);

        when(congressRepositoryPort.findById(congressId)).thenReturn(Optional.of(existing));
        when(iamUserLookupPort.isCongressAdminLinkedToInstitution(ownerId, institutionId, "token")).thenReturn(false);
        when(congressDeletionGuardPort.findBlockingDependencies(congressId)).thenReturn(List.of("rooms", "activities"));

        assertThatThrownBy(() -> useCase.execute(congressId, requester))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(apiException.getCode()).isEqualTo("resource.conflict");
                });

        verify(congressRepositoryPort, never()).deleteById(congressId);
    }

    @Test
    void shouldDeleteWhenAuthorizedAndNoDependents() {
        UUID congressId = UUID.randomUUID();
        UUID institutionId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Congress existing = sampleCongress(congressId, institutionId, ownerId);
        CongressRequesterContext requester = requester(ownerId);

        when(congressRepositoryPort.findById(congressId)).thenReturn(Optional.of(existing));
        when(iamUserLookupPort.isCongressAdminLinkedToInstitution(ownerId, institutionId, "token")).thenReturn(false);
        when(congressDeletionGuardPort.findBlockingDependencies(congressId)).thenReturn(List.of());
        when(congressInstitutionPort.findActiveInstitutionById(institutionId))
                .thenReturn(Optional.of(InstitutionSummary.builder().id(institutionId).name("USAC").build()));
        when(congressMapper.toResponse(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq("USAC")))
                .thenAnswer(invocation -> {
                    Congress congress = invocation.getArgument(0);
                    return CongressResponse.builder()
                            .id(congress.getId())
                            .institutionName("USAC")
                            .build();
                });

        CongressResponse response = useCase.execute(congressId, requester);

        verify(congressRepositoryPort).deleteById(congressId);
        assertThat(response.getId()).isEqualTo(congressId);
        assertThat(response.getInstitutionName()).isEqualTo("USAC");
    }

    private Congress sampleCongress(UUID congressId, UUID institutionId, UUID createdBy) {
        return Congress.builder()
                .id(congressId)
                .institutionId(institutionId)
                .name("Congreso")
                .description("Descripcion")
                .startDate(LocalDate.of(2026, 8, 10))
                .endDate(LocalDate.of(2026, 8, 12))
                .location("Guatemala")
                .price(new BigDecimal("40.00"))
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
