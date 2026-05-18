package ayd2.p2b.conference_service_api.unit.feature.institution.update;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.feature.institution.application.port.InstitutionRepositoryPort;
import ayd2.p2b.conference_service_api.feature.institution.application.update.UpdateInstitutionUseCase;
import ayd2.p2b.conference_service_api.feature.institution.domain.model.Institution;
import ayd2.p2b.conference_service_api.feature.institution.dto.request.UpdateInstitutionRequest;
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
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateInstitutionUseCaseTest {

    @Mock
    private InstitutionRepositoryPort repositoryPort;
    @Mock
    private InstitutionMapper mapper;

    private UpdateInstitutionUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new UpdateInstitutionUseCase(repositoryPort, mapper);
    }

    @Test
    void shouldRejectDuplicateName() {
        UUID institutionId = UUID.randomUUID();
        Institution institution = Institution.builder()
                .id(institutionId)
                .name("USAC")
                .description("Description")
                .contactEmail("admin@usac.edu.gt")
                .active(true)
                .build();

        when(repositoryPort.findById(institutionId)).thenReturn(Optional.of(institution));
        when(repositoryPort.existsByNameAndIdNot("URL", institutionId)).thenReturn(true);

        UpdateInstitutionRequest request = UpdateInstitutionRequest.builder()
                .name("URL")
                .build();

        assertThatThrownBy(() -> useCase.execute(institutionId, request, UUID.randomUUID()))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(apiException.getCode()).isEqualTo("resource.conflict");
                });
    }

    @Test
    void shouldUpdateProvidedFields() {
        UUID institutionId = UUID.randomUUID();
        UUID actorId = UUID.randomUUID();
        Institution institution = Institution.builder()
                .id(institutionId)
                .name("USAC")
                .description("Description")
                .contactEmail("admin@usac.edu.gt")
                .active(true)
                .build();
        when(repositoryPort.findById(institutionId)).thenReturn(Optional.of(institution));
        when(repositoryPort.existsByNameAndIdNot("USAC Updated", institutionId)).thenReturn(false);
        when(repositoryPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toResponse(any())).thenAnswer(invocation -> {
            Institution saved = invocation.getArgument(0);
            return ayd2.p2b.conference_service_api.feature.institution.dto.response.InstitutionResponse.builder()
                    .id(saved.getId())
                    .name(saved.getName())
                    .description(saved.getDescription())
                    .contactEmail(saved.getContactEmail())
                    .active(saved.isActive())
                    .build();
        });

        UpdateInstitutionRequest request = UpdateInstitutionRequest.builder()
                .name("USAC Updated")
                .build();
        var response = useCase.execute(institutionId, request, actorId);

        ArgumentCaptor<Institution> captor = ArgumentCaptor.forClass(Institution.class);
        verify(repositoryPort).save(captor.capture());
        Institution saved = captor.getValue();

        assertThat(response.getName()).isEqualTo("USAC Updated");
        assertThat(saved.getUpdatedBy()).isEqualTo(actorId);
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved).isNotSameAs(institution);
        assertThat(institution.getName()).isEqualTo("USAC");
    }
}
