package ayd2.p2b.conference_service_api.feature.call.mapper;

import ayd2.p2b.conference_service_api.feature.call.domain.model.Call;
import ayd2.p2b.conference_service_api.feature.call.dto.response.CallResponse;
import ayd2.p2b.conference_service_api.feature.call.infrastructure.persistence.entity.CallEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CallMapper {
    Call toDomain(CallEntity entity);

    @Mapping(target = "congress", ignore = true)
    CallEntity toEntity(Call call);

    CallResponse toResponse(Call call);
}
