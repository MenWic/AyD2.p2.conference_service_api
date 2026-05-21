package ayd2.p2b.conference_service_api.feature.activity.mapper;

import ayd2.p2b.conference_service_api.feature.activity.domain.model.Activity;
import ayd2.p2b.conference_service_api.feature.activity.dto.response.ActivityResponse;
import ayd2.p2b.conference_service_api.feature.activity.infrastructure.persistence.entity.ActivityEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ActivityMapper {
    Activity toDomain(ActivityEntity entity);

    @Mapping(target = "congress", ignore = true)
    @Mapping(target = "room", ignore = true)
    ActivityEntity toEntity(Activity activity);

    @Mapping(target = "leaders", ignore = true)
    ActivityResponse toResponse(Activity activity);
}
