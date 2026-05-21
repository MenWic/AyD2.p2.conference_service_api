package ayd2.p2b.conference_service_api.feature.activity.infrastructure.persistence.adapter;

import ayd2.p2b.conference_service_api.feature.activity.application.port.ActivityDependencyPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class JpaActivityDependencyAdapter implements ActivityDependencyPort {

    private final EntityManager entityManager;

    public JpaActivityDependencyAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public List<String> findBlockingDependencies(UUID activityId) {
        Map<String, String> checks = new LinkedHashMap<>();
        checks.put("reservations.activity_id", "reservations");
        checks.put("attendances.activity_id", "attendances");
        checks.put("diplomas.activity_id", "diplomas");
        checks.put("proposals.created_activity_id", "proposals");

        List<String> blockers = new ArrayList<>();
        for (Map.Entry<String, String> check : checks.entrySet()) {
            if (existsByColumn(check.getKey(), activityId)) {
                blockers.add(check.getValue());
            }
        }
        return blockers;
    }

    private boolean existsByColumn(String tableWithColumn, UUID activityId) {
        String[] parts = tableWithColumn.split("\\.");
        String table = parts[0];
        String column = parts[1];
        String sql = "select exists(select 1 from " + table + " t where t." + column + " = :activityId)";
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("activityId", activityId);
        Object value = query.getSingleResult();
        return Boolean.TRUE.equals(value);
    }
}
