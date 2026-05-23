package ayd2.p2b.conference_service_api.integration.persistence;

import ayd2.p2b.conference_service_api.feature.activity.domain.model.ActivityType;
import ayd2.p2b.conference_service_api.feature.activity.infrastructure.persistence.entity.ActivityEntity;
import ayd2.p2b.conference_service_api.feature.activity.infrastructure.persistence.repository.ActivityRepository;
import ayd2.p2b.conference_service_api.feature.call.domain.model.CallStatus;
import ayd2.p2b.conference_service_api.feature.call.infrastructure.persistence.entity.CallEntity;
import ayd2.p2b.conference_service_api.feature.call.infrastructure.persistence.repository.CallRepository;
import ayd2.p2b.conference_service_api.feature.congress.infrastructure.persistence.entity.CongressEntity;
import ayd2.p2b.conference_service_api.feature.congress.infrastructure.persistence.repository.CongressRepository;
import ayd2.p2b.conference_service_api.feature.institution.infrastructure.persistence.entity.InstitutionEntity;
import ayd2.p2b.conference_service_api.feature.institution.infrastructure.persistence.repository.InstitutionRepository;
import ayd2.p2b.conference_service_api.feature.proposal.domain.model.ProposalStatus;
import ayd2.p2b.conference_service_api.feature.proposal.domain.model.ProposalType;
import ayd2.p2b.conference_service_api.feature.proposal.infrastructure.persistence.entity.ProposalEntity;
import ayd2.p2b.conference_service_api.feature.proposal.infrastructure.persistence.repository.ProposalRepository;
import ayd2.p2b.conference_service_api.feature.room.infrastructure.persistence.entity.RoomEntity;
import ayd2.p2b.conference_service_api.feature.room.infrastructure.persistence.repository.RoomRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProposalPersistenceTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
        registry.add("spring.flyway.enabled", () -> true);
    }

    @Autowired
    private ProposalRepository proposalRepository;

    @Autowired
    private CallRepository callRepository;

    @Autowired
    private CongressRepository congressRepository;

    @Autowired
    private InstitutionRepository institutionRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldPersistPendingProposalWithNullReviewedFields() {
        CallEntity call = persistedOpenCall("USAC");
        UUID authorUserId = UUID.randomUUID();

        ProposalEntity saved = proposalRepository.saveAndFlush(newPendingProposal(call.getId(), authorUserId));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(ProposalStatus.PENDING);
        assertThat(saved.getReviewedBy()).isNull();
        assertThat(saved.getReviewedAt()).isNull();
    }

    @Test
    void shouldRejectApprovedOrRejectedWithoutReviewedFields() {
        CallEntity call = persistedOpenCall("URL");
        UUID authorUserId = UUID.randomUUID();

        assertThatThrownBy(() -> proposalRepository.saveAndFlush(newReviewedProposalWithoutReviewData(
                call.getId(),
                authorUserId,
                ProposalStatus.APPROVED
        ))).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> proposalRepository.saveAndFlush(newReviewedProposalWithoutReviewData(
                call.getId(),
                authorUserId,
                ProposalStatus.REJECTED
        ))).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectInvalidProposalStatusByCheckConstraint() {
        CallEntity call = persistedOpenCall("Del Valle");
        UUID authorUserId = UUID.randomUUID();

        assertThatThrownBy(() -> {
            entityManager.createNativeQuery("""
                            insert into proposals (
                                id, call_id, author_user_id, title, description, type, status, created_by
                            ) values (
                                :id, :callId, :authorUserId, :title, :description, :type, :status, :createdBy
                            )
                            """)
                    .setParameter("id", UUID.randomUUID())
                    .setParameter("callId", call.getId())
                    .setParameter("authorUserId", authorUserId)
                    .setParameter("title", "Invalid status proposal")
                    .setParameter("description", "Should fail status check.")
                    .setParameter("type", "PONENCIA")
                    .setParameter("status", "INVALID")
                    .setParameter("createdBy", authorUserId)
                    .executeUpdate();
            entityManager.flush();
        }).isInstanceOfAny(DataIntegrityViolationException.class, jakarta.persistence.PersistenceException.class);
    }

    @Test
    void shouldRejectInvalidProposalTypeByCheckConstraint() {
        CallEntity call = persistedOpenCall("Mariano");
        UUID authorUserId = UUID.randomUUID();

        assertThatThrownBy(() -> {
            entityManager.createNativeQuery("""
                            insert into proposals (
                                id, call_id, author_user_id, title, description, type, status, created_by
                            ) values (
                                :id, :callId, :authorUserId, :title, :description, :type, :status, :createdBy
                            )
                            """)
                    .setParameter("id", UUID.randomUUID())
                    .setParameter("callId", call.getId())
                    .setParameter("authorUserId", authorUserId)
                    .setParameter("title", "Invalid type proposal")
                    .setParameter("description", "Should fail type check.")
                    .setParameter("type", "INVALID")
                    .setParameter("status", "PENDING")
                    .setParameter("createdBy", authorUserId)
                    .executeUpdate();
            entityManager.flush();
        }).isInstanceOfAny(DataIntegrityViolationException.class, jakarta.persistence.PersistenceException.class);
    }

    @Test
    void shouldAllowCreatedActivityIdOnlyForApprovedStatus() {
        CongressEntity congress = persistedCongress("Landivar");
        CallEntity call = callRepository.saveAndFlush(newOpenCall(congress.getId()));
        ActivityEntity activity = activityRepository.saveAndFlush(newActivity(congress.getId()));
        UUID authorUserId = UUID.randomUUID();

        ProposalEntity approved = newReviewedProposal(call.getId(), authorUserId, ProposalStatus.APPROVED);
        approved.setCreatedActivityId(activity.getId());
        ProposalEntity savedApproved = proposalRepository.saveAndFlush(approved);
        assertThat(savedApproved.getCreatedActivityId()).isEqualTo(activity.getId());

        ProposalEntity invalidPending = newPendingProposal(call.getId(), authorUserId);
        invalidPending.setCreatedActivityId(activity.getId());
        assertThatThrownBy(() -> proposalRepository.saveAndFlush(invalidPending))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private CallEntity persistedOpenCall(String institutionName) {
        CongressEntity congress = persistedCongress(institutionName);
        return callRepository.saveAndFlush(newOpenCall(congress.getId()));
    }

    private CongressEntity persistedCongress(String institutionName) {
        InstitutionEntity institution = new InstitutionEntity();
        institution.setName(institutionName);
        institution.setDescription("Institution " + institutionName);
        institution.setContactEmail(institutionName.toLowerCase().replace(" ", "") + "@example.com");
        institution.setActive(true);
        institution.setCreatedBy(UUID.randomUUID());
        institutionRepository.saveAndFlush(institution);

        CongressEntity congress = new CongressEntity();
        congress.setInstitutionId(institution.getId());
        congress.setName("Congreso " + institutionName);
        congress.setDescription("Descripcion " + institutionName);
        congress.setStartDate(LocalDate.of(2026, 10, 10));
        congress.setEndDate(LocalDate.of(2026, 10, 12));
        congress.setLocation("Guatemala");
        congress.setPrice(new BigDecimal("45.00"));
        congress.setCreatedBy(UUID.randomUUID());
        return congressRepository.saveAndFlush(congress);
    }

    private CallEntity newOpenCall(UUID congressId) {
        CallEntity call = new CallEntity();
        call.setCongressId(congressId);
        call.setStatus(CallStatus.OPEN);
        call.setOpenedAt(OffsetDateTime.parse("2026-10-10T09:00:00Z"));
        call.setCreatedBy(UUID.randomUUID());
        return call;
    }

    private ActivityEntity newActivity(UUID congressId) {
        RoomEntity room = new RoomEntity();
        room.setCongressId(congressId);
        room.setName("Room A");
        room.setCapacity(100);
        room.setLocation("Main building");
        room.setCreatedBy(UUID.randomUUID());
        RoomEntity savedRoom = roomRepository.saveAndFlush(room);

        ActivityEntity activity = new ActivityEntity();
        activity.setCongressId(congressId);
        activity.setRoomId(savedRoom.getId());
        activity.setName("Reference Activity");
        activity.setDescription("Used to validate proposal created_activity constraint.");
        activity.setType(ActivityType.PONENCIA);
        activity.setStartTime(OffsetDateTime.parse("2026-10-11T10:00:00Z"));
        activity.setEndTime(OffsetDateTime.parse("2026-10-11T11:00:00Z"));
        activity.setWorkshopCapacity(null);
        activity.setCreatedBy(UUID.randomUUID());
        return activity;
    }

    private ProposalEntity newPendingProposal(UUID callId, UUID authorUserId) {
        ProposalEntity proposal = new ProposalEntity();
        proposal.setCallId(callId);
        proposal.setAuthorUserId(authorUserId);
        proposal.setTitle("Pending proposal");
        proposal.setDescription("Pending review proposal");
        proposal.setType(ProposalType.PONENCIA);
        proposal.setStatus(ProposalStatus.PENDING);
        proposal.setCreatedBy(authorUserId);
        return proposal;
    }

    private ProposalEntity newReviewedProposalWithoutReviewData(
            UUID callId,
            UUID authorUserId,
            ProposalStatus status
    ) {
        ProposalEntity proposal = new ProposalEntity();
        proposal.setCallId(callId);
        proposal.setAuthorUserId(authorUserId);
        proposal.setTitle(status + " without review fields");
        proposal.setDescription("Should violate proposal reviewed check.");
        proposal.setType(ProposalType.PONENCIA);
        proposal.setStatus(status);
        proposal.setCreatedBy(authorUserId);
        return proposal;
    }

    private ProposalEntity newReviewedProposal(UUID callId, UUID authorUserId, ProposalStatus status) {
        ProposalEntity proposal = new ProposalEntity();
        proposal.setCallId(callId);
        proposal.setAuthorUserId(authorUserId);
        proposal.setTitle(status + " with review fields");
        proposal.setDescription("Reviewed proposal.");
        proposal.setType(ProposalType.TALLER);
        proposal.setStatus(status);
        proposal.setReviewedBy(UUID.randomUUID());
        proposal.setReviewedAt(OffsetDateTime.parse("2026-10-11T15:00:00Z"));
        proposal.setCreatedBy(authorUserId);
        proposal.setUpdatedBy(proposal.getReviewedBy());
        return proposal;
    }
}
