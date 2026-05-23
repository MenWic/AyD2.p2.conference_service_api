package ayd2.p2b.conference_service_api.feature.proposal.infrastructure.persistence.entity;

import ayd2.p2b.conference_service_api.feature.call.infrastructure.persistence.entity.CallEntity;
import ayd2.p2b.conference_service_api.feature.proposal.domain.model.ProposalStatus;
import ayd2.p2b.conference_service_api.feature.proposal.domain.model.ProposalType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "proposals")
@Getter
@Setter
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
public class ProposalEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @ToString.Include
    private UUID id;

    @Column(name = "call_id", nullable = false)
    private UUID callId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "call_id", insertable = false, updatable = false)
    private CallEntity call;

    @Column(name = "author_user_id", nullable = false)
    private UUID authorUserId;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "description", nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private ProposalType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ProposalStatus status;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;

    @Column(name = "created_activity_id")
    private UUID createdActivityId;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void prePersist() {
        OffsetDateTime now = OffsetDateTime.now();
        if (status == null) {
            status = ProposalStatus.PENDING;
        }
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
