package com.utopios.taskforge.repository;

import com.utopios.taskforge.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByOwner(String owner);

    // A05 - recherche par requete derivee (parametree) + filtre proprietaire (A01).
    List<Task> findByOwnerAndTitleContainingIgnoreCase(String owner, String title);
}
