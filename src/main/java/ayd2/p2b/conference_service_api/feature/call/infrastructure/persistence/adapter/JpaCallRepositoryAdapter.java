package ayd2.p2b.conference_service_api.feature.call.infrastructure.persistence.adapter;

import ayd2.p2b.conference_service_api.feature.call.application.port.CallRepositoryPort;
import ayd2.p2b.conference_service_api.feature.call.domain.model.Call;
import ayd2.p2b.conference_service_api.feature.call.domain.model.CallStatus;
import ayd2.p2b.conference_service_api.feature.call.infrastructure.persistence.repository.CallRepository;
import ayd2.p2b.conference_service_api.feature.call.mapper.CallMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class JpaCallRepositoryAdapter implements CallRepositoryPort {

    private final CallRepository callRepository;
    private final CallMapper callMapper;

    public JpaCallRepositoryAdapter(
            CallRepository callRepository,
            CallMapper callMapper
    ) {
        this.callRepository = callRepository;
        this.callMapper = callMapper;
    }

    @Override
    public Call save(Call call) {
        return callMapper.toDomain(callRepository.save(callMapper.toEntity(call)));
    }

    @Override
    public Optional<Call> findById(UUID callId) {
        return callRepository.findById(callId).map(callMapper::toDomain);
    }

    @Override
    public Page<Call> findPublicByCongressId(UUID congressId, Pageable pageable) {
        return callRepository.findPublicByCongressId(congressId, pageable)
                .map(callMapper::toDomain);
    }

    @Override
    public boolean existsOpenCallByCongressId(UUID congressId) {
        return callRepository.existsByCongressIdAndStatus(congressId, CallStatus.OPEN);
    }
}
