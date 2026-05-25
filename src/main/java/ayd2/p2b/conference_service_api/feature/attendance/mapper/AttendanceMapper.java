package ayd2.p2b.conference_service_api.feature.attendance.mapper;

import ayd2.p2b.conference_service_api.feature.attendance.domain.model.Attendance;
import ayd2.p2b.conference_service_api.feature.attendance.dto.response.AttendanceResponse;
import ayd2.p2b.conference_service_api.feature.attendance.infrastructure.persistence.entity.AttendanceEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AttendanceMapper {

    Attendance toDomain(AttendanceEntity entity);

    @Mapping(target = "activity", ignore = true)
    AttendanceEntity toEntity(Attendance attendance);

    @Mapping(target = "personalId", source = "personalIdSnapshot")
    AttendanceResponse toResponse(Attendance attendance);
}
