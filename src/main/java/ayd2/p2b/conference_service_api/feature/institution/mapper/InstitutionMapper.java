package ayd2.p2b.conference_service_api.feature.institution.mapper;

import ayd2.p2b.conference_service_api.feature.institution.domain.model.Institution;
import ayd2.p2b.conference_service_api.feature.institution.dto.response.InstitutionResponse;
import ayd2.p2b.conference_service_api.feature.institution.infrastructure.persistence.entity.InstitutionEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface InstitutionMapper {
    Institution toDomain(InstitutionEntity entity);

    InstitutionEntity toEntity(Institution institution);

    InstitutionResponse toResponse(Institution institution);
}
