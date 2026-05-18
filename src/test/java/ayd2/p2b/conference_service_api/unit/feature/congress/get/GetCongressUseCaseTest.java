package ayd2.p2b.conference_service_api.unit.feature.congress.get;

import ayd2.p2b.conference_service_api.feature.congress.application.get.GetCongressUseCase;
import ayd2.p2b.conference_service_api.feature.congress.application.port.CongressRepositoryPort;
import ayd2.p2b.conference_service_api.feature.congress.domain.model.Congress;
import ayd2.p2b.conference_service_api.feature.congress.dto.internal.CongressView;
import ayd2.p2b.conference_service_api.feature.congress.dto.response.CongressResponse;
import ayd2.p2b.conference_service_api.feature.congress.mapper.CongressMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetCongressUseCaseTest {

    @Mock
    private CongressRepositoryPort congressRepositoryPort;
    @Mock
    private CongressMapper congressMapper;

    private GetCongressUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetCongressUseCase(congressRepositoryPort, congressMapper);
    }

    @Test
    void shouldReturnCongressResponse() {
        UUID congressId = UUID.randomUUID();
        Congress congress = Congress.builder()
                .id(congressId)
                .institutionId(UUID.randomUUID())
                .name("Congreso")
                .description("Descripcion")
                .startDate(LocalDate.of(2026, 8, 10))
                .endDate(LocalDate.of(2026, 8, 12))
                .location("Guatemala")
                .price(new BigDecimal("40.00"))
                .createdBy(UUID.randomUUID())
                .build();
        CongressView view = CongressView.builder()
                .congress(congress)
                .institutionName("USAC")
                .build();

        when(congressRepositoryPort.findPublicById(congressId)).thenReturn(Optional.of(view));
        when(congressMapper.toResponse(congress, "USAC")).thenReturn(CongressResponse.builder()
                .id(congressId)
                .name("Congreso")
                .institutionName("USAC")
                .build());

        CongressResponse response = useCase.execute(congressId);

        assertThat(response.getId()).isEqualTo(congressId);
        assertThat(response.getInstitutionName()).isEqualTo("USAC");
    }
}
