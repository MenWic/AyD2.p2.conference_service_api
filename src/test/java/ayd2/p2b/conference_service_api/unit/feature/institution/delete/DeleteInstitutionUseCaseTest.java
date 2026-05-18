package ayd2.p2b.conference_service_api.unit.feature.institution.delete;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.feature.institution.application.delete.DeleteInstitutionUseCase;
import ayd2.p2b.conference_service_api.feature.institution.application.port.InstitutionDependencyPort;
import ayd2.p2b.conference_service_api.feature.institution.application.port.InstitutionRepositoryPort;
import ayd2.p2b.conference_service_api.feature.institution.domain.model.Institution;
import ayd2.p2b.conference_service_api.feature.institution.dto.response.InstitutionResponse;
import ayd2.p2b.conference_service_api.feature.institution.mapper.InstitutionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteInstitutionUseCaseTest {

    @Mock
    private InstitutionRepositoryPort repositoryPort;
    @Mock
    private InstitutionDependencyPort dependencyPort;
    @Mock
    private InstitutionMapper mapper;

    private DeleteInstitutionUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new DeleteInstitutionUseCase(repositoryPort, dependencyPort, mapper);
    }

    @Test
    void shouldReturnConflictWhenInstitutionHasCongressDependency() {
        UUID institutionId = UUID.randomUUID();
        when(repositoryPort.findById(institutionId)).thenReturn(Optional.of(sampleInstitution(institutionId, true)));
        when(dependencyPort.hasCongressesByInstitutionId(institutionId)).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(institutionId, UUID.randomUUID()))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(apiException.getCode()).isEqualTo("resource.conflict");
                });

        verify(repositoryPort, never()).save(any());
    }

    @Test
    void shouldSoftDeleteInstitutionWhenNoDependenciesExist() {
        UUID institutionId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        Institution institution = sampleInstitution(institutionId, true);
        when(repositoryPort.findById(institutionId)).thenReturn(Optional.of(institution));
        when(dependencyPort.hasCongressesByInstitutionId(institutionId)).thenReturn(false);
        when(repositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toResponse(any())).thenAnswer(invocation -> {
            Institution saved = invocation.getArgument(0);
            return InstitutionResponse.builder()
                    .id(saved.getId())
                    .active(saved.isActive())
                    .build();
        });

        InstitutionResponse response = useCase.execute(institutionId, actorId);

        ArgumentCaptor<Institution> captor = ArgumentCaptor.forClass(Institution.class);
        verify(repositoryPort).save(captor.capture());
        Institution saved = captor.getValue();
        assertThat(saved.isActive()).isFalse();
        assertThat(saved.getUpdatedBy()).isEqualTo(actorId);
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved).isNotSameAs(institution);
        assertThat(institution.isActive()).isTrue();
        assertThat(response.isActive()).isFalse();
    }

    @Test
    void shouldReturnConflictWhenInactiveInstitutionStillHasCongressDependencies() {
        UUID institutionId = UUID.randomUUID();
        when(repositoryPort.findById(institutionId)).thenReturn(Optional.of(sampleInstitution(institutionId, false)));
        when(dependencyPort.hasCongressesByInstitutionId(institutionId)).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(institutionId, UUID.randomUUID()))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(apiException.getCode()).isEqualTo("resource.conflict");
                });

        verify(repositoryPort, never()).save(any());
    }

    private Institution sampleInstitution(UUID institutionId, boolean active) {
        return Institution.builder()
                .id(institutionId)
                .name("USAC")
                .description("Public University")
                .contactEmail("admin@usac.edu.gt")
                .active(active)
                .build();
    }
}
