package ayd2.p2b.conference_service_api.feature.diploma.infrastructure.persistence.adapter;

import ayd2.p2b.conference_service_api.feature.diploma.application.port.DiplomaRepositoryPort;
import ayd2.p2b.conference_service_api.feature.diploma.domain.model.Diploma;
import ayd2.p2b.conference_service_api.feature.diploma.domain.model.DiplomaType;
import ayd2.p2b.conference_service_api.feature.diploma.dto.response.DiplomaResponse;
import ayd2.p2b.conference_service_api.feature.diploma.infrastructure.persistence.entity.DiplomaEntity;
import ayd2.p2b.conference_service_api.feature.diploma.infrastructure.persistence.repository.DiplomaRepository;
import ayd2.p2b.conference_service_api.feature.diploma.mapper.DiplomaMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class JpaDiplomaRepositoryAdapter implements DiplomaRepositoryPort {

    private final DiplomaRepository diplomaRepository;
    private final DiplomaMapper diplomaMapper;

    public JpaDiplomaRepositoryAdapter(
            DiplomaRepository diplomaRepository,
            DiplomaMapper diplomaMapper
    ) {
        this.diplomaRepository = diplomaRepository;
        this.diplomaMapper = diplomaMapper;
    }

    @Override
    public Optional<Diploma> findById(UUID diplomaId) {
        return diplomaRepository.findById(diplomaId).map(diplomaMapper::toDomain);
    }

    @Override
    public List<Diploma> findExistingForUser(UUID userId) {
        return diplomaRepository.findByUserIdOrderByIssuedAtDesc(userId).stream()
                .map(diplomaMapper::toDomain)
                .toList();
    }

    @Override
    public Page<Diploma> findPageByUser(UUID userId, Pageable pageable) {
        return diplomaRepository.findByUserId(userId, pageable)
                .map(diplomaMapper::toDomain);
    }

    @Override
    public Page<DiplomaResponse> findPageResponseByUser(UUID userId, Pageable pageable) {
        return diplomaRepository.findByUserId(userId, pageable)
                .map(this::toResponse);
    }

    @Override
    public Optional<DiplomaResponse> findResponseById(UUID diplomaId) {
        return diplomaRepository.findById(diplomaId)
                .map(this::toResponse);
    }

    @Override
    public Diploma save(Diploma diploma) {
        DiplomaEntity saved = diplomaRepository.saveAndFlush(diplomaMapper.toEntity(diploma));
        return diplomaMapper.toDomain(saved);
    }

    @Override
    public Optional<Diploma> findByUniqueKey(UUID userId, UUID congressId, DiplomaType type, UUID activityId) {
        if (activityId == null) {
            return diplomaRepository.findByUserIdAndCongressIdAndTypeAndActivityIdIsNull(userId, congressId, type)
                    .map(diplomaMapper::toDomain);
        }
        return diplomaRepository.findByUserIdAndCongressIdAndTypeAndActivityId(userId, congressId, type, activityId)
                .map(diplomaMapper::toDomain);
    }

    private DiplomaResponse toResponse(DiplomaEntity entity) {
        return diplomaMapper.toResponse(
                diplomaMapper.toDomain(entity),
                entity.getCongress() == null ? null : entity.getCongress().getName(),
                entity.getActivity() == null ? null : entity.getActivity().getName()
        );
    }
}
