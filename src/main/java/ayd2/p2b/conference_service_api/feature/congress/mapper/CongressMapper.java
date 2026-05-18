package ayd2.p2b.conference_service_api.feature.congress.mapper;

import ayd2.p2b.conference_service_api.feature.congress.domain.model.Congress;
import ayd2.p2b.conference_service_api.feature.congress.dto.response.CongressResponse;
import ayd2.p2b.conference_service_api.feature.congress.infrastructure.persistence.entity.CongressEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CongressMapper {
    Congress toDomain(CongressEntity entity);

    CongressEntity toEntity(Congress congress);

    @Mapping(target = "institutionName", source = "institutionName")
    CongressResponse toResponse(Congress congress, String institutionName);
}
