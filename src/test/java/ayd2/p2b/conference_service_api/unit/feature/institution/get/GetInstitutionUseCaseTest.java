package ayd2.p2b.conference_service_api.unit.feature.institution.get;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.feature.institution.application.get.GetInstitutionUseCase;
import ayd2.p2b.conference_service_api.feature.institution.application.port.InstitutionRepositoryPort;
import ayd2.p2b.conference_service_api.feature.institution.dto.response.InstitutionResponse;
import ayd2.p2b.conference_service_api.feature.institution.mapper.InstitutionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetInstitutionUseCaseTest {

    @Mock
    private InstitutionRepositoryPort repositoryPort;
    @Mock
    private InstitutionMapper mapper;

    private GetInstitutionUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetInstitutionUseCase(repositoryPort, mapper);
    }

    @Test
    void shouldReturnNotFoundWhenInstitutionIsInactiveOrMissing() {
        UUID institutionId = UUID.randomUUID();
        when(repositoryPort.findActiveById(institutionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(institutionId))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(apiException.getCode()).isEqualTo("resource.not_found");
                });
    }

    @Test
    void shouldReturnInstitutionWhenActive() {
        UUID institutionId = UUID.randomUUID();
        var institution = ayd2.p2b.conference_service_api.feature.institution.domain.model.Institution.builder()
                .id(institutionId)
                .name("USAC")
                .active(true)
                .build();
        when(repositoryPort.findActiveById(institutionId)).thenReturn(Optional.of(institution));
        when(mapper.toResponse(institution)).thenReturn(InstitutionResponse.builder().id(institutionId).name("USAC").active(true).build());

        InstitutionResponse response = useCase.execute(institutionId);

        assertThat(response.getId()).isEqualTo(institutionId);
        assertThat(response.isActive()).isTrue();
    }
}
