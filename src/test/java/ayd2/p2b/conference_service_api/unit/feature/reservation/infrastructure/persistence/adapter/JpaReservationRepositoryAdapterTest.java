package ayd2.p2b.conference_service_api.unit.feature.reservation.infrastructure.persistence.adapter;

import ayd2.p2b.conference_service_api.feature.reservation.domain.model.Reservation;
import ayd2.p2b.conference_service_api.feature.reservation.infrastructure.persistence.adapter.JpaReservationRepositoryAdapter;
import ayd2.p2b.conference_service_api.feature.reservation.infrastructure.persistence.entity.ReservationEntity;
import ayd2.p2b.conference_service_api.feature.reservation.infrastructure.persistence.repository.ReservationRepository;
import ayd2.p2b.conference_service_api.feature.reservation.mapper.ReservationMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaReservationRepositoryAdapterTest {

    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private ReservationMapper reservationMapper;

    private JpaReservationRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaReservationRepositoryAdapter(reservationRepository, reservationMapper);
    }

    @Test
    void shouldSaveReservationThroughRepositoryAndMapper() {
        Reservation domain = Reservation.builder()
                .id(UUID.randomUUID())
                .activityId(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .createdBy(UUID.randomUUID())
                .build();
        ReservationEntity entity = new ReservationEntity();
        ReservationEntity savedEntity = new ReservationEntity();
        savedEntity.setId(domain.getId());

        when(reservationMapper.toEntity(domain)).thenReturn(entity);
        when(reservationRepository.save(entity)).thenReturn(savedEntity);
        when(reservationMapper.toDomain(savedEntity)).thenReturn(domain);

        Reservation result = adapter.save(domain);

        assertThat(result).isEqualTo(domain);
    }

    @Test
    void shouldFindReservationByIdWhenPresent() {
        UUID reservationId = UUID.randomUUID();
        ReservationEntity entity = new ReservationEntity();
        Reservation domain = Reservation.builder().id(reservationId).build();

        when(reservationRepository.findById(reservationId)).thenReturn(Optional.of(entity));
        when(reservationMapper.toDomain(entity)).thenReturn(domain);

        Optional<Reservation> result = adapter.findById(reservationId);

        assertThat(result).contains(domain);
    }

    @Test
    void shouldReturnEmptyWhenFindByIdIsMissing() {
        UUID reservationId = UUID.randomUUID();
        when(reservationRepository.findById(reservationId)).thenReturn(Optional.empty());

        Optional<Reservation> result = adapter.findById(reservationId);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldDelegateExistsByActivityIdAndUserIdReturningTrue() {
        UUID activityId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(reservationRepository.existsByActivityIdAndUserId(activityId, userId)).thenReturn(true);

        boolean exists = adapter.existsByActivityIdAndUserId(activityId, userId);

        assertThat(exists).isTrue();
        verify(reservationRepository).existsByActivityIdAndUserId(activityId, userId);
    }

    @Test
    void shouldDelegateExistsByActivityIdAndUserIdReturningFalse() {
        UUID activityId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(reservationRepository.existsByActivityIdAndUserId(activityId, userId)).thenReturn(false);

        boolean exists = adapter.existsByActivityIdAndUserId(activityId, userId);

        assertThat(exists).isFalse();
        verify(reservationRepository).existsByActivityIdAndUserId(activityId, userId);
    }

    @Test
    void shouldCountByActivityId() {
        UUID activityId = UUID.randomUUID();
        when(reservationRepository.countByActivityId(activityId)).thenReturn(3L);

        long count = adapter.countByActivityId(activityId);

        assertThat(count).isEqualTo(3L);
        verify(reservationRepository).countByActivityId(activityId);
    }

    @Test
    void shouldFindByActivityIdAndMapPageContent() {
        UUID activityId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        ReservationEntity firstEntity = new ReservationEntity();
        firstEntity.setId(UUID.randomUUID());
        ReservationEntity secondEntity = new ReservationEntity();
        secondEntity.setId(UUID.randomUUID());

        Reservation firstDomain = Reservation.builder().id(firstEntity.getId()).build();
        Reservation secondDomain = Reservation.builder().id(secondEntity.getId()).build();

        when(reservationRepository.findByActivityId(activityId, pageable))
                .thenReturn(new PageImpl<>(List.of(firstEntity, secondEntity), pageable, 2));
        when(reservationMapper.toDomain(firstEntity)).thenReturn(firstDomain);
        when(reservationMapper.toDomain(secondEntity)).thenReturn(secondDomain);

        Page<Reservation> result = adapter.findByActivityId(activityId, pageable);

        assertThat(result.getContent()).extracting(Reservation::getId)
                .containsExactly(firstEntity.getId(), secondEntity.getId());
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    void shouldFindByUserIdAndMapPageContent() {
        UUID userId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);
        ReservationEntity firstEntity = new ReservationEntity();
        firstEntity.setId(UUID.randomUUID());
        ReservationEntity secondEntity = new ReservationEntity();
        secondEntity.setId(UUID.randomUUID());

        Reservation firstDomain = Reservation.builder().id(firstEntity.getId()).build();
        Reservation secondDomain = Reservation.builder().id(secondEntity.getId()).build();

        when(reservationRepository.findByUserId(userId, pageable))
                .thenReturn(new PageImpl<>(List.of(firstEntity, secondEntity), pageable, 2));
        when(reservationMapper.toDomain(firstEntity)).thenReturn(firstDomain);
        when(reservationMapper.toDomain(secondEntity)).thenReturn(secondDomain);

        Page<Reservation> result = adapter.findByUserId(userId, pageable);

        assertThat(result.getContent()).extracting(Reservation::getId)
                .containsExactly(firstEntity.getId(), secondEntity.getId());
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    void shouldDeleteById() {
        UUID reservationId = UUID.randomUUID();

        adapter.deleteById(reservationId);

        verify(reservationRepository).deleteById(reservationId);
    }
}
