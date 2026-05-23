package ayd2.p2b.conference_service_api.feature.proposal.mapper;

import ayd2.p2b.conference_service_api.feature.proposal.domain.model.Proposal;
import ayd2.p2b.conference_service_api.feature.proposal.dto.response.ProposalResponse;
import ayd2.p2b.conference_service_api.feature.proposal.infrastructure.persistence.entity.ProposalEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ProposalMapper {
    Proposal toDomain(ProposalEntity entity);

    @Mapping(target = "call", ignore = true)
    ProposalEntity toEntity(Proposal proposal);

    ProposalResponse toResponse(Proposal proposal);
}
