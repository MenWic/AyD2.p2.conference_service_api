package ayd2.p2b.conference_service_api.unit.feature.congress.infrastructure.persistence.adapter;

import ayd2.p2b.conference_service_api.feature.congress.infrastructure.persistence.adapter.JpaCongressDeletionGuardAdapter;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaCongressDeletionGuardAdapterTest {

    @Mock
    private EntityManager entityManager;
    @Mock
    private Query query;

    private JpaCongressDeletionGuardAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaCongressDeletionGuardAdapter(entityManager);
    }

    @Test
    void shouldReturnEmptyWhenAllDependencyChecksAreFalse() {
        UUID congressId = UUID.randomUUID();

        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter("congressId", congressId)).thenReturn(query);
        when(query.getSingleResult())
                .thenReturn(Boolean.FALSE)
                .thenReturn(Boolean.FALSE)
                .thenReturn(Boolean.FALSE)
                .thenReturn(Boolean.FALSE)
                .thenReturn(Boolean.FALSE);

        List<String> result = adapter.findBlockingDependencies(congressId);

        assertThat(result).isEmpty();
        verify(query, times(5)).setParameter("congressId", congressId);
    }

    @Test
    void shouldReturnOnlyDependencyNamesWhoseChecksAreTrue() {
        UUID congressId = UUID.randomUUID();

        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter("congressId", congressId)).thenReturn(query);
        when(query.getSingleResult())
                .thenReturn(Boolean.TRUE)   // rooms
                .thenReturn(Boolean.FALSE)  // activities
                .thenReturn(Boolean.TRUE)   // calls
                .thenReturn(Boolean.FALSE)  // committee_members
                .thenReturn(Boolean.TRUE);  // enrollments

        List<String> result = adapter.findBlockingDependencies(congressId);

        assertThat(result).containsExactly("rooms", "calls", "enrollments");
        assertThat(result).doesNotContain("activities", "committee_members");
        verify(query, times(5)).setParameter("congressId", congressId);
    }

    @Test
    void shouldGenerateQueriesAgainstExpectedTables() {
        UUID congressId = UUID.randomUUID();

        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter("congressId", congressId)).thenReturn(query);
        when(query.getSingleResult())
                .thenReturn(Boolean.FALSE)
                .thenReturn(Boolean.FALSE)
                .thenReturn(Boolean.FALSE)
                .thenReturn(Boolean.FALSE)
                .thenReturn(Boolean.FALSE);

        adapter.findBlockingDependencies(congressId);

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(entityManager, times(5)).createNativeQuery(sqlCaptor.capture());
        String allSql = String.join("\n", sqlCaptor.getAllValues()).toLowerCase();
        assertThat(allSql).contains("from rooms");
        assertThat(allSql).contains("from activities");
        assertThat(allSql).contains("from calls");
        assertThat(allSql).contains("from committee_members");
        assertThat(allSql).contains("from enrollments");
        assertThat(allSql).contains("congress_id");
    }
}
