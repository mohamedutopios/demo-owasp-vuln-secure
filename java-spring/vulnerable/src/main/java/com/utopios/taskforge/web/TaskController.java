package com.utopios.taskforge.web;

import com.utopios.taskforge.model.Comment;
import com.utopios.taskforge.model.Task;
import com.utopios.taskforge.repository.CommentRepository;
import com.utopios.taskforge.repository.TaskRepository;
import com.utopios.taskforge.service.Preferences;
import com.utopios.taskforge.service.TaskSearchService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.util.Base64;
import java.util.List;

@Controller
public class TaskController {

    private final TaskRepository tasks;
    private final CommentRepository comments;
    private final TaskSearchService searchService;

    public TaskController(TaskRepository tasks, CommentRepository comments,
                          TaskSearchService searchService) {
        this.tasks = tasks;
        this.comments = comments;
        this.searchService = searchService;
    }

    private Preferences loadPrefs(HttpServletRequest req) {
        Preferences p = new Preferences();
        try {
            if (req.getCookies() != null) {
                for (Cookie c : req.getCookies()) {
                    if ("prefs".equals(c.getName())) {
                        byte[] data = Base64.getDecoder().decode(c.getValue());
                        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
                        return (Preferences) ois.readObject();
                    }
                }
            }
        } catch (Exception e) {
        }
        return p;
    }

    @GetMapping("/tasks")
    public String list(HttpSession session, HttpServletRequest req, Model model) {
        String owner = (String) session.getAttribute("user");
        model.addAttribute("tasks", tasks.findByOwner(owner));
        model.addAttribute("user", owner);
        model.addAttribute("prefs", loadPrefs(req));
        return "tasks";
    }

    @GetMapping("/tasks/search")
    public String search(@RequestParam String q, HttpSession session, Model model) {
        List<Task> results = searchService.search(q);
        model.addAttribute("tasks", results);
        model.addAttribute("user", session.getAttribute("user"));
        model.addAttribute("q", q);
        return "tasks";
    }

    @GetMapping("/tasks/{id}")
    public String detail(@PathVariable Long id, HttpSession session, Model model) {
        Task task = tasks.findById(id).orElse(null);
        model.addAttribute("task", task);
        model.addAttribute("comments", comments.findByTaskId(id));
        model.addAttribute("user", session.getAttribute("user"));
        return "task_detail";
    }

    @PostMapping("/tasks/{id}/comment")
    public String comment(@PathVariable Long id, @RequestParam String body, HttpSession session) {
        Comment c = new Comment();
        c.setTaskId(id);
        c.setAuthor((String) session.getAttribute("user"));
        c.setBody(body);
        comments.save(c);
        return "redirect:/tasks/" + id;
    }

    @PostMapping("/tasks/{id}/preview")
    public String preview(@PathVariable Long id, @RequestParam String url, Model model) {
        String content = "";
        try {
            content = new RestTemplate().getForObject(url, String.class);
            if (content != null && content.length() > 2000) {
                content = content.substring(0, 2000);
            }
        } catch (Exception e) {
        }
        model.addAttribute("content", content);
        model.addAttribute("target", url);
        return "preview";
    }
}
