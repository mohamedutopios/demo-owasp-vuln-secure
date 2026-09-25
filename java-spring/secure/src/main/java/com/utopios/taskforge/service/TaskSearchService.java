package com.utopios.taskforge.service;

import com.utopios.taskforge.model.Task;
import com.utopios.taskforge.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TaskSearchService {

    private final TaskRepository tasks;

    public TaskSearchService(TaskRepository tasks) {
        this.tasks = tasks;
    }

    // A05 - aucune concatenation SQL : requete derivee parametree par Spring Data.
    // A01 - le resultat est restreint aux taches du proprietaire courant.
    public List<Task> search(String owner, String q) {
        return tasks.findByOwnerAndTitleContainingIgnoreCase(owner, q == null ? "" : q);
    }
}
