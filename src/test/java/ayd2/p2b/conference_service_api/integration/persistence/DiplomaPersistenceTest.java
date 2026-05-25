package ayd2.p2b.conference_service_api.integration.persistence;

import ayd2.p2b.conference_service_api.feature.activity.domain.model.ActivityType;
import ayd2.p2b.conference_service_api.feature.activity.infrastructure.persistence.entity.ActivityEntity;
import ayd2.p2b.conference_service_api.feature.activity.infrastructure.persistence.repository.ActivityRepository;
import ayd2.p2b.conference_service_api.feature.congress.infrastructure.persistence.entity.CongressEntity;
import ayd2.p2b.conference_service_api.feature.congress.infrastructure.persistence.repository.CongressRepository;
import ayd2.p2b.conference_service_api.feature.diploma.domain.model.DiplomaType;
import ayd2.p2b.conference_service_api.feature.diploma.infrastructure.persistence.entity.DiplomaEntity;
import ayd2.p2b.conference_service_api.feature.diploma.infrastructure.persistence.repository.DiplomaRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DiplomaPersistenceTest {

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
    private DiplomaRepository diplomaRepository;

    @Autowired
    private InstitutionRepository institutionRepository;

    @Autowired
    private CongressRepository congressRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private ActivityRepository activityRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void shouldEnforceParticipationDuplicateProtection() {
        ActivityContext context = persistedActivityContext();
        UUID userId = UUID.randomUUID();

        diplomaRepository.saveAndFlush(newParticipationDiploma(userId, context.congressId()));

        assertThatThrownBy(() -> diplomaRepository.saveAndFlush(newParticipationDiploma(userId, context.congressId())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldEnforceLeadershipDuplicateProtection() {
        ActivityContext context = persistedActivityContext();
        UUID userId = UUID.randomUUID();

        diplomaRepository.saveAndFlush(newLeadershipDiploma(userId, context.congressId(), context.activityId()));

        assertThatThrownBy(() -> diplomaRepository.saveAndFlush(
                newLeadershipDiploma(userId, context.congressId(), context.activityId())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectParticipationWithNonNullActivityId() {
        ActivityContext context = persistedActivityContext();
        DiplomaEntity invalid = baseDiploma(UUID.randomUUID(), context.congressId(), DiplomaType.PARTICIPATION);
        invalid.setActivityId(context.activityId());

        assertThatThrownBy(() -> diplomaRepository.saveAndFlush(invalid))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRejectLeadershipWithNullActivityId() {
        ActivityContext context = persistedActivityContext();
        DiplomaEntity invalid = baseDiploma(UUID.randomUUID(), context.congressId(), DiplomaType.LEADERSHIP);
        invalid.setActivityId(null);

        assertThatThrownBy(() -> diplomaRepository.saveAndFlush(invalid))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldStoreDiplomaTypeAsVarchar() {
        String dataType = (String) entityManager.createNativeQuery("""
                        SELECT data_type
                        FROM information_schema.columns
                        WHERE table_name = 'diplomas' AND column_name = 'type'
                        """)
                .getSingleResult();

        assertThat(dataType).isEqualTo("character varying");
    }

    @Test
    void shouldRestrictDeletingCongressWhenParticipationDiplomaExists() {
        UUID congressId = persistedCongressOnly();
        diplomaRepository.saveAndFlush(newParticipationDiploma(UUID.randomUUID(), congressId));

        assertThatThrownBy(() -> {
            congressRepository.deleteById(congressId);
            congressRepository.flush();
        })
                .isInstanceOf(DataIntegrityViolationException.class)
                .satisfies(error -> assertThat(rootCauseMessage(error)).contains("fk_diploma_congress"));
    }

    @Test
    void shouldRestrictDeletingActivityWhenLeadershipDiplomaExists() {
        ActivityContext context = persistedActivityContext();
        diplomaRepository.saveAndFlush(newLeadershipDiploma(
                UUID.randomUUID(),
                context.congressId(),
                context.activityId()
        ));

        assertThatThrownBy(() -> {
            activityRepository.deleteById(context.activityId());
            activityRepository.flush();
        })
                .isInstanceOf(DataIntegrityViolationException.class)
                .satisfies(error -> assertThat(rootCauseMessage(error)).contains("fk_diploma_activity"));
    }

    private UUID persistedCongressOnly() {
        UUID adminId = UUID.randomUUID();

        InstitutionEntity institution = new InstitutionEntity();
        institution.setName("Diploma Institution " + UUID.randomUUID());
        institution.setDescription("Institution for diploma congress FK test");
        institution.setContactEmail(UUID.randomUUID() + "@example.com");
        institution.setActive(true);
        institution.setCreatedBy(adminId);
        institutionRepository.saveAndFlush(institution);

        CongressEntity congress = new CongressEntity();
        congress.setInstitutionId(institution.getId());
        congress.setName("Diploma Congress " + UUID.randomUUID());
        congress.setDescription("Congress for diploma FK test");
        congress.setStartDate(LocalDate.of(2026, 11, 1));
        congress.setEndDate(LocalDate.of(2026, 11, 2));
        congress.setLocation("Guatemala");
        congress.setPrice(new BigDecimal("45.00"));
        congress.setCreatedBy(adminId);
        congressRepository.saveAndFlush(congress);

        return congress.getId();
    }

    private DiplomaEntity newParticipationDiploma(UUID userId, UUID congressId) {
        DiplomaEntity diploma = baseDiploma(userId, congressId, DiplomaType.PARTICIPATION);
        diploma.setActivityId(null);
        return diploma;
    }

    private DiplomaEntity newLeadershipDiploma(UUID userId, UUID congressId, UUID activityId) {
        DiplomaEntity diploma = baseDiploma(userId, congressId, DiplomaType.LEADERSHIP);
        diploma.setActivityId(activityId);
        return diploma;
    }

    private DiplomaEntity baseDiploma(UUID userId, UUID congressId, DiplomaType type) {
        DiplomaEntity diploma = new DiplomaEntity();
        diploma.setUserId(userId);
        diploma.setCongressId(congressId);
        diploma.setType(type);
        diploma.setCreatedBy(UUID.randomUUID());
        diploma.setIssuedAt(OffsetDateTime.parse("2026-11-01T10:00:00Z"));
        diploma.setCreatedAt(OffsetDateTime.parse("2026-11-01T10:00:00Z"));
        return diploma;
    }

    private ActivityContext persistedActivityContext() {
        UUID adminId = UUID.randomUUID();

        InstitutionEntity institution = new InstitutionEntity();
        institution.setName("Diploma Institution " + UUID.randomUUID());
        institution.setDescription("Institution for diploma persistence tests");
        institution.setContactEmail(UUID.randomUUID() + "@example.com");
        institution.setActive(true);
        institution.setCreatedBy(adminId);
        institutionRepository.saveAndFlush(institution);

        CongressEntity congress = new CongressEntity();
        congress.setInstitutionId(institution.getId());
        congress.setName("Diploma Congress " + UUID.randomUUID());
        congress.setDescription("Congress for diploma persistence tests");
        congress.setStartDate(LocalDate.of(2026, 11, 1));
        congress.setEndDate(LocalDate.of(2026, 11, 2));
        congress.setLocation("Guatemala");
        congress.setPrice(new BigDecimal("45.00"));
        congress.setCreatedBy(adminId);
        congressRepository.saveAndFlush(congress);

        RoomEntity room = new RoomEntity();
        room.setCongressId(congress.getId());
        room.setName("Diploma Room " + UUID.randomUUID());
        room.setCapacity(120);
        room.setLocation("Building A");
        room.setCreatedBy(adminId);
        roomRepository.saveAndFlush(room);

        ActivityEntity activity = new ActivityEntity();
        activity.setCongressId(congress.getId());
        activity.setRoomId(room.getId());
        activity.setName("Diploma Activity " + UUID.randomUUID());
        activity.setDescription("Activity for diploma persistence tests");
        activity.setType(ActivityType.PONENCIA);
        activity.setWorkshopCapacity(null);
        activity.setStartTime(OffsetDateTime.parse("2026-11-01T10:00:00Z"));
        activity.setEndTime(OffsetDateTime.parse("2026-11-01T11:00:00Z"));
        activity.setCreatedBy(adminId);
        activityRepository.saveAndFlush(activity);

        return new ActivityContext(congress.getId(), activity.getId());
    }

    private String rootCauseMessage(Throwable throwable) {
        Throwable root = throwable;

        while (root.getCause() != null) {
            root = root.getCause();
        }

        return root.getMessage();
    }

    private record ActivityContext(UUID congressId, UUID activityId) {
    }
}