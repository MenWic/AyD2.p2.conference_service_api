package ayd2.p2b.conference_service_api.feature.institution.application.port;

import java.util.UUID;

public interface InstitutionDependencyPort {
    boolean hasCongressesByInstitutionId(UUID institutionId);
}
