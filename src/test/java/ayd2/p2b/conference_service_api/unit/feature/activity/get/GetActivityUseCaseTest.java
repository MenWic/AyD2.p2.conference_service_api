package ayd2.p2b.conference_service_api.unit.feature.activity.get;

import ayd2.p2b.conference_service_api.common.exception.ApiException;
import ayd2.p2b.conference_service_api.feature.activity.application.get.GetActivityUseCase;
import ayd2.p2b.conference_service_api.feature.activity.application.port.ActivityLeaderRepositoryPort;
import ayd2.p2b.conference_service_api.feature.activity.application.port.ActivityRepositoryPort;
import ayd2.p2b.conference_service_api.feature.activity.domain.model.ActivityLeader;
import ayd2.p2b.conference_service_api.feature.activity.domain.model.ActivityLeaderType;
import ayd2.p2b.conference_service_api.feature.activity.domain.model.Activity;
import ayd2.p2b.conference_service_api.feature.activity.domain.model.ActivityType;
import ayd2.p2b.conference_service_api.feature.activity.dto.response.ActivityResponse;
import ayd2.p2b.conference_service_api.feature.activity.mapper.ActivityMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetActivityUseCaseTest {

    @Mock
    private ActivityRepositoryPort activityRepositoryPort;
    @Mock
    private ActivityLeaderRepositoryPort activityLeaderRepositoryPort;
    @Mock
    private ActivityMapper activityMapper;

    private GetActivityUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetActivityUseCase(activityRepositoryPort, activityLeaderRepositoryPort, activityMapper);
    }

    @Test
    void shouldReturnPublicActivity() {
        UUID activityId = UUID.randomUUID();
        Activity activity = Activity.builder()
                .id(activityId)
                .congressId(UUID.randomUUID())
                .roomId(UUID.randomUUID())
                .name("Ponencia")
                .description("Desc")
                .type(ActivityType.PONENCIA)
                .startTime(OffsetDateTime.parse("2026-10-10T10:00:00Z"))
                .endTime(OffsetDateTime.parse("2026-10-10T11:00:00Z"))
                .build();
        when(activityRepositoryPort.findPublicById(activityId)).thenReturn(Optional.of(activity));
        when(activityMapper.toResponse(activity)).thenReturn(ActivityResponse.builder().id(activityId).build());
        UUID leaderId = UUID.randomUUID();
        when(activityLeaderRepositoryPort.findByActivityId(activityId)).thenReturn(List.of(
                ActivityLeader.builder().userId(leaderId).leaderType(ActivityLeaderType.SPEAKER).build()
        ));

        ActivityResponse response = useCase.execute(activityId);

        assertThat(response.getId()).isEqualTo(activityId);
        assertThat(response.getLeaders()).containsExactly(leaderId);
    }

    @Test
    void shouldReturnNotFoundWhenMissing() {
        UUID activityId = UUID.randomUUID();
        when(activityRepositoryPort.findPublicById(activityId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(activityId))
                .isInstanceOf(ApiException.class)
                .satisfies(ex -> {
                    ApiException apiException = (ApiException) ex;
                    assertThat(apiException.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(apiException.getCode()).isEqualTo("resource.not_found");
                });
    }
}
