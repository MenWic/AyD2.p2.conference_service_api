package ayd2.p2b.conference_service_api.integration.persistence;

import ayd2.p2b.conference_service_api.feature.activity.domain.model.ActivityType;
import ayd2.p2b.conference_service_api.feature.activity.domain.model.ActivityLeaderType;
import ayd2.p2b.conference_service_api.feature.activity.infrastructure.persistence.entity.ActivityEntity;
import ayd2.p2b.conference_service_api.feature.activity.infrastructure.persistence.entity.ActivityLeaderEntity;
import ayd2.p2b.conference_service_api.feature.activity.infrastructure.persistence.repository.ActivityRepository;
import ayd2.p2b.conference_service_api.feature.activity.infrastructure.persistence.repository.ActivityLeaderRepository;
import ayd2.p2b.conference_service_api.feature.congress.infrastructure.persistence.entity.CongressEntity;
import ayd2.p2b.conference_service_api.feature.congress.infrastructure.persistence.repository.CongressRepository;
import ayd2.p2b.conference_service_api.feature.institution.infrastructure.persistence.entity.InstitutionEntity;
import ayd2.p2b.conference_service_api.feature.institution.infrastructure.persistence.repository.InstitutionRepository;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ActivityPersistenceTest {

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
    private ActivityRepository activityRepository;
    @Autowired
    private ActivityLeaderRepository activityLeaderRepository;
    @Autowired
    private RoomRepository roomRepository;
    @Autowired
    private CongressRepository congressRepository;
    @Autowired
    private InstitutionRepository institutionRepository;
    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldRejectInvalidTimeRangeByCheckConstraint() {
        RoomEntity room = persistedRoom("USAC");
        ActivityEntity activity = baseActivity(room);
        activity.setStartTime(OffsetDateTime.parse("2026-10-10T11:00:00Z"));
        activity.setEndTime(OffsetDateTime.parse("2026-10-10T10:00:00Z"));

        assertThatThrownBy(() -> activityRepository.saveAndFlush(activity))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectWorkshopCapacityByTypeConstraint() {
        RoomEntity room = persistedRoom("URL");
        ActivityEntity tallerWithoutCapacity = baseActivity(room);
        tallerWithoutCapacity.setType(ActivityType.TALLER);
        tallerWithoutCapacity.setWorkshopCapacity(null);

        assertThatThrownBy(() -> activityRepository.saveAndFlush(tallerWithoutCapacity))
                .isInstanceOf(DataIntegrityViolationException.class);

        ActivityEntity ponenciaWithCapacity = baseActivity(room);
        ponenciaWithCapacity.setType(ActivityType.PONENCIA);
        ponenciaWithCapacity.setWorkshopCapacity(10);

        assertThatThrownBy(() -> activityRepository.saveAndFlush(ponenciaWithCapacity))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldEnforceRoomForeignKeyRestrictOnDelete() {
        RoomEntity room = persistedRoom("Del Valle");
        activityRepository.saveAndFlush(baseActivity(room));

        assertThatThrownBy(() -> {
            roomRepository.deleteById(room.getId());
            roomRepository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldEnforceCongressForeignKeyRestrictOnDelete() {
        RoomEntity room = persistedRoom("Mariano");
        activityRepository.saveAndFlush(baseActivity(room));

        assertThatThrownBy(() -> {
            congressRepository.deleteById(room.getCongressId());
            congressRepository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectOverlapInSameRoomByExclusionConstraint() {
        RoomEntity room = persistedRoom("Galileo");

        ActivityEntity first = baseActivity(room);
        first.setStartTime(OffsetDateTime.parse("2026-10-10T10:00:00Z"));
        first.setEndTime(OffsetDateTime.parse("2026-10-10T11:00:00Z"));
        activityRepository.saveAndFlush(first);

        ActivityEntity overlap = baseActivity(room);
        overlap.setStartTime(OffsetDateTime.parse("2026-10-10T10:30:00Z"));
        overlap.setEndTime(OffsetDateTime.parse("2026-10-10T11:30:00Z"));

        assertThatThrownBy(() -> activityRepository.saveAndFlush(overlap))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldPersistAndReadLeadersForActivity() {
        ActivityEntity activity = activityRepository.saveAndFlush(baseActivity(persistedRoom("Landivar")));
        UUID leaderId = UUID.randomUUID();

        ActivityLeaderEntity leader = new ActivityLeaderEntity();
        leader.setActivityId(activity.getId());
        leader.setUserId(leaderId);
        leader.setLeaderType(ActivityLeaderType.WORKSHOP_LEADER);
        activityLeaderRepository.saveAndFlush(leader);

        List<ActivityLeaderEntity> leaders = activityLeaderRepository.findByActivityIdOrderByUserIdAsc(activity.getId());
        assertThat(leaders).hasSize(1);
        assertThat(leaders.getFirst().getUserId()).isEqualTo(leaderId);
    }

    @Test
    void shouldRejectDuplicateLeaderByPrimaryKey() {
        ActivityEntity activity = activityRepository.saveAndFlush(baseActivity(persistedRoom("Meso")));
        UUID leaderId = UUID.randomUUID();

        ActivityLeaderEntity first = new ActivityLeaderEntity();
        first.setActivityId(activity.getId());
        first.setUserId(leaderId);
        first.setLeaderType(ActivityLeaderType.SPEAKER);
        activityLeaderRepository.saveAndFlush(first);

        assertThatThrownBy(() -> {
            entityManager.createNativeQuery("""
                            insert into activity_leaders (activity_id, user_id, leader_type)
                            values (:activityId, :userId, :leaderType)
                            """)
                    .setParameter("activityId", activity.getId())
                    .setParameter("userId", leaderId)
                    .setParameter("leaderType", "GUEST_SPEAKER")
                    .executeUpdate();
            entityManager.flush();
        }).isInstanceOfAny(DataIntegrityViolationException.class, jakarta.persistence.PersistenceException.class);
    }

    @Test
    void shouldReplaceLeaderAssignments() {
        ActivityEntity activity = activityRepository.saveAndFlush(baseActivity(persistedRoom("Itzamna")));
        UUID oldLeaderId = UUID.randomUUID();
        UUID newLeaderId = UUID.randomUUID();

        ActivityLeaderEntity oldLeader = new ActivityLeaderEntity();
        oldLeader.setActivityId(activity.getId());
        oldLeader.setUserId(oldLeaderId);
        oldLeader.setLeaderType(ActivityLeaderType.SPEAKER);
        activityLeaderRepository.saveAndFlush(oldLeader);

        activityLeaderRepository.deleteByActivityId(activity.getId());

        ActivityLeaderEntity newLeader = new ActivityLeaderEntity();
        newLeader.setActivityId(activity.getId());
        newLeader.setUserId(newLeaderId);
        newLeader.setLeaderType(ActivityLeaderType.GUEST_SPEAKER);
        activityLeaderRepository.saveAndFlush(newLeader);

        List<ActivityLeaderEntity> leaders = activityLeaderRepository.findByActivityIdOrderByUserIdAsc(activity.getId());
        assertThat(leaders).hasSize(1);
        assertThat(leaders.getFirst().getUserId()).isEqualTo(newLeaderId);
    }

    @Test
    void shouldCascadeDeleteLeadersWhenActivityIsDeleted() {
        ActivityEntity activity = activityRepository.saveAndFlush(baseActivity(persistedRoom("Del Istmo")));

        ActivityLeaderEntity leader = new ActivityLeaderEntity();
        leader.setActivityId(activity.getId());
        leader.setUserId(UUID.randomUUID());
        leader.setLeaderType(ActivityLeaderType.SPEAKER);
        activityLeaderRepository.saveAndFlush(leader);

        activityRepository.deleteById(activity.getId());
        activityRepository.flush();

        assertThat(activityLeaderRepository.findByActivityIdOrderByUserIdAsc(activity.getId())).isEmpty();
    }

    private RoomEntity persistedRoom(String institutionName) {
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
        congress.setDescription("Descripcion");
        congress.setStartDate(LocalDate.of(2026, 10, 10));
        congress.setEndDate(LocalDate.of(2026, 10, 12));
        congress.setLocation("Guatemala");
        congress.setPrice(new BigDecimal("45.00"));
        congress.setCreatedBy(UUID.randomUUID());
        congressRepository.saveAndFlush(congress);

        RoomEntity room = new RoomEntity();
        room.setCongressId(congress.getId());
        room.setName("Sala " + institutionName);
        room.setCapacity(100);
        room.setLocation("Edificio A");
        room.setCreatedBy(UUID.randomUUID());
        return roomRepository.saveAndFlush(room);
    }

    private ActivityEntity baseActivity(RoomEntity room) {
        ActivityEntity activity = new ActivityEntity();
        activity.setCongressId(room.getCongressId());
        activity.setRoomId(room.getId());
        activity.setName("Actividad");
        activity.setDescription("Descripcion");
        activity.setType(ActivityType.PONENCIA);
        activity.setStartTime(OffsetDateTime.parse("2026-10-10T10:00:00Z"));
        activity.setEndTime(OffsetDateTime.parse("2026-10-10T11:00:00Z"));
        activity.setWorkshopCapacity(null);
        activity.setCreatedBy(UUID.randomUUID());
        return activity;
    }
}
