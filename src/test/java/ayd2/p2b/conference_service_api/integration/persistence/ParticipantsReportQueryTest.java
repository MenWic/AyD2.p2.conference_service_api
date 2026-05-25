package ayd2.p2b.conference_service_api.integration.persistence;

import ayd2.p2b.conference_service_api.feature.activity.domain.model.ActivityLeaderType;
import ayd2.p2b.conference_service_api.feature.activity.domain.model.ActivityType;
import ayd2.p2b.conference_service_api.feature.activity.infrastructure.persistence.entity.ActivityEntity;
import ayd2.p2b.conference_service_api.feature.activity.infrastructure.persistence.entity.ActivityLeaderEntity;
import ayd2.p2b.conference_service_api.feature.activity.infrastructure.persistence.repository.ActivityLeaderRepository;
import ayd2.p2b.conference_service_api.feature.activity.infrastructure.persistence.repository.ActivityRepository;
import ayd2.p2b.conference_service_api.feature.call.domain.model.CallStatus;
import ayd2.p2b.conference_service_api.feature.call.infrastructure.persistence.entity.CallEntity;
import ayd2.p2b.conference_service_api.feature.call.infrastructure.persistence.repository.CallRepository;
import ayd2.p2b.conference_service_api.feature.congress.infrastructure.persistence.entity.CongressEntity;
import ayd2.p2b.conference_service_api.feature.congress.infrastructure.persistence.repository.CongressRepository;
import ayd2.p2b.conference_service_api.feature.enrollment.infrastructure.persistence.entity.EnrollmentEntity;
import ayd2.p2b.conference_service_api.feature.enrollment.infrastructure.persistence.repository.EnrollmentJpaRepository;
import ayd2.p2b.conference_service_api.feature.institution.infrastructure.persistence.entity.InstitutionEntity;
import ayd2.p2b.conference_service_api.feature.institution.infrastructure.persistence.repository.InstitutionRepository;
import ayd2.p2b.conference_service_api.feature.proposal.domain.model.ProposalStatus;
import ayd2.p2b.conference_service_api.feature.proposal.domain.model.ProposalType;
import ayd2.p2b.conference_service_api.feature.proposal.infrastructure.persistence.entity.ProposalEntity;
import ayd2.p2b.conference_service_api.feature.proposal.infrastructure.persistence.repository.ProposalRepository;
import ayd2.p2b.conference_service_api.feature.report.dto.internal.ParticipantRow;
import ayd2.p2b.conference_service_api.feature.report.dto.response.ParticipationTypeEnum;
import ayd2.p2b.conference_service_api.feature.room.infrastructure.persistence.entity.RoomEntity;
import ayd2.p2b.conference_service_api.feature.room.infrastructure.persistence.repository.RoomRepository;
import ayd2.p2b.conference_service_api.feature.report.infrastructure.persistence.query.JpaParticipantsReportQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaParticipantsReportQuery.class)
class ParticipantsReportQueryTest {

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
    private JpaParticipantsReportQuery query;

    @Autowired
    private InstitutionRepository institutionRepository;
    @Autowired
    private CongressRepository congressRepository;
    @Autowired
    private RoomRepository roomRepository;
    @Autowired
    private ActivityRepository activityRepository;
    @Autowired
    private ActivityLeaderRepository activityLeaderRepository;
    @Autowired
    private CallRepository callRepository;
    @Autowired
    private ProposalRepository proposalRepository;
    @Autowired
    private EnrollmentJpaRepository enrollmentRepository;

    private UUID congressId;
    private UUID activityId;
    private UUID callId;

    @BeforeEach
    void setUp() {
        InstitutionEntity inst = new InstitutionEntity();
        inst.setName("Participants Test Inst " + UUID.randomUUID());
        inst.setDescription("Desc");
        inst.setContactEmail("test@inst.com");
        inst.setActive(true);
        inst.setCreatedBy(UUID.randomUUID());
        inst = institutionRepository.saveAndFlush(inst);

        CongressEntity congress = new CongressEntity();
        congress.setInstitutionId(inst.getId());
        congress.setName("Participants Congress");
        congress.setDescription("Description");
        congress.setStartDate(LocalDate.of(2026, 6, 1));
        congress.setEndDate(LocalDate.of(2026, 6, 5));
        congress.setLocation("Guatemala");
        congress.setPrice(new BigDecimal("100.00"));
        congress.setCreatedBy(UUID.randomUUID());
        congress = congressRepository.saveAndFlush(congress);
        congressId = congress.getId();

        RoomEntity room = new RoomEntity();
        room.setCongressId(congressId);
        room.setName("Room A");
        room.setCreatedBy(UUID.randomUUID());
        room = roomRepository.saveAndFlush(room);

        OffsetDateTime start = OffsetDateTime.of(2026, 6, 1, 9, 0, 0, 0, ZoneOffset.UTC);
        ActivityEntity activity = new ActivityEntity();
        activity.setCongressId(congressId);
        activity.setRoomId(room.getId());
        activity.setName("Keynote");
        activity.setDescription("Opening");
        activity.setType(ActivityType.PONENCIA);
        activity.setStartTime(start);
        activity.setEndTime(start.plusHours(2));
        activity.setCreatedBy(UUID.randomUUID());
        activity = activityRepository.saveAndFlush(activity);
        activityId = activity.getId();

        CallEntity call = new CallEntity();
        call.setCongressId(congressId);
        call.setStatus(CallStatus.OPEN);
        call.setCreatedBy(UUID.randomUUID());
        call = callRepository.saveAndFlush(call);
        callId = call.getId();
    }

    @Test
    void no_participants_returns_empty_list() {
        List<ParticipantRow> result = query.findParticipants(congressId);

        assertThat(result).isEmpty();
    }

    @Test
    void enrolled_user_appears_with_enrolled_type() {
        UUID userId = UUID.randomUUID();
        persistEnrollment(userId, congressId);

        List<ParticipantRow> result = query.findParticipants(congressId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo(userId);
        assertThat(result.get(0).getParticipationTypes()).contains(ParticipationTypeEnum.ENROLLED);
    }

    @Test
    void approved_proposal_author_appears_with_proposal_author_type() {
        UUID authorId = UUID.randomUUID();
        UUID reviewerId = UUID.randomUUID();
        persistApprovedProposal(authorId, callId, reviewerId);

        List<ParticipantRow> result = query.findParticipants(congressId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo(authorId);
        assertThat(result.get(0).getParticipationTypes()).contains(ParticipationTypeEnum.PROPOSAL_AUTHOR);
    }

    @Test
    void pending_proposal_author_is_not_included() {
        UUID authorId = UUID.randomUUID();
        persistPendingProposal(authorId, callId);

        List<ParticipantRow> result = query.findParticipants(congressId);

        assertThat(result).isEmpty();
    }

    @Test
    void activity_speaker_appears_with_speaker_type() {
        UUID speakerId = UUID.randomUUID();
        persistActivityLeader(activityId, speakerId, ActivityLeaderType.SPEAKER);

        List<ParticipantRow> result = query.findParticipants(congressId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo(speakerId);
        assertThat(result.get(0).getParticipationTypes()).contains(ParticipationTypeEnum.SPEAKER);
    }

    @Test
    void activity_workshop_leader_appears_with_workshop_leader_type() {
        UUID leaderId = UUID.randomUUID();
        persistActivityLeader(activityId, leaderId, ActivityLeaderType.WORKSHOP_LEADER);

        List<ParticipantRow> result = query.findParticipants(congressId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getParticipationTypes()).contains(ParticipationTypeEnum.WORKSHOP_LEADER);
    }

    @Test
    void activity_guest_speaker_appears_with_guest_speaker_type() {
        UUID guestId = UUID.randomUUID();
        persistActivityLeader(activityId, guestId, ActivityLeaderType.GUEST_SPEAKER);

        List<ParticipantRow> result = query.findParticipants(congressId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getParticipationTypes()).contains(ParticipationTypeEnum.GUEST_SPEAKER);
    }

    @Test
    void same_user_enrolled_and_speaker_has_both_types_merged() {
        UUID userId = UUID.randomUUID();
        persistEnrollment(userId, congressId);
        persistActivityLeader(activityId, userId, ActivityLeaderType.SPEAKER);

        List<ParticipantRow> result = query.findParticipants(congressId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getParticipationTypes())
                .containsExactlyInAnyOrder(ParticipationTypeEnum.ENROLLED, ParticipationTypeEnum.SPEAKER);
    }

    @Test
    void different_congress_is_not_included() {
        UUID otherCongressId = createAdditionalCongress();
        UUID userId = UUID.randomUUID();
        persistEnrollment(userId, otherCongressId);

        List<ParticipantRow> result = query.findParticipants(congressId);

        assertThat(result).isEmpty();
    }

    @Test
    void leaders_from_other_congress_are_excluded() {
        UUID speakerInTargetCongress = UUID.randomUUID();
        UUID speakerInOtherCongress = UUID.randomUUID();

        persistActivityLeader(activityId, speakerInTargetCongress, ActivityLeaderType.SPEAKER);

        UUID otherCongressId = createAdditionalCongress();
        UUID otherRoomId = createRoom(otherCongressId, "Other Room");
        UUID otherActivityId = createActivity(otherCongressId, otherRoomId, "Other Talk", ActivityType.PONENCIA, null);
        persistActivityLeader(otherActivityId, speakerInOtherCongress, ActivityLeaderType.SPEAKER);

        List<ParticipantRow> result = query.findParticipants(congressId);

        assertThat(result).extracting(ParticipantRow::getUserId).contains(speakerInTargetCongress);
        assertThat(result).extracting(ParticipantRow::getUserId).doesNotContain(speakerInOtherCongress);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private UUID createAdditionalCongress() {
        InstitutionEntity inst2 = new InstitutionEntity();
        inst2.setName("Other Inst " + UUID.randomUUID());
        inst2.setDescription("Desc");
        inst2.setContactEmail("other@inst.com");
        inst2.setActive(true);
        inst2.setCreatedBy(UUID.randomUUID());
        inst2 = institutionRepository.saveAndFlush(inst2);

        CongressEntity other = new CongressEntity();
        other.setInstitutionId(inst2.getId());
        other.setName("Other Congress " + UUID.randomUUID());
        other.setDescription("Description");
        other.setStartDate(LocalDate.of(2026, 7, 1));
        other.setEndDate(LocalDate.of(2026, 7, 5));
        other.setLocation("Xela");
        other.setPrice(new BigDecimal("50.00"));
        other.setCreatedBy(UUID.randomUUID());
        other = congressRepository.saveAndFlush(other);
        return other.getId();
    }

    private UUID createRoom(UUID targetCongressId, String name) {
        RoomEntity room = new RoomEntity();
        room.setCongressId(targetCongressId);
        room.setName(name);
        room.setCreatedBy(UUID.randomUUID());
        room = roomRepository.saveAndFlush(room);
        return room.getId();
    }

    private UUID createActivity(
            UUID targetCongressId,
            UUID roomId,
            String name,
            ActivityType type,
            Integer workshopCapacity
    ) {
        OffsetDateTime start = OffsetDateTime.of(2026, 6, 2, 9, 0, 0, 0, ZoneOffset.UTC);
        ActivityEntity activity = new ActivityEntity();
        activity.setCongressId(targetCongressId);
        activity.setRoomId(roomId);
        activity.setName(name);
        activity.setDescription("Description");
        activity.setType(type);
        activity.setStartTime(start);
        activity.setEndTime(start.plusHours(1));
        activity.setWorkshopCapacity(workshopCapacity);
        activity.setCreatedBy(UUID.randomUUID());
        activity = activityRepository.saveAndFlush(activity);
        return activity.getId();
    }

    private void persistEnrollment(UUID userId, UUID targetCongressId) {
        EnrollmentEntity e = new EnrollmentEntity();
        e.setCongressId(targetCongressId);
        e.setUserId(userId);
        e.setPaymentId(UUID.randomUUID());
        e.setPaymentDate(LocalDate.of(2026, 5, 1));
        e.setCreatedBy(UUID.randomUUID());
        enrollmentRepository.saveAndFlush(e);
    }

    private void persistApprovedProposal(UUID authorId, UUID targetCallId, UUID reviewerId) {
        ProposalEntity p = new ProposalEntity();
        p.setCallId(targetCallId);
        p.setAuthorUserId(authorId);
        p.setTitle("Test Proposal");
        p.setDescription("Description");
        p.setType(ProposalType.PONENCIA);
        p.setStatus(ProposalStatus.APPROVED);
        p.setReviewedBy(reviewerId);
        p.setReviewedAt(OffsetDateTime.now());
        p.setCreatedBy(authorId);
        proposalRepository.saveAndFlush(p);
    }

    private void persistPendingProposal(UUID authorId, UUID targetCallId) {
        ProposalEntity p = new ProposalEntity();
        p.setCallId(targetCallId);
        p.setAuthorUserId(authorId);
        p.setTitle("Pending Proposal");
        p.setDescription("Description");
        p.setType(ProposalType.PONENCIA);
        p.setStatus(ProposalStatus.PENDING);
        p.setCreatedBy(authorId);
        proposalRepository.saveAndFlush(p);
    }

    private void persistActivityLeader(UUID targetActivityId, UUID userId, ActivityLeaderType leaderType) {
        ActivityLeaderEntity leader = new ActivityLeaderEntity();
        leader.setActivityId(targetActivityId);
        leader.setUserId(userId);
        leader.setLeaderType(leaderType);
        activityLeaderRepository.saveAndFlush(leader);
    }
}
