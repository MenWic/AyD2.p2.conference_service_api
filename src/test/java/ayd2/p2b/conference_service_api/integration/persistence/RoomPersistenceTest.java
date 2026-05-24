package ayd2.p2b.conference_service_api.integration.persistence;

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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class RoomPersistenceTest {

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
    private RoomRepository roomRepository;

    @Autowired
    private CongressRepository congressRepository;

    @Autowired
    private InstitutionRepository institutionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldEnforceUniqueRoomNamePerCongress() {
        CongressEntity congress = persistedCongress("USAC");
        roomRepository.saveAndFlush(newRoom(congress.getId(), "Sala A", 120));

        assertThatThrownBy(() -> roomRepository.saveAndFlush(newRoom(congress.getId(), "Sala A", 80)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldEnforceRoomCapacityCheckConstraint() {
        CongressEntity congress = persistedCongress("URL");

        assertThatThrownBy(() -> roomRepository.saveAndFlush(newRoom(congress.getId(), "Sala Cero", 0)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldEnforceForeignKeyCongressConstraint() {
        assertThatThrownBy(() -> roomRepository.saveAndFlush(newRoom(UUID.randomUUID(), "Sala Orfana", 25)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldRestrictRoomDeleteWhenActivitiesReferenceIt() {
        CongressEntity congress = persistedCongress("Del Valle");
        RoomEntity room = roomRepository.saveAndFlush(newRoom(congress.getId(), "Sala Taller", 90));

        jdbcTemplate.update(
                """
                INSERT INTO activities (
                    id, congress_id, room_id, name, description, type,
                    start_time, end_time, workshop_capacity, created_by
                ) VALUES (?, ?, ?, ?, ?, 'PONENCIA', ?, ?, ?, ?)
                """,
                UUID.randomUUID(),
                congress.getId(),
                room.getId(),
                "Ponencia 1",
                "Descripcion",
                OffsetDateTime.parse("2026-10-10T10:00:00Z"),
                OffsetDateTime.parse("2026-10-10T11:00:00Z"),
                null,
                UUID.randomUUID()
        );

        assertThatThrownBy(() -> {
            roomRepository.deleteById(room.getId());
            roomRepository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
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

    private RoomEntity newRoom(UUID congressId, String name, Integer capacity) {
        RoomEntity room = new RoomEntity();
        room.setCongressId(congressId);
        room.setName(name);
        room.setCapacity(capacity);
        room.setLocation("Edificio A");
        room.setCreatedBy(UUID.randomUUID());
        return room;
    }
}
