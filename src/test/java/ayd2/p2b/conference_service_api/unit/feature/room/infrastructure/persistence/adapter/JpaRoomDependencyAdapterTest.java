package ayd2.p2b.conference_service_api.unit.feature.room.infrastructure.persistence.adapter;

import ayd2.p2b.conference_service_api.feature.room.infrastructure.persistence.adapter.JpaRoomDependencyAdapter;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JpaRoomDependencyAdapterTest {

    @Mock
    private EntityManager entityManager;
    @Mock
    private Query query;

    private JpaRoomDependencyAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JpaRoomDependencyAdapter(entityManager);
    }

    @Test
    void shouldReturnTrueWhenActivitiesExistForRoom() {
        UUID roomId = UUID.randomUUID();

        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter("roomId", roomId)).thenReturn(query);
        when(query.getSingleResult()).thenReturn(Boolean.TRUE);

        assertThat(adapter.existsActivitiesForRoom(roomId)).isTrue();
    }

    @Test
    void shouldReturnFalseWhenActivitiesDoNotExistForRoom() {
        UUID roomId = UUID.randomUUID();

        when(entityManager.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter("roomId", roomId)).thenReturn(query);
        when(query.getSingleResult()).thenReturn(Boolean.FALSE);

        assertThat(adapter.existsActivitiesForRoom(roomId)).isFalse();
    }
}
