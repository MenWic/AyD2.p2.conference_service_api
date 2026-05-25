package ayd2.p2b.conference_service_api.unit.feature.call.infrastructure.persistence.adapter;

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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    void shouldSaveCallThroughRepositoryAndMapper() {
        Call call = sampleCall();
        CallEntity entity = new CallEntity();
        CallEntity savedEntity = new CallEntity();
        savedEntity.setId(call.getId());

        when(callMapper.toEntity(call)).thenReturn(entity);
        when(callRepository.save(entity)).thenReturn(savedEntity);
        when(callMapper.toDomain(savedEntity)).thenReturn(call);

        Call result = adapter.save(call);

        assertThat(result).isEqualTo(call);
    }

    @Test
    void shouldFindByIdWhenPresent() {
        UUID callId = UUID.randomUUID();
        CallEntity entity = new CallEntity();
        Call domain = sampleCall();

        when(callRepository.findById(callId)).thenReturn(Optional.of(entity));
        when(callMapper.toDomain(entity)).thenReturn(domain);

        assertThat(adapter.findById(callId)).contains(domain);
    }

    @Test
    void shouldReturnEmptyWhenFindByIdIsMissing() {
        UUID callId = UUID.randomUUID();
        when(callRepository.findById(callId)).thenReturn(Optional.empty());

        assertThat(adapter.findById(callId)).isEmpty();
    }

    @Test
    void shouldFindPublicByCongressIdAndMapPageContent() {
        UUID congressId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 1);
        CallEntity entity = new CallEntity();
        entity.setId(UUID.randomUUID());
        Call domain = sampleCall().toBuilder().id(entity.getId()).build();

        when(callRepository.findPublicByCongressId(congressId, pageable))
                .thenReturn(new PageImpl<>(List.of(entity), pageable, 7));
        when(callMapper.toDomain(entity)).thenReturn(domain);

        Page<Call> page = adapter.findPublicByCongressId(congressId, pageable);

        assertThat(page.getContent()).containsExactly(domain);
        assertThat(page.getTotalElements()).isEqualTo(7);
    }

    @Test
    void shouldDelegateExistsOpenCallByCongressIdWithOpenStatus() {
        UUID congressId = UUID.randomUUID();
        when(callRepository.existsByCongressIdAndStatus(congressId, CallStatus.OPEN)).thenReturn(true);

        boolean exists = adapter.existsOpenCallByCongressId(congressId);

        assertThat(exists).isTrue();
        verify(callRepository).existsByCongressIdAndStatus(congressId, CallStatus.OPEN);
    }

    @Test
    void shouldReturnFalseWhenNoOpenCallExists() {
        UUID congressId = UUID.randomUUID();
        when(callRepository.existsByCongressIdAndStatus(congressId, CallStatus.OPEN)).thenReturn(false);

        boolean exists = adapter.existsOpenCallByCongressId(congressId);

        assertThat(exists).isFalse();
        verify(callRepository).existsByCongressIdAndStatus(congressId, CallStatus.OPEN);
    }

    private Call sampleCall() {
        return Call.builder()
                .id(UUID.randomUUID())
                .congressId(UUID.randomUUID())
                .status(CallStatus.OPEN)
                .openedAt(OffsetDateTime.parse("2026-10-10T10:00:00Z"))
                .createdBy(UUID.randomUUID())
                .build();
    }
}
