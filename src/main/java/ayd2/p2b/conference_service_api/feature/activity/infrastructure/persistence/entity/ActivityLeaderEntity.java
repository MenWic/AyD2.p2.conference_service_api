package ayd2.p2b.conference_service_api.feature.activity.infrastructure.persistence.entity;

import ayd2.p2b.conference_service_api.feature.activity.domain.model.ActivityLeaderType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.UUID;

@Entity
@Table(name = "activity_leaders")
@IdClass(ActivityLeaderEntityId.class)
@Getter
@Setter
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
public class ActivityLeaderEntity {

    @Id
    @Column(name = "activity_id", nullable = false)
    @ToString.Include
    private UUID activityId;

    @Id
    @Column(name = "user_id", nullable = false)
    @ToString.Include
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "leader_type", nullable = false)
    private ActivityLeaderType leaderType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", insertable = false, updatable = false)
    private ActivityEntity activity;
}
