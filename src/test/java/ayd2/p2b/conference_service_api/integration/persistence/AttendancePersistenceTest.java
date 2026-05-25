package ayd2.p2b.conference_service_api.integration.persistence;

import ayd2.p2b.conference_service_api.feature.activity.domain.model.ActivityType;
import ayd2.p2b.conference_service_api.feature.activity.infrastructure.persistence.entity.ActivityEntity;
import ayd2.p2b.conference_service_api.feature.activity.infrastructure.persistence.repository.ActivityRepository;
import ayd2.p2b.conference_service_api.feature.attendance.infrastructure.persistence.entity.AttendanceEntity;
import ayd2.p2b.conference_service_api.feature.attendance.infrastructure.persistence.repository.AttendanceRepository;
import ayd2.p2b.conference_service_api.feature.congress.infrastructure.persistence.entity.CongressEntity;
import ayd2.p2b.conference_service_api.feature.congress.infrastructure.persistence.repository.CongressRepository;
import ayd2.p2b.conference_service_api.feature.institution.infrastructure.persistence.entity.InstitutionEntity;
import ayd2.p2b.conference_service_api.feature.institution.infrastructure.persistence.repository.InstitutionRepository;
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
class AttendancePersistenceTest {

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
    private AttendanceRepository attendanceRepository;
    @Autowired
    private ActivityRepository activityRepository;
    @Autowired
    private RoomRepository roomRepository;
    @Autowired
    private CongressRepository congressRepository;
    @Autowired
    private InstitutionRepository institutionRepository;

    @Test
    void shouldEnforceUniqueAttendanceByActivityAndUser() {
        ActivityEntity activity = persistedActivity();
        UUID userId = UUID.randomUUID();

        attendanceRepository.saveAndFlush(newAttendance(activity.getId(), userId, "PID123"));

        assertThatThrownBy(() -> attendanceRepository.saveAndFlush(newAttendance(activity.getId(), userId, "PID123")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldSaveAndFindAttendanceWithPersonalIdSnapshot() {
        ActivityEntity activity = persistedActivity();
        UUID userId = UUID.randomUUID();
        AttendanceEntity saved = attendanceRepository.saveAndFlush(newAttendance(activity.getId(), userId, "PID321"));

        assertThat(attendanceRepository.existsByActivityIdAndUserId(activity.getId(), userId)).isTrue();
        assertThat(attendanceRepository.findAll(PageRequest.of(0, 20)).getContent().getFirst().getPersonalIdSnapshot())
                .isEqualTo("PID321");
        assertThat(saved.getRegisteredBy()).isNotNull();
        assertThat(saved.getRegisteredAt()).isNotNull();
    }

    private ActivityEntity persistedActivity() {
        UUID adminId = UUID.randomUUID();

        InstitutionEntity institution = new InstitutionEntity();
        institution.setName("Attendance Institution");
        institution.setDescription("Attendance Institution Description");
        institution.setContactEmail("attendance@example.com");
        institution.setActive(true);
        institution.setCreatedBy(adminId);
        institutionRepository.saveAndFlush(institution);

        CongressEntity congress = new CongressEntity();
        congress.setInstitutionId(institution.getId());
        congress.setName("Attendance Congress");
        congress.setDescription("Attendance Congress Description");
        congress.setStartDate(LocalDate.of(2026, 10, 10));
        congress.setEndDate(LocalDate.of(2026, 10, 12));
        congress.setLocation("Guatemala");
        congress.setPrice(new BigDecimal("45.00"));
        congress.setCreatedBy(adminId);
        congressRepository.saveAndFlush(congress);

        RoomEntity room = new RoomEntity();
        room.setCongressId(congress.getId());
        room.setName("Attendance Room");
        room.setCapacity(120);
        room.setLocation("Building B");
        room.setCreatedBy(adminId);
        roomRepository.saveAndFlush(room);

        ActivityEntity activity = new ActivityEntity();
        activity.setCongressId(congress.getId());
        activity.setRoomId(room.getId());
        activity.setName("Ponencia Activity");
        activity.setDescription("Ponencia Activity Description");
        activity.setType(ActivityType.PONENCIA);
        activity.setWorkshopCapacity(null);
        activity.setStartTime(OffsetDateTime.parse("2026-10-10T10:00:00Z"));
        activity.setEndTime(OffsetDateTime.parse("2026-10-10T11:00:00Z"));
        activity.setCreatedBy(adminId);
        return activityRepository.saveAndFlush(activity);
    }

    private AttendanceEntity newAttendance(UUID activityId, UUID userId, String personalId) {
        AttendanceEntity attendance = new AttendanceEntity();
        attendance.setActivityId(activityId);
        attendance.setUserId(userId);
        attendance.setPersonalIdSnapshot(personalId);
        attendance.setRegisteredBy(UUID.randomUUID());
        attendance.setCreatedBy(attendance.getRegisteredBy());
        return attendance;
    }
}
