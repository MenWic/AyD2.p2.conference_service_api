package ayd2.p2b.conference_service_api.unit.feature.proposal.domain;

import ayd2.p2b.conference_service_api.feature.proposal.domain.exception.ProposalDomainException;
import ayd2.p2b.conference_service_api.feature.proposal.domain.model.Proposal;
import ayd2.p2b.conference_service_api.feature.proposal.domain.model.ProposalStatus;
import ayd2.p2b.conference_service_api.feature.proposal.domain.model.ProposalType;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProposalTest {

    @Test
    void shouldAcceptValidPendingProposal() {
        Proposal proposal = pendingBuilder().build();

        assertThatCode(proposal::validateInvariants).doesNotThrowAnyException();
    }

    @Test
    void shouldRejectNullCallId() {
        assertThatThrownBy(() -> pendingBuilder().callId(null).build().validateInvariants())
                .isInstanceOf(ProposalDomainException.class);
    }

    @Test
    void shouldRejectNullAuthorUserId() {
        assertThatThrownBy(() -> pendingBuilder().authorUserId(null).build().validateInvariants())
                .isInstanceOf(ProposalDomainException.class);
    }

    @Test
    void shouldRejectBlankTitle() {
        assertThatThrownBy(() -> pendingBuilder().title(" ").build().validateInvariants())
                .isInstanceOf(ProposalDomainException.class);
    }

    @Test
    void shouldRejectTooLongTitle() {
        String longTitle = "x".repeat(501);
        assertThatThrownBy(() -> pendingBuilder().title(longTitle).build().validateInvariants())
                .isInstanceOf(ProposalDomainException.class);
    }

    @Test
    void shouldRejectBlankDescription() {
        assertThatThrownBy(() -> pendingBuilder().description(" ").build().validateInvariants())
                .isInstanceOf(ProposalDomainException.class);
    }

    @Test
    void shouldRejectNullType() {
        assertThatThrownBy(() -> pendingBuilder().type(null).build().validateInvariants())
                .isInstanceOf(ProposalDomainException.class);
    }

    @Test
    void shouldRejectNullStatus() {
        assertThatThrownBy(() -> pendingBuilder().status(null).build().validateInvariants())
                .isInstanceOf(ProposalDomainException.class);
    }

    @Test
    void shouldRejectNullCreatedBy() {
        assertThatThrownBy(() -> pendingBuilder().createdBy(null).build().validateInvariants())
                .isInstanceOf(ProposalDomainException.class);
    }

    @Test
    void shouldRejectPendingWithReviewedFields() {
        assertThatThrownBy(() -> pendingBuilder()
                .reviewedBy(UUID.randomUUID())
                .reviewedAt(OffsetDateTime.now())
                .build()
                .validateInvariants())
                .isInstanceOf(ProposalDomainException.class);
    }

    @Test
    void shouldRejectApprovedOrRejectedWithoutReviewedFields() {
        Proposal approvedMissingReviewedBy = pendingBuilder()
                .status(ProposalStatus.APPROVED)
                .reviewedAt(OffsetDateTime.now())
                .build();
        Proposal rejectedMissingReviewedAt = pendingBuilder()
                .status(ProposalStatus.REJECTED)
                .reviewedBy(UUID.randomUUID())
                .build();

        assertThatThrownBy(approvedMissingReviewedBy::validateInvariants).isInstanceOf(ProposalDomainException.class);
        assertThatThrownBy(rejectedMissingReviewedAt::validateInvariants).isInstanceOf(ProposalDomainException.class);
    }

    @Test
    void shouldRejectCreatedActivityIdWithNonApprovedStatus() {
        assertThatThrownBy(() -> pendingBuilder()
                .status(ProposalStatus.REJECTED)
                .reviewedBy(UUID.randomUUID())
                .reviewedAt(OffsetDateTime.now())
                .createdActivityId(UUID.randomUUID())
                .build()
                .validateInvariants())
                .isInstanceOf(ProposalDomainException.class);
    }

    @Test
    void shouldApprovePendingProposal() {
        UUID reviewer = UUID.randomUUID();
        OffsetDateTime reviewedAt = OffsetDateTime.parse("2026-10-10T10:00:00Z");

        Proposal approved = pendingBuilder().build().approve(reviewer, reviewedAt);

        assertThat(approved.getStatus()).isEqualTo(ProposalStatus.APPROVED);
        assertThat(approved.getReviewedBy()).isEqualTo(reviewer);
        assertThat(approved.getReviewedAt()).isEqualTo(reviewedAt);
        assertThat(approved.getUpdatedBy()).isEqualTo(reviewer);
    }

    @Test
    void shouldRejectApproveOnNonPending() {
        Proposal approved = pendingBuilder()
                .status(ProposalStatus.APPROVED)
                .reviewedBy(UUID.randomUUID())
                .reviewedAt(OffsetDateTime.now())
                .build();

        assertThatThrownBy(() -> approved.approve(UUID.randomUUID(), OffsetDateTime.now()))
                .isInstanceOf(ProposalDomainException.class);
    }

    @Test
    void shouldRejectApproveWithNullReviewerOrReviewedAt() {
        Proposal pending = pendingBuilder().build();

        assertThatThrownBy(() -> pending.approve(null, OffsetDateTime.now())).isInstanceOf(ProposalDomainException.class);
        assertThatThrownBy(() -> pending.approve(UUID.randomUUID(), null)).isInstanceOf(ProposalDomainException.class);
    }

    @Test
    void shouldRejectPendingProposal() {
        UUID reviewer = UUID.randomUUID();
        OffsetDateTime reviewedAt = OffsetDateTime.parse("2026-10-11T11:00:00Z");

        Proposal rejected = pendingBuilder().build().reject(reviewer, reviewedAt);

        assertThat(rejected.getStatus()).isEqualTo(ProposalStatus.REJECTED);
        assertThat(rejected.getReviewedBy()).isEqualTo(reviewer);
        assertThat(rejected.getReviewedAt()).isEqualTo(reviewedAt);
        assertThat(rejected.getUpdatedBy()).isEqualTo(reviewer);
    }

    @Test
    void shouldRejectRejectOnNonPending() {
        Proposal rejected = pendingBuilder()
                .status(ProposalStatus.REJECTED)
                .reviewedBy(UUID.randomUUID())
                .reviewedAt(OffsetDateTime.now())
                .build();

        assertThatThrownBy(() -> rejected.reject(UUID.randomUUID(), OffsetDateTime.now()))
                .isInstanceOf(ProposalDomainException.class);
    }

    @Test
    void shouldRejectRejectWithNullReviewerOrReviewedAt() {
        Proposal pending = pendingBuilder().build();

        assertThatThrownBy(() -> pending.reject(null, OffsetDateTime.now())).isInstanceOf(ProposalDomainException.class);
        assertThatThrownBy(() -> pending.reject(UUID.randomUUID(), null)).isInstanceOf(ProposalDomainException.class);
    }

    private Proposal.ProposalBuilder pendingBuilder() {
        return Proposal.builder()
                .id(UUID.randomUUID())
                .callId(UUID.randomUUID())
                .authorUserId(UUID.randomUUID())
                .title("Titulo")
                .description("Descripcion")
                .type(ProposalType.PONENCIA)
                .status(ProposalStatus.PENDING)
                .createdBy(UUID.randomUUID());
    }
}
