package ayd2.p2b.conference_service_api.feature.committee.mapper;

import ayd2.p2b.conference_service_api.feature.committee.domain.model.CommitteeMember;
import ayd2.p2b.conference_service_api.feature.committee.dto.response.CommitteeMemberResponse;
import ayd2.p2b.conference_service_api.feature.committee.infrastructure.persistence.entity.CommitteeMemberEntity;
import ayd2.p2b.conference_service_api.integration.dto.IamCommitteeCandidateSummary;
import ayd2.p2b.conference_service_api.integration.dto.IamUserSummary;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CommitteeMapper {
    CommitteeMember toDomain(CommitteeMemberEntity entity);

    @Mapping(target = "congress", ignore = true)
    CommitteeMemberEntity toEntity(CommitteeMember member);

    @Mapping(target = "fullName", source = "summary.fullName")
    @Mapping(target = "email", source = "summary.email")
    CommitteeMemberResponse toResponse(CommitteeMember member, IamUserSummary summary);

    @Mapping(target = "congressId", source = "member.congressId")
    @Mapping(target = "userId", source = "member.userId")
    @Mapping(target = "addedAt", source = "member.addedAt")
    @Mapping(target = "fullName", source = "summary.fullName")
    @Mapping(target = "email", source = "summary.email")
    CommitteeMemberResponse toResponse(CommitteeMember member, IamCommitteeCandidateSummary summary);
}
