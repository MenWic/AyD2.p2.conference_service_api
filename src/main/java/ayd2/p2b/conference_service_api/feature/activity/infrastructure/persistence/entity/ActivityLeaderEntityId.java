package ayd2.p2b.conference_service_api.feature.activity.infrastructure.persistence.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityLeaderEntityId implements Serializable {
    private UUID activityId;
    private UUID userId;
}
