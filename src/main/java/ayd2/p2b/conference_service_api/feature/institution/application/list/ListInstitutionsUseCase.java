package ayd2.p2b.conference_service_api.feature.institution.application.list;

import ayd2.p2b.conference_service_api.common.response.PageResponse;
import ayd2.p2b.conference_service_api.feature.institution.application.port.InstitutionRepositoryPort;
import ayd2.p2b.conference_service_api.feature.institution.dto.response.InstitutionResponse;
import ayd2.p2b.conference_service_api.feature.institution.mapper.InstitutionMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
public class ListInstitutionsUseCase {

    private final InstitutionRepositoryPort institutionRepositoryPort;
    private final InstitutionMapper institutionMapper;

    public ListInstitutionsUseCase(
            InstitutionRepositoryPort institutionRepositoryPort,
            InstitutionMapper institutionMapper
    ) {
        this.institutionRepositoryPort = institutionRepositoryPort;
        this.institutionMapper = institutionMapper;
    }

    public PageResponse<InstitutionResponse> execute(Pageable pageable) {
        Page<InstitutionResponse> page = institutionRepositoryPort.findAllActive(pageable)
                .map(institutionMapper::toResponse);

        return PageResponse.<InstitutionResponse>builder()
                .items(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalItems(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }
}
