package ayd2.p2b.conference_service_api.feature.reservation.mapper;

import ayd2.p2b.conference_service_api.feature.reservation.domain.model.Reservation;
import ayd2.p2b.conference_service_api.feature.reservation.dto.response.ReservationResponse;
import ayd2.p2b.conference_service_api.feature.reservation.infrastructure.persistence.entity.ReservationEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReservationMapper {

    Reservation toDomain(ReservationEntity entity);

    @Mapping(target = "activity", ignore = true)
    ReservationEntity toEntity(Reservation reservation);

    ReservationResponse toResponse(Reservation reservation);
}
