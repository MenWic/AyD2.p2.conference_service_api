package ayd2.p2b.conference_service_api.unit.feature.institution.list;

import ayd2.p2b.conference_service_api.common.response.PageResponse;
import ayd2.p2b.conference_service_api.feature.institution.application.list.ListInstitutionsUseCase;
import ayd2.p2b.conference_service_api.feature.institution.application.port.InstitutionRepositoryPort;
import ayd2.p2b.conference_service_api.feature.institution.domain.model.Institution;
import ayd2.p2b.conference_service_api.feature.institution.dto.response.InstitutionResponse;
import ayd2.p2b.conference_service_api.feature.institution.mapper.InstitutionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListInstitutionsUseCaseTest {

    @Mock
    private InstitutionRepositoryPort repositoryPort;
    @Mock
    private InstitutionMapper mapper;

    private ListInstitutionsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ListInstitutionsUseCase(repositoryPort, mapper);
    }

    @Test
    void shouldReturnActiveInstitutionsOnly() {
        Pageable pageable = PageRequest.of(0, 20);
        Institution first = Institution.builder()
                .id(UUID.randomUUID())
                .name("USAC")
                .active(true)
                .build();
        Institution second = Institution.builder()
                .id(UUID.randomUUID())
                .name("URL")
                .active(true)
                .build();

        when(repositoryPort.findAllActive(pageable)).thenReturn(new PageImpl<>(List.of(first, second), pageable, 2));
        when(mapper.toResponse(first)).thenReturn(InstitutionResponse.builder().id(first.getId()).name(first.getName()).active(true).build());
        when(mapper.toResponse(second)).thenReturn(InstitutionResponse.builder().id(second.getId()).name(second.getName()).active(true).build());

        PageResponse<InstitutionResponse> response = useCase.execute(pageable);

        assertThat(response.getItems()).hasSize(2);
        assertThat(response.getItems()).allMatch(InstitutionResponse::isActive);
        assertThat(response.getTotalItems()).isEqualTo(2);
    }
}
