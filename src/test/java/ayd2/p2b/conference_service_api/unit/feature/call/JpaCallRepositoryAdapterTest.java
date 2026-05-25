package ayd2.p2b.conference_service_api.unit.feature.call;

import ayd2.p2b.conference_service_api.feature.call.domain.model.Call;
import ayd2.p2b.conference_service_api.feature.call.domain.model.CallStatus;
import ayd2.p2b.conference_service_api.feature.call.infrastructure.persistence.adapter.JpaCallRepositoryAdapter;
import ayd2.p2b.conference_service_api.feature.call.infrastructure.persistence.entity.CallEntity;
import ayd2.p2b.conference_service_api.feature.call.infrastructure.persistence.repository.CallRepository;
import ayd2.p2b.conference_service_api.feature.call.mapper.CallMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class JpaCallRepositoryAdapterTest {

    @Mock
    private CallRepository callRepository;
    @Mock
    private CallMapper callMapper;

    private JpaCallRepositoryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaCallRepositoryAdapter(callRepository, callMapper);
    }

    @Test
    void save_delegates_to_repository_and_mapper() {
        Call call = Call.builder().id(UUID.randomUUID()).congressId(UUID.randomUUID())
                .status(CallStatus.OPEN).createdBy(UUID.randomUUID()).build();
        CallEntity entity = new CallEntity();
        CallEntity saved = new CallEntity();
        Call expected = call;
        given(callMapper.toEntity(call)).willReturn(entity);
        given(callRepository.save(entity)).willReturn(saved);
        given(callMapper.toDomain(saved)).willReturn(expected);

        Call result = adapter.save(call);

        assertThat(result).isEqualTo(expected);
        verify(callRepository).save(entity);
    }

    @Test
    void findById_returns_mapped_domain_when_present() {
        UUID callId = UUID.randomUUID();
        CallEntity entity = new CallEntity();
        Call expected = Call.builder().id(callId).congressId(UUID.randomUUID())
                .status(CallStatus.OPEN).createdBy(UUID.randomUUID()).build();
        given(callRepository.findById(callId)).willReturn(Optional.of(entity));
        given(callMapper.toDomain(entity)).willReturn(expected);

        Optional<Call> result = adapter.findById(callId);

        assertThat(result).isPresent().contains(expected);
    }

    @Test
    void findById_returns_empty_when_not_found() {
        UUID callId = UUID.randomUUID();
        given(callRepository.findById(callId)).willReturn(Optional.empty());

        Optional<Call> result = adapter.findById(callId);

        assertThat(result).isEmpty();
    }

    @Test
    void findPublicByCongressId_returns_mapped_page() {
        UUID congressId = UUID.randomUUID();
        Pageable pageable = Pageable.unpaged();
        CallEntity entity = new CallEntity();
        Call call = Call.builder().id(UUID.randomUUID()).congressId(congressId)
                .status(CallStatus.OPEN).createdBy(UUID.randomUUID()).build();
        Page<CallEntity> entityPage = new PageImpl<>(List.of(entity));
        given(callRepository.findPublicByCongressId(congressId, pageable)).willReturn(entityPage);
        given(callMapper.toDomain(entity)).willReturn(call);

        Page<Call> result = adapter.findPublicByCongressId(congressId, pageable);

        assertThat(result.getContent()).containsExactly(call);
    }

    @Test
    void existsOpenCallByCongressId_delegates_correctly() {
        UUID congressId = UUID.randomUUID();
        given(callRepository.existsByCongressIdAndStatus(congressId, CallStatus.OPEN)).willReturn(true);

        boolean result = adapter.existsOpenCallByCongressId(congressId);

        assertThat(result).isTrue();
        verify(callRepository).existsByCongressIdAndStatus(congressId, CallStatus.OPEN);
    }
}
