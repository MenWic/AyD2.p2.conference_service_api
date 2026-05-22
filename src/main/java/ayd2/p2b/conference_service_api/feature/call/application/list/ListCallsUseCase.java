package ayd2.p2b.conference_service_api.feature.call.application.list;

import ayd2.p2b.conference_service_api.common.response.PageResponse;
import ayd2.p2b.conference_service_api.feature.call.application.exception.CallExceptions;
import ayd2.p2b.conference_service_api.feature.call.application.port.CallCongressPort;
import ayd2.p2b.conference_service_api.feature.call.application.port.CallRepositoryPort;
import ayd2.p2b.conference_service_api.feature.call.dto.response.CallResponse;
import ayd2.p2b.conference_service_api.feature.call.mapper.CallMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component
@Transactional(readOnly = true)
public class ListCallsUseCase {

    private final CallRepositoryPort callRepositoryPort;
    private final CallCongressPort callCongressPort;
    private final CallMapper callMapper;

    public ListCallsUseCase(
            CallRepositoryPort callRepositoryPort,
            CallCongressPort callCongressPort,
            CallMapper callMapper
    ) {
        this.callRepositoryPort = callRepositoryPort;
        this.callCongressPort = callCongressPort;
        this.callMapper = callMapper;
    }

    public PageResponse<CallResponse> execute(UUID congressId, Pageable pageable) {
        if (!callCongressPort.existsPublicCongressById(congressId)) {
            throw CallExceptions.congressNotFound(congressId);
        }

        Page<CallResponse> page = callRepositoryPort.findPublicByCongressId(congressId, pageable)
                .map(callMapper::toResponse);

        return PageResponse.<CallResponse>builder()
                .items(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalItems(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();
    }
}
