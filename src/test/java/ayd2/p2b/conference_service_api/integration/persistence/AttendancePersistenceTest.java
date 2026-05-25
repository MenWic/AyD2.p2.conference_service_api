package ayd2.p2b.conference_service_api.integration.persistence;

import ayd2.p2b.conference_service_api.feature.activity.domain.model.ActivityType;
import ayd2.p2b.conference_service_api.feature.activity.infrastructure.persistence.entity.ActivityEntity;
import ayd2.p2b.conference_service_api.feature.activity.infrastructure.persistence.repository.ActivityRepository;
import ayd2.p2b.conference_service_api.feature.attendance.dto.internal.AttendanceSearchCriteria;
import ayd2.p2b.conference_service_api.feature.attendance.infrastructure.persistence.entity.AttendanceEntity;
import ayd2.p2b.conference_service_api.feature.attendance.infrastructure.persistence.repository.AttendanceRepository;
import ayd2.p2b.conference_service_api.feature.attendance.infrastructure.persistence.specification.AttendanceSpecification;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
    @Autowired
    private EntityManager entityManager;

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

    @Test
    void shouldKeepImmutableColumnsUnchangedOnUpdateAttempt() {
        ActivityEntity activity = persistedActivity();
        UUID userId = UUID.randomUUID();
        AttendanceEntity saved = attendanceRepository.saveAndFlush(newAttendance(activity.getId(), userId, "PID-IMM-1"));
        UUID attendanceId = saved.getId();

        entityManager.clear();
        AttendanceEntity persisted = attendanceRepository.findById(attendanceId).orElseThrow();

        UUID originalActivityId = persisted.getActivityId();
        UUID originalUserId = persisted.getUserId();
        String originalPersonalId = persisted.getPersonalIdSnapshot();
        UUID originalRegisteredBy = persisted.getRegisteredBy();
        OffsetDateTime originalRegisteredAt = persisted.getRegisteredAt();
        UUID originalCreatedBy = persisted.getCreatedBy();
        OffsetDateTime originalCreatedAt = persisted.getCreatedAt();

        persisted.setActivityId(UUID.randomUUID());
        persisted.setUserId(UUID.randomUUID());
        persisted.setPersonalIdSnapshot("PID-IMM-2");
        persisted.setRegisteredBy(UUID.randomUUID());
        persisted.setRegisteredAt(originalRegisteredAt.plusHours(1));
        persisted.setCreatedBy(UUID.randomUUID());
        persisted.setCreatedAt(originalCreatedAt.plusHours(1));
        attendanceRepository.saveAndFlush(persisted);

        entityManager.clear();
        AttendanceEntity reloaded = attendanceRepository.findById(attendanceId).orElseThrow();

        assertThat(reloaded.getActivityId()).isEqualTo(originalActivityId);
        assertThat(reloaded.getUserId()).isEqualTo(originalUserId);
        assertThat(reloaded.getPersonalIdSnapshot()).isEqualTo(originalPersonalId);
        assertThat(reloaded.getRegisteredBy()).isEqualTo(originalRegisteredBy);
        assertThat(reloaded.getRegisteredAt()).isEqualTo(originalRegisteredAt);
        assertThat(reloaded.getCreatedBy()).isEqualTo(originalCreatedBy);
        assertThat(reloaded.getCreatedAt()).isEqualTo(originalCreatedAt);
    }

    @Test
    void shouldApplyScopeWithNoCriteriaIncludingOwnerAndLinkedAndExcludingOutsider() {
        UUID requesterUserId = UUID.randomUUID();
        ScopeFixture fixture = persistedScopeFixture(requesterUserId);

        AttendanceEntity ownerAttendance = persistedAttendance(
                fixture.ownerActivityId(),
                UUID.randomUUID(),
                "PID-OWNER-1",
                requesterUserId,
                OffsetDateTime.parse("2026-10-10T10:00:00Z")
        );
        AttendanceEntity linkedAttendance = persistedAttendance(
                fixture.linkedActivityId(),
                UUID.randomUUID(),
                "PID-LINKED-1",
                UUID.randomUUID(),
                OffsetDateTime.parse("2026-10-10T11:00:00Z")
        );
        AttendanceEntity outsiderAttendance = persistedAttendance(
                fixture.outsiderActivityId(),
                UUID.randomUUID(),
                "PID-OUT-1",
                UUID.randomUUID(),
                OffsetDateTime.parse("2026-10-10T12:00:00Z")
        );

        var result = queryByCriteria(null, requesterUserId, Set.of(fixture.linkedInstitutionId()));
        var ids = result.getContent().stream().map(AttendanceEntity::getId).collect(Collectors.toSet());

        assertThat(ids).contains(ownerAttendance.getId(), linkedAttendance.getId());
        assertThat(ids).doesNotContain(outsiderAttendance.getId());
    }

    @Test
    void shouldFallbackToOwnerOnlyWhenLinkedInstitutionIdsAreNullOrEmpty() {
        UUID requesterUserId = UUID.randomUUID();
        ScopeFixture fixture = persistedScopeFixture(requesterUserId);

        AttendanceEntity ownerAttendance = persistedAttendance(
                fixture.ownerActivityId(),
                UUID.randomUUID(),
                "PID-OWNER-ONLY",
                requesterUserId,
                OffsetDateTime.parse("2026-10-11T10:00:00Z")
        );
        AttendanceEntity linkedAttendance = persistedAttendance(
                fixture.linkedActivityId(),
                UUID.randomUUID(),
                "PID-LINKED-SHOULD-NOT-APPEAR",
                UUID.randomUUID(),
                OffsetDateTime.parse("2026-10-11T11:00:00Z")
        );

        var resultWithNull = queryByCriteria(null, requesterUserId, null);
        var resultWithEmpty = queryByCriteria(null, requesterUserId, Set.of());

        var nullIds = resultWithNull.getContent().stream().map(AttendanceEntity::getId).collect(Collectors.toSet());
        var emptyIds = resultWithEmpty.getContent().stream().map(AttendanceEntity::getId).collect(Collectors.toSet());

        assertThat(nullIds).containsExactly(ownerAttendance.getId());
        assertThat(emptyIds).containsExactly(ownerAttendance.getId());
        assertThat(nullIds).doesNotContain(linkedAttendance.getId());
        assertThat(emptyIds).doesNotContain(linkedAttendance.getId());
    }

    @Test
    void shouldFilterByActivityId() {
        UUID requesterUserId = UUID.randomUUID();
        ScopeFixture fixture = persistedScopeFixture(requesterUserId);

        persistedAttendance(
                fixture.ownerActivityId(),
                UUID.randomUUID(),
                "PID-ACT-OWNER",
                requesterUserId,
                OffsetDateTime.parse("2026-10-12T10:00:00Z")
        );
        AttendanceEntity target = persistedAttendance(
                fixture.linkedActivityId(),
                UUID.randomUUID(),
                "PID-ACT-TARGET",
                UUID.randomUUID(),
                OffsetDateTime.parse("2026-10-12T11:00:00Z")
        );

        AttendanceSearchCriteria criteria = AttendanceSearchCriteria.builder()
                .activityId(fixture.linkedActivityId())
                .build();

        var result = queryByCriteria(criteria, requesterUserId, Set.of(fixture.linkedInstitutionId()));
        var ids = result.getContent().stream().map(AttendanceEntity::getId).collect(Collectors.toSet());

        assertThat(ids).containsExactly(target.getId());
    }

    @Test
    void shouldFilterByRoomId() {
        UUID requesterUserId = UUID.randomUUID();
        ScopeFixture fixture = persistedScopeFixture(requesterUserId);

        AttendanceEntity target = persistedAttendance(
                fixture.linkedActivityId(),
                UUID.randomUUID(),
                "PID-ROOM-TARGET",
                UUID.randomUUID(),
                OffsetDateTime.parse("2026-10-13T10:00:00Z")
        );
        AttendanceEntity otherRoomAttendance = persistedAttendance(
                fixture.linkedSecondActivityId(),
                UUID.randomUUID(),
                "PID-ROOM-OTHER",
                UUID.randomUUID(),
                OffsetDateTime.parse("2026-10-13T11:00:00Z")
        );

        AttendanceSearchCriteria criteria = AttendanceSearchCriteria.builder()
                .roomId(fixture.linkedRoomId())
                .build();

        var result = queryByCriteria(criteria, requesterUserId, Set.of(fixture.linkedInstitutionId()));
        var ids = result.getContent().stream().map(AttendanceEntity::getId).collect(Collectors.toSet());

        assertThat(ids).containsExactly(target.getId());
        assertThat(ids).doesNotContain(otherRoomAttendance.getId());
    }

    @Test
    void shouldFilterByDateRangeUsingInclusiveCalendarDays() {
        UUID requesterUserId = UUID.randomUUID();
        ScopeFixture fixture = persistedScopeFixture(requesterUserId);

        AttendanceEntity atDayStart = persistedAttendance(
                fixture.ownerActivityId(),
                UUID.randomUUID(),
                "PID-DATE-START",
                requesterUserId,
                OffsetDateTime.parse("2026-10-14T00:00:00Z")
        );
        AttendanceEntity beforeNextDay = persistedAttendance(
                fixture.ownerActivityId(),
                UUID.randomUUID(),
                "PID-DATE-END",
                requesterUserId,
                OffsetDateTime.parse("2026-10-14T23:59:59Z")
        );
        AttendanceEntity nextDay = persistedAttendance(
                fixture.ownerActivityId(),
                UUID.randomUUID(),
                "PID-DATE-NEXT",
                requesterUserId,
                OffsetDateTime.parse("2026-10-15T00:00:00Z")
        );

        AttendanceSearchCriteria criteria = AttendanceSearchCriteria.builder()
                .dateFrom(LocalDate.of(2026, 10, 14))
                .dateTo(LocalDate.of(2026, 10, 14))
                .build();

        var result = queryByCriteria(criteria, requesterUserId, Set.of());
        var ids = result.getContent().stream().map(AttendanceEntity::getId).collect(Collectors.toSet());

        assertThat(ids).contains(atDayStart.getId(), beforeNextDay.getId());
        assertThat(ids).doesNotContain(nextDay.getId());
    }

    @Test
    void shouldFilterByPersonalIdUsingTrimmedCaseInsensitiveExactMatch() {
        UUID requesterUserId = UUID.randomUUID();
        ScopeFixture fixture = persistedScopeFixture(requesterUserId);

        AttendanceEntity target = persistedAttendance(
                fixture.ownerActivityId(),
                UUID.randomUUID(),
                "PID-ABC-1",
                requesterUserId,
                OffsetDateTime.parse("2026-10-16T10:00:00Z")
        );
        AttendanceEntity partialMatchShouldFail = persistedAttendance(
                fixture.ownerActivityId(),
                UUID.randomUUID(),
                "PID-ABC-10",
                requesterUserId,
                OffsetDateTime.parse("2026-10-16T11:00:00Z")
        );

        AttendanceSearchCriteria criteria = AttendanceSearchCriteria.builder()
                .personalId("   pid-abc-1   ")
                .build();

        var result = queryByCriteria(criteria, requesterUserId, Set.of());
        var ids = result.getContent().stream().map(AttendanceEntity::getId).collect(Collectors.toSet());

        assertThat(ids).containsExactly(target.getId());
        assertThat(ids).doesNotContain(partialMatchShouldFail.getId());
    }

    @Test
    void shouldApplyCombinedFiltersReturningOnlySingleMatch() {
        UUID requesterUserId = UUID.randomUUID();
        ScopeFixture fixture = persistedScopeFixture(requesterUserId);

        AttendanceEntity target = persistedAttendance(
                fixture.linkedActivityId(),
                UUID.randomUUID(),
                "PID-COMB-1",
                UUID.randomUUID(),
                OffsetDateTime.parse("2026-10-17T12:00:00Z")
        );
        persistedAttendance(
                fixture.linkedActivityId(),
                UUID.randomUUID(),
                "PID-COMB-OTHER",
                UUID.randomUUID(),
                OffsetDateTime.parse("2026-10-17T12:10:00Z")
        );
        persistedAttendance(
                fixture.linkedSecondActivityId(),
                UUID.randomUUID(),
                "PID-COMB-1",
                UUID.randomUUID(),
                OffsetDateTime.parse("2026-10-17T12:00:00Z")
        );
        persistedAttendance(
                fixture.linkedActivityId(),
                UUID.randomUUID(),
                "PID-COMB-1",
                UUID.randomUUID(),
                OffsetDateTime.parse("2026-10-18T12:00:00Z")
        );

        AttendanceSearchCriteria criteria = AttendanceSearchCriteria.builder()
                .activityId(fixture.linkedActivityId())
                .roomId(fixture.linkedRoomId())
                .dateFrom(LocalDate.of(2026, 10, 17))
                .dateTo(LocalDate.of(2026, 10, 17))
                .personalId("  pid-comb-1 ")
                .build();

        var result = queryByCriteria(criteria, requesterUserId, Set.of(fixture.linkedInstitutionId()));
        var ids = result.getContent().stream().map(AttendanceEntity::getId).collect(Collectors.toSet());

        assertThat(ids).containsExactly(target.getId());
    }

    private org.springframework.data.domain.Page<AttendanceEntity> queryByCriteria(
            AttendanceSearchCriteria criteria,
            UUID requesterUserId,
            Set<UUID> linkedInstitutionIds
    ) {
        Specification<AttendanceEntity> specification = AttendanceSpecification.byCriteriaAndScope(
                criteria,
                requesterUserId,
                linkedInstitutionIds
        );
        return attendanceRepository.findAll(specification, PageRequest.of(0, 50));
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

    private ScopeFixture persistedScopeFixture(UUID requesterUserId) {
        UUID linkedAdmin = UUID.randomUUID();
        UUID outsiderAdmin = UUID.randomUUID();

        InstitutionEntity ownerInstitution = persistedInstitution("Owner Institution", requesterUserId);
        InstitutionEntity linkedInstitution = persistedInstitution("Linked Institution", linkedAdmin);
        InstitutionEntity outsiderInstitution = persistedInstitution("Outsider Institution", outsiderAdmin);

        CongressEntity ownerCongress = persistedCongress(ownerInstitution.getId(), requesterUserId, "Owner Congress");
        CongressEntity linkedCongress = persistedCongress(linkedInstitution.getId(), linkedAdmin, "Linked Congress");
        CongressEntity outsiderCongress = persistedCongress(outsiderInstitution.getId(), outsiderAdmin, "Outsider Congress");

        RoomEntity ownerRoom = persistedRoom(ownerCongress.getId(), requesterUserId, "Owner Room");
        RoomEntity linkedRoom = persistedRoom(linkedCongress.getId(), linkedAdmin, "Linked Room");
        RoomEntity linkedSecondRoom = persistedRoom(linkedCongress.getId(), linkedAdmin, "Linked Room 2");
        RoomEntity outsiderRoom = persistedRoom(outsiderCongress.getId(), outsiderAdmin, "Outsider Room");

        ActivityEntity ownerActivity = persistedActivity(
                ownerCongress.getId(),
                ownerRoom.getId(),
                requesterUserId,
                "Owner Activity",
                OffsetDateTime.parse("2026-10-10T09:00:00Z"),
                OffsetDateTime.parse("2026-10-10T10:00:00Z")
        );
        ActivityEntity linkedActivity = persistedActivity(
                linkedCongress.getId(),
                linkedRoom.getId(),
                linkedAdmin,
                "Linked Activity",
                OffsetDateTime.parse("2026-10-11T09:00:00Z"),
                OffsetDateTime.parse("2026-10-11T10:00:00Z")
        );
        ActivityEntity linkedSecondActivity = persistedActivity(
                linkedCongress.getId(),
                linkedSecondRoom.getId(),
                linkedAdmin,
                "Linked Activity 2",
                OffsetDateTime.parse("2026-10-12T09:00:00Z"),
                OffsetDateTime.parse("2026-10-12T10:00:00Z")
        );
        ActivityEntity outsiderActivity = persistedActivity(
                outsiderCongress.getId(),
                outsiderRoom.getId(),
                outsiderAdmin,
                "Outsider Activity",
                OffsetDateTime.parse("2026-10-13T09:00:00Z"),
                OffsetDateTime.parse("2026-10-13T10:00:00Z")
        );

        return new ScopeFixture(
                linkedInstitution.getId(),
                ownerActivity.getId(),
                linkedActivity.getId(),
                linkedSecondActivity.getId(),
                outsiderActivity.getId(),
                linkedRoom.getId()
        );
    }

    private InstitutionEntity persistedInstitution(String namePrefix, UUID createdBy) {
        InstitutionEntity institution = new InstitutionEntity();
        institution.setName(namePrefix + "-" + UUID.randomUUID());
        institution.setDescription(namePrefix + " Description");
        institution.setContactEmail(UUID.randomUUID() + "@example.com");
        institution.setActive(true);
        institution.setCreatedBy(createdBy);
        return institutionRepository.saveAndFlush(institution);
    }

    private CongressEntity persistedCongress(UUID institutionId, UUID createdBy, String namePrefix) {
        CongressEntity congress = new CongressEntity();
        congress.setInstitutionId(institutionId);
        congress.setName(namePrefix + "-" + UUID.randomUUID());
        congress.setDescription(namePrefix + " Description");
        congress.setStartDate(LocalDate.of(2026, 10, 10));
        congress.setEndDate(LocalDate.of(2026, 10, 12));
        congress.setLocation("Guatemala");
        congress.setPrice(new BigDecimal("45.00"));
        congress.setCreatedBy(createdBy);
        return congressRepository.saveAndFlush(congress);
    }

    private RoomEntity persistedRoom(UUID congressId, UUID createdBy, String namePrefix) {
        RoomEntity room = new RoomEntity();
        room.setCongressId(congressId);
        room.setName(namePrefix + "-" + UUID.randomUUID());
        room.setCapacity(120);
        room.setLocation("Building B");
        room.setCreatedBy(createdBy);
        return roomRepository.saveAndFlush(room);
    }

    private ActivityEntity persistedActivity(
            UUID congressId,
            UUID roomId,
            UUID createdBy,
            String namePrefix,
            OffsetDateTime startTime,
            OffsetDateTime endTime
    ) {
        ActivityEntity activity = new ActivityEntity();
        activity.setCongressId(congressId);
        activity.setRoomId(roomId);
        activity.setName(namePrefix + "-" + UUID.randomUUID());
        activity.setDescription(namePrefix + " Description");
        activity.setType(ActivityType.PONENCIA);
        activity.setWorkshopCapacity(null);
        activity.setStartTime(startTime);
        activity.setEndTime(endTime);
        activity.setCreatedBy(createdBy);
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

    private AttendanceEntity persistedAttendance(
            UUID activityId,
            UUID userId,
            String personalId,
            UUID registeredBy,
            OffsetDateTime registeredAt
    ) {
        AttendanceEntity attendance = new AttendanceEntity();
        attendance.setActivityId(activityId);
        attendance.setUserId(userId);
        attendance.setPersonalIdSnapshot(personalId);
        attendance.setRegisteredBy(registeredBy);
        attendance.setRegisteredAt(registeredAt);
        attendance.setCreatedBy(registeredBy);
        return attendanceRepository.saveAndFlush(attendance);
    }

    private record ScopeFixture(
            UUID linkedInstitutionId,
            UUID ownerActivityId,
            UUID linkedActivityId,
            UUID linkedSecondActivityId,
            UUID outsiderActivityId,
            UUID linkedRoomId
    ) {
    }
}
