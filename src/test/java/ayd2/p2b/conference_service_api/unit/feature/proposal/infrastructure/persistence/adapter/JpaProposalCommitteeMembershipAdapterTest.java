package ayd2.p2b.conference_service_api.unit.feature.proposal.infrastructure.persistence.adapter;

import ayd2.p2b.conference_service_api.feature.proposal.infrastructure.persistence.adapter.JpaProposalCommitteeMembershipAdapter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaProposalCommitteeMembershipAdapterTest {

    @Mock
    private EntityManager entityManager;
    @Mock
    private Query query;

    private JpaProposalCommitteeMembershipAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaProposalCommitteeMembershipAdapter(entityManager);
    }

    @Test
    void shouldReturnTrueWhenCommitteeMembershipExists() {
        UUID congressId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter("congressId", congressId)).thenReturn(query);
        when(query.setParameter("userId", userId)).thenReturn(query);
        when(query.getSingleResult()).thenReturn(Boolean.TRUE);

        boolean result = adapter.existsByCongressIdAndUserId(congressId, userId);

        assertThat(result).isTrue();
        verify(query).setParameter("congressId", congressId);
        verify(query).setParameter("userId", userId);
    }

    @Test
    void shouldReturnFalseWhenCommitteeMembershipDoesNotExist() {
        UUID congressId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter("congressId", congressId)).thenReturn(query);
        when(query.setParameter("userId", userId)).thenReturn(query);
        when(query.getSingleResult()).thenReturn(Boolean.FALSE);

        boolean result = adapter.existsByCongressIdAndUserId(congressId, userId);

        assertThat(result).isFalse();
    }

    @Test
    void shouldReturnFalseWhenQueryResultIsNullOrNonBoolean() {
        UUID congressId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter("congressId", congressId)).thenReturn(query);
        when(query.setParameter("userId", userId)).thenReturn(query);
        when(query.getSingleResult()).thenReturn((Object) null).thenReturn("true");

        assertThat(adapter.existsByCongressIdAndUserId(congressId, userId)).isFalse();
        assertThat(adapter.existsByCongressIdAndUserId(congressId, userId)).isFalse();
    }
}
