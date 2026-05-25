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
import ayd2.p2b.conference_service_api.feature.report.dto.internal.WorkshopReservationRow;
import ayd2.p2b.conference_service_api.feature.report.dto.response.AttendanceActivityItem;
import ayd2.p2b.conference_service_api.feature.report.infrastructure.persistence.adapter.JpaParticipantsCongressScopeAdapter;
import ayd2.p2b.conference_service_api.feature.report.infrastructure.persistence.query.JpaAttendanceByActivityQuery;
import ayd2.p2b.conference_service_api.feature.report.infrastructure.persistence.query.JpaWorkshopReservationsQuery;
import ayd2.p2b.conference_service_api.feature.reservation.infrastructure.persistence.entity.ReservationEntity;
import ayd2.p2b.conference_service_api.feature.reservation.infrastructure.persistence.repository.ReservationJpaRepository;
import ayd2.p2b.conference_service_api.feature.room.infrastructure.persistence.entity.RoomEntity;
import ayd2.p2b.conference_service_api.feature.room.infrastructure.persistence.repository.RoomRepository;
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
    private UUID otherCongressId;
    private UUID activityPonenciaId;
    private UUID activityTallerId;
    private UUID otherCongressTallerId;
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

        otherCongressId = createOtherCongress();
        UUID otherRoom = createRoom(otherCongressId, "Other room");
        otherCongressTallerId = createActivity(otherCongressId, otherRoom, "Other Workshop", ActivityType.TALLER, 10, start.plusDays(1));
    }

    @Test
    void attendance_query_no_filters_returns_all_congress_activities_in_deterministic_order() {
        List<AttendanceActivityItem> result = attendanceQuery.query(congressId, null, null, null, null);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(AttendanceActivityItem::getActivityName)
                .containsExactly("Keynote", "Java Workshop");
    }

    @Test
    void attendance_query_includes_zero_attendance_activities() {
        List<AttendanceActivityItem> result = attendanceQuery.query(congressId, null, null, null, null);

        assertThat(result).extracting(AttendanceActivityItem::getAttendanceCount)
                .containsExactly(0L, 0L);
    }

    @Test
    void attendance_query_filters_by_activity_id_scoped_to_congress() {
        List<AttendanceActivityItem> sameCongress = attendanceQuery.query(congressId, activityPonenciaId, null, null, null);
        List<AttendanceActivityItem> otherCongressActivity = attendanceQuery.query(congressId, otherCongressTallerId, null, null, null);

        assertThat(sameCongress).hasSize(1);
        assertThat(sameCongress.get(0).getActivityName()).isEqualTo("Keynote");
        assertThat(otherCongressActivity).isEmpty();
    }

    @Test
    void attendance_query_filters_by_room_id_scoped_to_congress() {
        List<AttendanceActivityItem> result = attendanceQuery.query(congressId, null, roomId, null, null);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(AttendanceActivityItem::getActivityName)
                .containsExactly("Keynote", "Java Workshop");
    }

    @Test
    void attendance_query_filters_by_date_range_on_start_time() {
        OffsetDateTime from = OffsetDateTime.of(2026, 6, 1, 10, 0, 0, 0, ZoneOffset.UTC);
        OffsetDateTime to = OffsetDateTime.of(2026, 6, 1, 10, 59, 59, 0, ZoneOffset.UTC);

        List<AttendanceActivityItem> byFrom = attendanceQuery.query(congressId, null, null, from, null);
        List<AttendanceActivityItem> byTo = attendanceQuery.query(congressId, null, null, null, to);

        assertThat(byFrom).hasSize(1);
        assertThat(byFrom.get(0).getActivityName()).isEqualTo("Java Workshop");
        assertThat(byTo).hasSize(1);
        assertThat(byTo.get(0).getActivityName()).isEqualTo("Keynote");
    }

    @Test
    void attendance_query_counts_attendances_correctly() {
        persistAttendance(activityPonenciaId, UUID.randomUUID(), "ID001");
        persistAttendance(activityPonenciaId, UUID.randomUUID(), "ID002");

        List<AttendanceActivityItem> result = attendanceQuery.query(congressId, activityPonenciaId, null, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAttendanceCount()).isEqualTo(2L);
    }

    @Test
    void workshop_query_returns_only_taller_activities() {
        List<WorkshopReservationRow> result = workshopQuery.query(congressId, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getActivityId()).isEqualTo(activityTallerId);
        assertThat(result.get(0).getActivityName()).isEqualTo("Java Workshop");
    }

    @Test
    void workshop_query_filters_by_activity_id_scoped_to_congress() {
        List<WorkshopReservationRow> sameCongress = workshopQuery.query(congressId, activityTallerId);
        List<WorkshopReservationRow> otherCongress = workshopQuery.query(congressId, otherCongressTallerId);
        List<WorkshopReservationRow> ponencia = workshopQuery.query(congressId, activityPonenciaId);

        assertThat(sameCongress).hasSize(1);
        assertThat(sameCongress.get(0).getActivityId()).isEqualTo(activityTallerId);
        assertThat(otherCongress).isEmpty();
        assertThat(ponencia).isEmpty();
    }

    @Test
    void workshop_query_returns_reserved_user_ids_without_fallback_personal_id_logic() {
        UUID user1 = UUID.randomUUID();
        UUID user2 = UUID.randomUUID();
        persistReservation(activityTallerId, user1);
        persistReservation(activityTallerId, user2);

        List<WorkshopReservationRow> result = workshopQuery.query(congressId, activityTallerId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getReservedUserIds()).containsExactlyInAnyOrder(user1, user2);
    }

    @Test
    void workshop_query_includes_workshop_without_reservations() {
        List<WorkshopReservationRow> result = workshopQuery.query(congressId, activityTallerId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getReservedUserIds()).isEmpty();
        assertThat(result.get(0).getWorkshopCapacity()).isEqualTo(30);
    }

    @Test
    void workshop_query_different_congress_returns_empty() {
        List<WorkshopReservationRow> result = workshopQuery.query(UUID.randomUUID(), null);

        assertThat(result).isEmpty();
    }

    @Test
    void congress_scope_adapter_returns_summary_for_existing_congress() {
        var summary = congressScopeAdapter.findCongressSummary(congressId);

        assertThat(summary).isPresent();
        assertThat(summary.get().getCongressId()).isEqualTo(congressId);
        assertThat(summary.get().getCongressName()).isEqualTo("Test Congress");
        assertThat(summary.get().getInstitutionId()).isNotNull();
    }

    @Test
    void congress_scope_adapter_returns_empty_for_unknown_congress() {
        assertThat(congressScopeAdapter.findCongressSummary(UUID.randomUUID())).isEmpty();
    }

    private UUID createOtherCongress() {
        InstitutionEntity inst = new InstitutionEntity();
        inst.setName("Other Institution " + UUID.randomUUID());
        inst.setDescription("Description");
        inst.setContactEmail("other@example.com");
        inst.setActive(true);
        inst.setCreatedBy(UUID.randomUUID());
        inst = institutionRepository.saveAndFlush(inst);

        CongressEntity congress = new CongressEntity();
        congress.setInstitutionId(inst.getId());
        congress.setName("Other Congress");
        congress.setDescription("Description");
        congress.setStartDate(LocalDate.of(2026, 6, 10));
        congress.setEndDate(LocalDate.of(2026, 6, 15));
        congress.setLocation("Xela");
        congress.setPrice(new BigDecimal("80.00"));
        congress.setCreatedBy(UUID.randomUUID());
        return congressRepository.saveAndFlush(congress).getId();
    }

    private UUID createRoom(UUID targetCongressId, String name) {
        RoomEntity room = new RoomEntity();
        room.setCongressId(targetCongressId);
        room.setName(name);
        room.setCreatedBy(UUID.randomUUID());
        return roomRepository.saveAndFlush(room).getId();
    }

    private UUID createActivity(
            UUID targetCongressId,
            UUID targetRoomId,
            String name,
            ActivityType type,
            Integer workshopCapacity,
            OffsetDateTime startTime
    ) {
        ActivityEntity activity = new ActivityEntity();
        activity.setCongressId(targetCongressId);
        activity.setRoomId(targetRoomId);
        activity.setName(name);
        activity.setDescription("Description");
        activity.setType(type);
        activity.setStartTime(startTime);
        activity.setEndTime(startTime.plusHours(1));
        activity.setWorkshopCapacity(workshopCapacity);
        activity.setCreatedBy(UUID.randomUUID());
        return activityRepository.saveAndFlush(activity).getId();
    }

    private void persistAttendance(UUID activityId, UUID userId, String personalId) {
        AttendanceEntity attendance = new AttendanceEntity();
        attendance.setActivityId(activityId);
        attendance.setUserId(userId);
        attendance.setPersonalIdSnapshot(personalId);
        attendance.setRegisteredBy(UUID.randomUUID());
        attendance.setRegisteredAt(OffsetDateTime.now());
        attendance.setCreatedBy(UUID.randomUUID());
        attendance.setCreatedAt(OffsetDateTime.now());
        attendanceRepository.saveAndFlush(attendance);
    }

    private void persistReservation(UUID activityId, UUID userId) {
        ReservationEntity reservation = new ReservationEntity();
        reservation.setActivityId(activityId);
        reservation.setUserId(userId);
        reservation.setReservedAt(OffsetDateTime.now());
        reservation.setCreatedBy(UUID.randomUUID());
        reservationRepository.saveAndFlush(reservation);
    }
}
