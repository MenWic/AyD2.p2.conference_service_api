package ayd2.p2b.conference_service_api.feature.congress.application.list;

import ayd2.p2b.conference_service_api.common.response.PageResponse;
import ayd2.p2b.conference_service_api.feature.congress.application.port.CongressRepositoryPort;
import ayd2.p2b.conference_service_api.feature.congress.dto.internal.CongressSearchCriteria;
import ayd2.p2b.conference_service_api.feature.congress.dto.response.CongressResponse;
import ayd2.p2b.conference_service_api.feature.congress.mapper.CongressMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
public class ListCongressesUseCase {

    private final CongressRepositoryPort congressRepositoryPort;
    private final CongressMapper congressMapper;

    public ListCongressesUseCase(
            CongressRepositoryPort congressRepositoryPort,
            CongressMapper congressMapper
    ) {
        this.congressRepositoryPort = congressRepositoryPort;
        this.congressMapper = congressMapper;
    }

    public PageResponse<CongressResponse> execute(CongressSearchCriteria criteria, Pageable pageable) {
        Page<CongressResponse> page = congressRepositoryPort.findPublicByCriteria(criteria, pageable)
                .map(view -> congressMapper.toResponse(view.getCongress(), view.getInstitutionName()));

        return PageResponse.<CongressResponse>builder()
                .items(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalItems(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }
}
