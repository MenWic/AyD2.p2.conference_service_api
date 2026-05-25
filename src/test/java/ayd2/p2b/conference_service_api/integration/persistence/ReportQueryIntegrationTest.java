package ayd2.p2b.conference_service_api.integration.persistence;

import ayd2.p2b.conference_service_api.feature.activity.domain.model.ActivityType;
import ayd2.p2b.conference_service_api.feature.activity.infrastructure.persistence.entity.ActivityEntity;
import ayd2.p2b.conference_service_api.feature.activity.infrastructure.persistence.repository.ActivityRepository;
import ayd2.p2b.conference_service_api.feature.attendance.infrastructure.persistence.entity.AttendanceEntity;
import ayd2.p2b.conference_service_api.feature.attendance.infrastructure.persistence.repository.AttendanceJpaRepository;
import ayd2.p2b.conference_service_api.feature.congress.infrastructure.persistence.entity.CongressEntity;
import ayd2.p2b.conference_service_api.feature.congress.infrastructure.persistence.repository.CongressRepository;
import ayd2.p2b.conference_service_api.feature.institution.infrastructure.persistence.entity.InstitutionEntity;
import ayd2.p2b.conference_service_api.feature.institution.infrastructure.persistence.repository.InstitutionRepository;
import ayd2.p2b.conference_service_api.feature.reservation.infrastructure.persistence.entity.ReservationEntity;
import ayd2.p2b.conference_service_api.feature.reservation.infrastructure.persistence.repository.ReservationJpaRepository;
import ayd2.p2b.conference_service_api.feature.room.infrastructure.persistence.entity.RoomEntity;
import ayd2.p2b.conference_service_api.feature.room.infrastructure.persistence.repository.RoomRepository;
import ayd2.p2b.conference_service_api.feature.report.dto.response.AttendanceActivityItem;
import ayd2.p2b.conference_service_api.feature.report.dto.response.WorkshopReservationItem;
import ayd2.p2b.conference_service_api.feature.report.infrastructure.persistence.adapter.JpaParticipantsCongressScopeAdapter;
import ayd2.p2b.conference_service_api.feature.report.infrastructure.persistence.query.JpaAttendanceByActivityQuery;
import ayd2.p2b.conference_service_api.feature.report.infrastructure.persistence.query.JpaWorkshopReservationsQuery;
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
@Import({
        JpaAttendanceByActivityQuery.class,
        JpaWorkshopReservationsQuery.class,
        JpaParticipantsCongressScopeAdapter.class
})
class ReportQueryIntegrationTest {

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
    private JpaAttendanceByActivityQuery attendanceQuery;
    @Autowired
    private JpaWorkshopReservationsQuery workshopQuery;
    @Autowired
    private JpaParticipantsCongressScopeAdapter congressScopeAdapter;

    @Autowired
    private InstitutionRepository institutionRepository;
    @Autowired
    private CongressRepository congressRepository;
    @Autowired
    private RoomRepository roomRepository;
    @Autowired
    private ActivityRepository activityRepository;
    @Autowired
    private AttendanceJpaRepository attendanceRepository;
    @Autowired
    private ReservationJpaRepository reservationRepository;

    private UUID congressId;
    private UUID activityPonenciaId;
    private UUID activityTallerId;
    private UUID roomId;

    @BeforeEach
    void setUp() {
        InstitutionEntity inst = new InstitutionEntity();
        inst.setName("Test Institution " + UUID.randomUUID());
        inst.setDescription("Description");
        inst.setContactEmail("test@example.com");
        inst.setActive(true);
        inst.setCreatedBy(UUID.randomUUID());
        inst = institutionRepository.saveAndFlush(inst);

        CongressEntity congress = new CongressEntity();
        congress.setInstitutionId(inst.getId());
        congress.setName("Test Congress");
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
        room.setName("Main Hall");
        room.setCreatedBy(UUID.randomUUID());
        room = roomRepository.saveAndFlush(room);
        roomId = room.getId();

        OffsetDateTime start = OffsetDateTime.of(2026, 6, 1, 9, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime end = OffsetDateTime.of(2026, 6, 1, 11, 0, 0, 0, ZoneOffset.UTC);

        UUID adminId = UUID.randomUUID();

        ActivityEntity ponencia = new ActivityEntity();
        ponencia.setCongressId(congressId);
        ponencia.setRoomId(roomId);
        ponencia.setName("Keynote");
        ponencia.setDescription("Opening keynote");
        ponencia.setType(ActivityType.PONENCIA);
        ponencia.setStartTime(start);
        ponencia.setEndTime(end);
        ponencia.setCreatedBy(adminId);
        ponencia = activityRepository.saveAndFlush(ponencia);
        activityPonenciaId = ponencia.getId();

        ActivityEntity taller = new ActivityEntity();
        taller.setCongressId(congressId);
        taller.setRoomId(roomId);
        taller.setName("Java Workshop");
        taller.setDescription("Hands-on Java");
        taller.setType(ActivityType.TALLER);
        taller.setStartTime(start.plusHours(2));
        taller.setEndTime(end.plusHours(2));
        taller.setWorkshopCapacity(30);
        taller.setCreatedBy(adminId);
        taller = activityRepository.saveAndFlush(taller);
        activityTallerId = taller.getId();
    }

    // ── JpaAttendanceByActivityQuery ─────────────────────────────────────────

    @Test
    void attendance_query_no_filters_returns_all_activities_for_congress() {
        List<AttendanceActivityItem> result = attendanceQuery.query(congressId, null, null, null, null);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(AttendanceActivityItem::getActivityName)
                .containsExactlyInAnyOrder("Keynote", "Java Workshop");
    }

    @Test
    void attendance_query_filters_by_activity_id() {
        List<AttendanceActivityItem> result = attendanceQuery.query(congressId, activityPonenciaId, null, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getActivityName()).isEqualTo("Keynote");
    }

    @Test
    void attendance_query_filters_by_room_id() {
        List<AttendanceActivityItem> result = attendanceQuery.query(congressId, null, roomId, null, null);

        assertThat(result).hasSize(2);
    }

    @Test
    void attendance_query_filters_by_date_from() {
        OffsetDateTime dateFrom = OffsetDateTime.of(2026, 6, 1, 10, 0, 0, 0, ZoneOffset.UTC);
        List<AttendanceActivityItem> result = attendanceQuery.query(congressId, null, null, dateFrom, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getActivityName()).isEqualTo("Java Workshop");
    }

    @Test
    void attendance_query_filters_by_date_to() {
        OffsetDateTime dateTo = OffsetDateTime.of(2026, 6, 1, 10, 0, 0, 0, ZoneOffset.UTC);
        List<AttendanceActivityItem> result = attendanceQuery.query(congressId, null, null, null, dateTo);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getActivityName()).isEqualTo("Keynote");
    }

    @Test
    void attendance_query_counts_attendances_correctly() {
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();
        persistAttendance(activityPonenciaId, user1, "ID001");
        persistAttendance(activityPonenciaId, user2, "ID002");

        List<AttendanceActivityItem> result = attendanceQuery.query(congressId, activityPonenciaId, null, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAttendanceCount()).isEqualTo(2L);
    }

    @Test
    void attendance_query_returns_room_name() {
        List<AttendanceActivityItem> result = attendanceQuery.query(congressId, activityPonenciaId, null, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRoomName()).isEqualTo("Main Hall");
    }

    @Test
    void attendance_query_different_congress_returns_empty() {
        UUID otherCongressId = UUID.randomUUID();
        List<AttendanceActivityItem> result = attendanceQuery.query(otherCongressId, null, null, null, null);

        assertThat(result).isEmpty();
    }

    // ── JpaWorkshopReservationsQuery ─────────────────────────────────────────

    @Test
    void workshop_query_returns_only_taller_activities() {
        List<WorkshopReservationItem> result = workshopQuery.query(congressId, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getActivityName()).isEqualTo("Java Workshop");
    }

    @Test
    void workshop_query_filters_by_activity_id() {
        List<WorkshopReservationItem> result = workshopQuery.query(congressId, activityTallerId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getActivityId()).isEqualTo(activityTallerId);
    }

    @Test
    void workshop_query_activity_id_filter_for_ponencia_returns_empty() {
        List<WorkshopReservationItem> result = workshopQuery.query(congressId, activityPonenciaId);

        assertThat(result).isEmpty();
    }

    @Test
    void workshop_query_counts_reservations_and_available_seats() {
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();
        persistReservation(activityTallerId, user1);
        persistReservation(activityTallerId, user2);

        List<WorkshopReservationItem> result = workshopQuery.query(congressId, activityTallerId);

        assertThat(result).hasSize(1);
        WorkshopReservationItem item = result.get(0);
        assertThat(item.getWorkshopCapacity()).isEqualTo(30);
        assertThat(item.getReservationCount()).isEqualTo(2);
        assertThat(item.getAvailableSeats()).isEqualTo(28);
    }

    @Test
    void workshop_query_available_seats_never_negative() {
        for (int i = 0; i < 35; i++) {
            persistReservation(activityTallerId, UUID.randomUUID());
        }

        List<WorkshopReservationItem> result = workshopQuery.query(congressId, activityTallerId);

        assertThat(result.get(0).getAvailableSeats()).isEqualTo(0);
    }

    @Test
    void workshop_query_roster_contains_reserved_user_ids() {
        UUID userId = UUID.randomUUID();
        persistReservation(activityTallerId, userId);

        List<WorkshopReservationItem> result = workshopQuery.query(congressId, activityTallerId);

        assertThat(result.get(0).getRoster()).hasSize(1);
        assertThat(result.get(0).getRoster().get(0).getPersonalId()).isEqualTo(userId.toString());
    }

    @Test
    void workshop_query_different_congress_returns_empty() {
        List<WorkshopReservationItem> result = workshopQuery.query(UUID.randomUUID(), null);

        assertThat(result).isEmpty();
    }

    // ── JpaParticipantsCongressScopeAdapter ──────────────────────────────────

    @Test
    void congress_scope_adapter_returns_summary_for_existing_congress() {
        var summary = congressScopeAdapter.findCongressSummary(congressId);

        assertThat(summary).isPresent();
        assertThat(summary.get().getCongressId()).isEqualTo(congressId);
        assertThat(summary.get().getCongressName()).isEqualTo("Test Congress");
    }

    @Test
    void congress_scope_adapter_returns_empty_for_unknown_congress() {
        var summary = congressScopeAdapter.findCongressSummary(UUID.randomUUID());

        assertThat(summary).isEmpty();
    }

    @Test
    void congress_scope_adapter_includes_institution_id() {
        var summary = congressScopeAdapter.findCongressSummary(congressId);

        assertThat(summary).isPresent();
        assertThat(summary.get().getInstitutionId()).isNotNull();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void persistAttendance(UUID activityId, UUID userId, String personalId) {
        AttendanceEntity a = new AttendanceEntity();
        a.setActivityId(activityId);
        a.setUserId(userId);
        a.setPersonalIdSnapshot(personalId);
        a.setRegisteredBy(UUID.randomUUID());
        a.setRegisteredAt(OffsetDateTime.now());
        a.setCreatedBy(UUID.randomUUID());
        a.setCreatedAt(OffsetDateTime.now());
        attendanceRepository.saveAndFlush(a);
    }

    private void persistReservation(UUID activityId, UUID userId) {
        ReservationEntity r = new ReservationEntity();
        r.setActivityId(activityId);
        r.setUserId(userId);
        r.setReservedAt(OffsetDateTime.now());
        r.setCreatedBy(UUID.randomUUID());
        reservationRepository.saveAndFlush(r);
    }
}
