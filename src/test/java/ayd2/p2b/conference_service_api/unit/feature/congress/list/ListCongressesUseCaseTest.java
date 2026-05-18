package ayd2.p2b.conference_service_api.unit.feature.congress.list;

import ayd2.p2b.conference_service_api.common.response.PageResponse;
import ayd2.p2b.conference_service_api.feature.congress.application.list.ListCongressesUseCase;
import ayd2.p2b.conference_service_api.feature.congress.application.port.CongressRepositoryPort;
import ayd2.p2b.conference_service_api.feature.congress.domain.model.Congress;
import ayd2.p2b.conference_service_api.feature.congress.dto.internal.CongressSearchCriteria;
import ayd2.p2b.conference_service_api.feature.congress.dto.internal.CongressView;
import ayd2.p2b.conference_service_api.feature.congress.dto.response.CongressResponse;
import ayd2.p2b.conference_service_api.feature.congress.mapper.CongressMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListCongressesUseCaseTest {

    @Mock
    private CongressRepositoryPort congressRepositoryPort;
    @Mock
    private CongressMapper congressMapper;

    private ListCongressesUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ListCongressesUseCase(congressRepositoryPort, congressMapper);
    }

    @Test
    void shouldApplyFiltersAndMapPageResponse() {
        UUID institutionId = UUID.randomUUID();
        Congress congress = Congress.builder()
                .id(UUID.randomUUID())
                .institutionId(institutionId)
                .name("Congreso IA")
                .description("Investigacion")
                .startDate(LocalDate.of(2026, 11, 15))
                .endDate(LocalDate.of(2026, 11, 17))
                .location("Guatemala")
                .price(new BigDecimal("55.00"))
                .build();
        CongressView view = CongressView.builder().congress(congress).institutionName("USAC").build();

        CongressSearchCriteria criteria = CongressSearchCriteria.builder()
                .institutionId(institutionId)
                .startDateFrom(LocalDate.of(2026, 11, 1))
                .startDateTo(LocalDate.of(2026, 11, 30))
                .search("IA")
                .build();
        PageRequest pageable = PageRequest.of(0, 20);

        when(congressRepositoryPort.findPublicByCriteria(eq(criteria), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(view), pageable, 1));
        when(congressMapper.toResponse(any(), eq("USAC"))).thenReturn(CongressResponse.builder()
                .id(congress.getId())
                .institutionName("USAC")
                .build());

        PageResponse<CongressResponse> response = useCase.execute(criteria, pageable);

        verify(congressRepositoryPort).findPublicByCriteria(criteria, pageable);
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getTotalItems()).isEqualTo(1);
        assertThat(response.getTotalPages()).isEqualTo(1);
    }
}
