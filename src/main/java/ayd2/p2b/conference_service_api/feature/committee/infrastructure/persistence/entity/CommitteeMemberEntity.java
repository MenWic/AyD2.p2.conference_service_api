package ayd2.p2b.conference_service_api.feature.committee.infrastructure.persistence.entity;

import ayd2.p2b.conference_service_api.feature.congress.infrastructure.persistence.entity.CongressEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "committee_members")
@IdClass(CommitteeMemberId.class)
@Getter
@Setter
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
public class CommitteeMemberEntity {

    @Id
    @Column(name = "congress_id", nullable = false)
    @ToString.Include
    private UUID congressId;

    @Id
    @Column(name = "user_id", nullable = false)
    @ToString.Include
    private UUID userId;

    @Column(name = "added_at", nullable = false)
    private OffsetDateTime addedAt;

    @Column(name = "added_by", nullable = false)
    private UUID addedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "congress_id", insertable = false, updatable = false)
    private CongressEntity congress;

    @PrePersist
    void prePersist() {
        if (addedAt == null) {
            addedAt = OffsetDateTime.now();
        }
    }
}
