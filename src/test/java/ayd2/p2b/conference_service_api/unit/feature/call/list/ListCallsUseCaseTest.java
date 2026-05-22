package ayd2.p2b.conference_service_api.unit.feature.call.list;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.feature.call.application.list.ListCallsUseCase;
import ayd2.p2b.conference_service_api.feature.call.application.port.CallCongressPort;
import ayd2.p2b.conference_service_api.feature.call.application.port.CallRepositoryPort;
import ayd2.p2b.conference_service_api.feature.call.domain.model.Call;
import ayd2.p2b.conference_service_api.feature.call.domain.model.CallStatus;
import ayd2.p2b.conference_service_api.feature.call.dto.response.CallResponse;
import ayd2.p2b.conference_service_api.feature.call.mapper.CallMapper;
import ayd2.p2b.conference_service_api.common.response.PageResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListCallsUseCaseTest {

    @Mock
    private CallRepositoryPort callRepositoryPort;
    @Mock
    private CallCongressPort callCongressPort;
    @Mock
    private CallMapper callMapper;

    private ListCallsUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ListCallsUseCase(callRepositoryPort, callCongressPort, callMapper);
    }

    @Test
    void shouldListCallsForPublicCongress() {
        UUID congressId = UUID.randomUUID();
        Call call = Call.builder()
                .id(UUID.randomUUID())
                .congressId(congressId)
                .status(CallStatus.OPEN)
                .openedAt(OffsetDateTime.now())
                .build();
        PageRequest pageable = PageRequest.of(0, 20);

        when(callCongressPort.existsPublicCongressById(congressId)).thenReturn(true);
        when(callRepositoryPort.findPublicByCongressId(congressId, pageable))
                .thenReturn(new PageImpl<>(List.of(call), pageable, 1));
        when(callMapper.toResponse(call)).thenReturn(CallResponse.builder()
                .id(call.getId())
                .congressId(congressId)
                .status(CallStatus.OPEN)
                .openedAt(call.getOpenedAt())
                .build());

        PageResponse<CallResponse> response = useCase.execute(congressId, pageable);

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().getFirst().getId()).isEqualTo(call.getId());
        assertThat(response.getTotalItems()).isEqualTo(1);
    }

    @Test
    void shouldRejectMissingPublicCongress() {
        UUID congressId = UUID.randomUUID();
        when(callCongressPort.existsPublicCongressById(congressId)).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(congressId, PageRequest.of(0, 20)))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(apiException.getCode()).isEqualTo("resource.not_found");
                });
    }
}
