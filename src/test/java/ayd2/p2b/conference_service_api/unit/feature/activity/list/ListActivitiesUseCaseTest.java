package ayd2.p2b.conference_service_api.unit.feature.activity.list;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.common.response.PageResponse;
import ayd2.p2b.conference_service_api.feature.activity.application.list.ListActivitiesUseCase;
import ayd2.p2b.conference_service_api.feature.activity.application.port.ActivityCongressRoomPort;
import ayd2.p2b.conference_service_api.feature.activity.application.port.ActivityLeaderRepositoryPort;
import ayd2.p2b.conference_service_api.feature.activity.application.port.ActivityRepositoryPort;
import ayd2.p2b.conference_service_api.feature.activity.domain.model.Activity;
import ayd2.p2b.conference_service_api.feature.activity.domain.model.ActivityLeader;
import ayd2.p2b.conference_service_api.feature.activity.domain.model.ActivityLeaderType;
import ayd2.p2b.conference_service_api.feature.activity.domain.model.ActivityType;
import ayd2.p2b.conference_service_api.feature.activity.dto.internal.ActivitySearchCriteria;
import ayd2.p2b.conference_service_api.feature.activity.dto.response.ActivityResponse;
import ayd2.p2b.conference_service_api.feature.activity.mapper.ActivityMapper;
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
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListActivitiesUseCaseTest {

    @Mock
    private ActivityRepositoryPort activityRepositoryPort;
    @Mock
    private ActivityLeaderRepositoryPort activityLeaderRepositoryPort;
    @Mock
    private ActivityCongressRoomPort activityCongressRoomPort;
    @Mock
    private ActivityMapper activityMapper;

    private ListActivitiesUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ListActivitiesUseCase(
                activityRepositoryPort,
                activityLeaderRepositoryPort,
                activityCongressRoomPort,
                activityMapper
        );
    }

    @Test
    void shouldReturnMappedPageResponse() {
        UUID congressId = UUID.randomUUID();
        Activity activity = Activity.builder()
                .id(UUID.randomUUID())
                .congressId(congressId)
                .roomId(UUID.randomUUID())
                .name("Ponencia")
                .description("Desc")
                .type(ActivityType.PONENCIA)
                .startTime(OffsetDateTime.parse("2026-10-10T10:00:00Z"))
                .endTime(OffsetDateTime.parse("2026-10-10T11:00:00Z"))
                .build();

        when(activityCongressRoomPort.existsPublicCongressById(congressId)).thenReturn(true);
        when(activityRepositoryPort.findPublicByCongressId(congressId, ActivitySearchCriteria.builder().build(), PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(activity), PageRequest.of(0, 20), 1));
        UUID leaderId = UUID.randomUUID();
        when(activityLeaderRepositoryPort.findByActivityIds(Set.of(activity.getId())))
                .thenReturn(Map.of(
                        activity.getId(),
                        List.of(ActivityLeader.builder().userId(leaderId).leaderType(ActivityLeaderType.SPEAKER).build())
                ));
        when(activityMapper.toResponse(activity)).thenReturn(ActivityResponse.builder().id(activity.getId()).build());

        PageResponse<ActivityResponse> response = useCase.execute(
                congressId,
                ActivitySearchCriteria.builder().build(),
                PageRequest.of(0, 20)
        );

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().getFirst().getLeaders()).hasSize(1);
    }

    @Test
    void shouldReturnNotFoundWhenCongressIsNotPublic() {
        UUID congressId = UUID.randomUUID();
        when(activityCongressRoomPort.existsPublicCongressById(congressId)).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(congressId, ActivitySearchCriteria.builder().build(), PageRequest.of(0, 20)))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(apiException.getCode()).isEqualTo("resource.not_found");
                });
    }
}
