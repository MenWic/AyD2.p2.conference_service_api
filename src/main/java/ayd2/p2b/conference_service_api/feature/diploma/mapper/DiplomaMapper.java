package ayd2.p2b.conference_service_api.feature.diploma.mapper;

import ayd2.p2b.conference_service_api.feature.diploma.domain.model.Diploma;
import ayd2.p2b.conference_service_api.feature.diploma.dto.response.DiplomaResponse;
import ayd2.p2b.conference_service_api.feature.diploma.infrastructure.persistence.entity.DiplomaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DiplomaMapper {

    Diploma toDomain(DiplomaEntity entity);

    @Mapping(target = "congress", ignore = true)
    @Mapping(target = "activity", ignore = true)
    DiplomaEntity toEntity(Diploma diploma);

    default DiplomaResponse toResponse(Diploma diploma, String congressName, String activityName) {
        if (diploma == null) {
            return null;
        }
        return DiplomaResponse.builder()
                .id(diploma.getId())
                .userId(diploma.getUserId())
                .congressId(diploma.getCongressId())
                .type(diploma.getType())
                .activityId(diploma.getActivityId())
                .issuedAt(diploma.getIssuedAt())
                .congressName(congressName)
                .activityName(activityName)
                .available(true)
                .build();
    }
}
