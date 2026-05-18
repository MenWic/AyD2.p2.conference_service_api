package ayd2.p2b.conference_service_api.unit.feature.institution.create;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.feature.institution.application.create.CreateInstitutionUseCase;
import ayd2.p2b.conference_service_api.feature.institution.application.port.InstitutionRepositoryPort;
import ayd2.p2b.conference_service_api.feature.institution.domain.model.Institution;
import ayd2.p2b.conference_service_api.feature.institution.dto.request.CreateInstitutionRequest;
import ayd2.p2b.conference_service_api.feature.institution.dto.response.InstitutionResponse;
import ayd2.p2b.conference_service_api.feature.institution.mapper.InstitutionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateInstitutionUseCaseTest {

    @Mock
    private InstitutionRepositoryPort repositoryPort;
    @Mock
    private InstitutionMapper mapper;

    private CreateInstitutionUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateInstitutionUseCase(repositoryPort, mapper);
    }

    @Test
    void shouldTrimFieldsAndCreateInstitutionWhenInputIsValid() {
        UUID actorId = UUID.randomUUID();
        CreateInstitutionRequest request = CreateInstitutionRequest.builder()
                .name("  USAC  ")
                .description("  Public University  ")
                .contactEmail("  admin@usac.edu.gt  ")
                .build();

        when(repositoryPort.existsByName("USAC")).thenReturn(false);
        when(repositoryPort.save(any())).thenAnswer(invocation -> {
            Institution institution = invocation.getArgument(0);
            return institution.toBuilder()
                    .id(UUID.randomUUID())
                    .build();
        });
        when(mapper.toResponse(any())).thenAnswer(invocation -> {
            Institution institution = invocation.getArgument(0);
            return InstitutionResponse.builder()
                    .id(institution.getId())
                    .name(institution.getName())
                    .description(institution.getDescription())
                    .contactEmail(institution.getContactEmail())
                    .active(institution.isActive())
                    .build();
        });

        InstitutionResponse response = useCase.execute(request, actorId);

        ArgumentCaptor<Institution> captor = ArgumentCaptor.forClass(Institution.class);
        verify(repositoryPort).save(captor.capture());
        Institution saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("USAC");
        assertThat(saved.getDescription()).isEqualTo("Public University");
        assertThat(saved.getContactEmail()).isEqualTo("admin@usac.edu.gt");
        assertThat(saved.getCreatedBy()).isEqualTo(actorId);
        assertThat(saved.isActive()).isTrue();
        assertThat(response.getName()).isEqualTo("USAC");
    }

    @Test
    void shouldRejectDuplicateName() {
        CreateInstitutionRequest request = CreateInstitutionRequest.builder()
                .name("USAC")
                .description("Public University")
                .contactEmail("admin@usac.edu.gt")
                .build();
        when(repositoryPort.existsByName("USAC")).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(request, UUID.randomUUID()))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(apiException.getCode()).isEqualTo("resource.conflict");
                });
    }

    @Test
    void shouldRejectBlankName() {
        CreateInstitutionRequest request = CreateInstitutionRequest.builder()
                .name("  ")
                .description("Public University")
                .contactEmail("admin@usac.edu.gt")
                .build();

        assertThatThrownBy(() -> useCase.execute(request, UUID.randomUUID()))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(apiException.getCode()).isEqualTo("validation.failed");
                });
    }

    @Test
    void shouldRejectInvalidEmail() {
        CreateInstitutionRequest request = CreateInstitutionRequest.builder()
                .name("USAC")
                .description("Public University")
                .contactEmail("invalid-email")
                .build();

        assertThatThrownBy(() -> useCase.execute(request, UUID.randomUUID()))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(apiException.getCode()).isEqualTo("validation.failed");
                });
    }
}
