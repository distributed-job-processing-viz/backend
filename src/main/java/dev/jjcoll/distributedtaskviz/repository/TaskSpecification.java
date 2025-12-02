package dev.jjcoll.distributedtaskviz.repository;

import dev.jjcoll.distributedtaskviz.model.Complexity;
import dev.jjcoll.distributedtaskviz.model.Task;
import dev.jjcoll.distributedtaskviz.model.TaskStatus;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;

import java.util.ArrayList;
import java.util.List;

public class TaskSpecification {

    /**
     Internally it builds:
     - If both are null: No WHERE clause (returns all)
     - If only complexity: WHERE complexity = ?
     - If only status: WHERE status = ?
     - If both: WHERE complexity = ? AND status = ?
     */

    public static Specification<Task> getSpecification(TaskStatus taskStatus, Complexity complexity) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (complexity != null) {
                predicates.add(criteriaBuilder.equal(root.get("complexity"), complexity));
            }


            if (taskStatus != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), taskStatus));
            }

            // Combine all predicates with AND logic
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
