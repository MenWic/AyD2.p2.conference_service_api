package ayd2.p2b.conference_service_api.integration.persistence;

import ayd2.p2b.conference_service_api.feature.activity.domain.model.ActivityType;
import ayd2.p2b.conference_service_api.feature.activity.infrastructure.persistence.entity.ActivityEntity;
import ayd2.p2b.conference_service_api.feature.activity.infrastructure.persistence.repository.ActivityRepository;
import ayd2.p2b.conference_service_api.feature.congress.infrastructure.persistence.entity.CongressEntity;
import ayd2.p2b.conference_service_api.feature.congress.infrastructure.persistence.repository.CongressRepository;
import ayd2.p2b.conference_service_api.feature.institution.infrastructure.persistence.entity.InstitutionEntity;
import ayd2.p2b.conference_service_api.feature.institution.infrastructure.persistence.repository.InstitutionRepository;
import ayd2.p2b.conference_service_api.feature.reservation.infrastructure.persistence.entity.ReservationEntity;
import ayd2.p2b.conference_service_api.feature.reservation.infrastructure.persistence.repository.ReservationRepository;
import ayd2.p2b.conference_service_api.feature.room.infrastructure.persistence.entity.RoomEntity;
import ayd2.p2b.conference_service_api.feature.room.infrastructure.persistence.repository.RoomRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
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
class ReservationPersistenceTest {

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
    private ReservationRepository reservationRepository;
    @Autowired
    private ActivityRepository activityRepository;
    @Autowired
    private RoomRepository roomRepository;
    @Autowired
    private CongressRepository congressRepository;
    @Autowired
    private InstitutionRepository institutionRepository;

    @Test
    void shouldEnforceUniqueReservationByActivityAndUser() {
        ActivityEntity activity = persistedWorkshopActivity();
        UUID userId = UUID.randomUUID();

        reservationRepository.saveAndFlush(newReservation(activity.getId(), userId));

        assertThatThrownBy(() -> reservationRepository.saveAndFlush(newReservation(activity.getId(), userId)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldSaveAndListReservationsByActivityAndUser() {
        ActivityEntity activity = persistedWorkshopActivity();
        UUID userId = UUID.randomUUID();
        reservationRepository.saveAndFlush(newReservation(activity.getId(), userId));

        assertThat(reservationRepository.findByActivityId(activity.getId(), PageRequest.of(0, 20)).getTotalElements())
                .isEqualTo(1);
        assertThat(reservationRepository.findByUserId(userId, PageRequest.of(0, 20)).getTotalElements())
                .isEqualTo(1);
    }

    private ActivityEntity persistedWorkshopActivity() {
        UUID adminId = UUID.randomUUID();

        InstitutionEntity institution = new InstitutionEntity();
        institution.setName("Reservation Institution");
        institution.setDescription("Reservation Institution Description");
        institution.setContactEmail("reservation@example.com");
        institution.setActive(true);
        institution.setCreatedBy(adminId);
        institutionRepository.saveAndFlush(institution);

        CongressEntity congress = new CongressEntity();
        congress.setInstitutionId(institution.getId());
        congress.setName("Reservation Congress");
        congress.setDescription("Reservation Congress Description");
        congress.setStartDate(LocalDate.of(2026, 10, 10));
        congress.setEndDate(LocalDate.of(2026, 10, 12));
        congress.setLocation("Guatemala");
        congress.setPrice(new BigDecimal("45.00"));
        congress.setCreatedBy(adminId);
        congressRepository.saveAndFlush(congress);

        RoomEntity room = new RoomEntity();
        room.setCongressId(congress.getId());
        room.setName("Workshop Room");
        room.setCapacity(100);
        room.setLocation("Building A");
        room.setCreatedBy(adminId);
        roomRepository.saveAndFlush(room);

        ActivityEntity activity = new ActivityEntity();
        activity.setCongressId(congress.getId());
        activity.setRoomId(room.getId());
        activity.setName("Workshop Activity");
        activity.setDescription("Workshop Activity Description");
        activity.setType(ActivityType.TALLER);
        activity.setWorkshopCapacity(20);
        activity.setStartTime(OffsetDateTime.parse("2026-10-10T10:00:00Z"));
        activity.setEndTime(OffsetDateTime.parse("2026-10-10T11:00:00Z"));
        activity.setCreatedBy(adminId);
        return activityRepository.saveAndFlush(activity);
    }

    private ReservationEntity newReservation(UUID activityId, UUID userId) {
        ReservationEntity reservation = new ReservationEntity();
        reservation.setActivityId(activityId);
        reservation.setUserId(userId);
        reservation.setCreatedBy(userId);
        return reservation;
    }
}
