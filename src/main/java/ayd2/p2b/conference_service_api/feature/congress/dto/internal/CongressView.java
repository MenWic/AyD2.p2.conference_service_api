package ayd2.p2b.conference_service_api.feature.congress.dto.internal;

import ayd2.p2b.conference_service_api.feature.congress.domain.model.Congress;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CongressView {
    Congress congress;
    String institutionName;
}
