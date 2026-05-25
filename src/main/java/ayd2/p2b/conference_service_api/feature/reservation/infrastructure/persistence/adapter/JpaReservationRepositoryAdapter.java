package ayd2.p2b.conference_service_api.feature.reservation.infrastructure.persistence.adapter;

import ayd2.p2b.conference_service_api.feature.reservation.application.port.ReservationRepositoryPort;
import ayd2.p2b.conference_service_api.feature.reservation.domain.model.Reservation;
import ayd2.p2b.conference_service_api.feature.reservation.infrastructure.persistence.repository.ReservationRepository;
import ayd2.p2b.conference_service_api.feature.reservation.mapper.ReservationMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class JpaReservationRepositoryAdapter implements ReservationRepositoryPort {

    private final ReservationRepository reservationRepository;
    private final ReservationMapper reservationMapper;

    public JpaReservationRepositoryAdapter(
            ReservationRepository reservationRepository,
            ReservationMapper reservationMapper
    ) {
        this.reservationRepository = reservationRepository;
        this.reservationMapper = reservationMapper;
    }

    @Override
    public Reservation save(Reservation reservation) {
        return reservationMapper.toDomain(
                reservationRepository.save(reservationMapper.toEntity(reservation))
        );
    }

    @Override
    public Optional<Reservation> findById(UUID reservationId) {
        return reservationRepository.findById(reservationId).map(reservationMapper::toDomain);
    }

    @Override
    public boolean existsByActivityIdAndUserId(UUID activityId, UUID userId) {
        return reservationRepository.existsByActivityIdAndUserId(activityId, userId);
    }

    @Override
    public long countByActivityId(UUID activityId) {
        return reservationRepository.countByActivityId(activityId);
    }

    @Override
    public Page<Reservation> findByActivityId(UUID activityId, Pageable pageable) {
        return reservationRepository.findByActivityId(activityId, pageable).map(reservationMapper::toDomain);
    }

    @Override
    public Page<Reservation> findByUserId(UUID userId, Pageable pageable) {
        return reservationRepository.findByUserId(userId, pageable).map(reservationMapper::toDomain);
    }

    @Override
    public void deleteById(UUID reservationId) {
        reservationRepository.deleteById(reservationId);
    }
}
