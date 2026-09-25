package com.utopios.taskforge.service;

import com.utopios.taskforge.model.Task;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

@Service
public class TaskSearchService {

    @PersistenceContext
    private EntityManager em;

    @SuppressWarnings("unchecked")
    public List<Task> search(String q) {
        String sql = "SELECT * FROM tasks WHERE title LIKE '%" + q + "%'";
        return em.createNativeQuery(sql, Task.class).getResultList();
    }
}
