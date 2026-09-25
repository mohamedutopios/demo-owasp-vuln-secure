package com.utopios.taskforge.web;

import com.utopios.taskforge.model.Comment;
import com.utopios.taskforge.model.Task;
import com.utopios.taskforge.repository.CommentRepository;
import com.utopios.taskforge.repository.TaskRepository;
import com.utopios.taskforge.security.PrefsCodec;
import com.utopios.taskforge.security.UrlSafety;
import com.utopios.taskforge.service.Preferences;
import com.utopios.taskforge.service.TaskSearchService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Controller
public class TaskController {

    private static final Logger AUDIT = LoggerFactory.getLogger("taskforge.audit");

    private final TaskRepository tasks;
    private final CommentRepository comments;
    private final TaskSearchService searchService;
    private final PrefsCodec prefsCodec;

    public TaskController(TaskRepository tasks, CommentRepository comments,
                          TaskSearchService searchService, PrefsCodec prefsCodec) {
        this.tasks = tasks;
        this.comments = comments;
        this.searchService = searchService;
        this.prefsCodec = prefsCodec;
    }

    private static boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private Preferences loadPrefs(HttpServletRequest req) {
        if (req.getCookies() != null) {
            for (Cookie c : req.getCookies()) {
                if ("prefs".equals(c.getName())) {
                    return prefsCodec.decode(c.getValue());   // A08 - JSON signe verifie
                }
            }
        }
        return new Preferences();
    }

    // A01 - controle d'acces au niveau objet (anti-IDOR/BOLA).
    private Task ownedTaskOr403(Long id, Authentication auth) {
        Task task = tasks.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!task.getOwner().equals(auth.getName()) && !isAdmin(auth)) {
            AUDIT.warn("acces_objet_refuse user={} task={}", auth.getName(), id);
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return task;
    }

    @GetMapping("/")
    public String index() {
        return "redirect:/tasks";
    }

    @GetMapping("/tasks")
    public String list(Authentication auth, HttpServletRequest req, Model model) {
        model.addAttribute("tasks", tasks.findByOwner(auth.getName()));
        model.addAttribute("user", auth.getName());
        model.addAttribute("admin", isAdmin(auth));
        model.addAttribute("prefs", loadPrefs(req));
        return "tasks";
    }

    @GetMapping("/tasks/search")
    public String search(@RequestParam(required = false) String q,
                         Authentication auth, HttpServletRequest req, Model model) {
        model.addAttribute("tasks", searchService.search(auth.getName(), q));
        model.addAttribute("user", auth.getName());
        model.addAttribute("admin", isAdmin(auth));
        model.addAttribute("prefs", loadPrefs(req));
        model.addAttribute("q", q);
        return "tasks";
    }

    @GetMapping("/tasks/{id}")
    public String detail(@PathVariable Long id, Authentication auth, Model model) {
        Task task = ownedTaskOr403(id, auth);
        model.addAttribute("task", task);
        model.addAttribute("comments", comments.findByTaskId(id));
        model.addAttribute("user", auth.getName());
        return "task_detail";
    }

    @PostMapping("/tasks/{id}/comment")
    public String comment(@PathVariable Long id, @RequestParam String body, Authentication auth) {
        ownedTaskOr403(id, auth);
        Comment c = new Comment();
        c.setTaskId(id);
        c.setAuthor(auth.getName());
        // A05 - persistance parametree (JPA) ; l'echappement XSS est assure par le gabarit.
        c.setBody(body);
        comments.save(c);
        return "redirect:/tasks/" + id;
    }

    @PostMapping("/tasks/{id}/preview")
    public String preview(@PathVariable Long id, @RequestParam String url,
                          Authentication auth, Model model) {
        ownedTaskOr403(id, auth);
        // A01 (SSRF) - schema http/https uniquement, blocage des plages internes.
        if (!UrlSafety.isSafe(url)) {
            AUDIT.warn("ssrf_refuse user={} url={}", auth.getName(), url);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        String content;
        try {
            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NEVER)   // pas de rebond vers l'interne
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .header("User-Agent", "TaskForge-preview")
                    .GET().build();
            HttpResponse<Void> resp = client.send(request, HttpResponse.BodyHandlers.discarding());
            // A05 - on n'affiche pas le HTML distant, seulement des metadonnees sures.
            content = "Type: " + resp.headers().firstValue("content-type").orElse("inconnu");
        } catch (Exception e) {
            // A10 - erreur geree, message generique.
            content = "indisponible";
        }
        model.addAttribute("content", content);
        model.addAttribute("target", url);
        return "preview";
    }

    @PostMapping("/prefs")
    public String savePrefs(@RequestParam(required = false) String theme,
                            HttpServletResponse resp) {
        Preferences p = new Preferences();
        p.setTheme("sombre".equals(theme) ? "sombre" : "clair");
        Cookie c = new Cookie("prefs", prefsCodec.encode(p));   // A08 - cookie signe
        c.setHttpOnly(true);
        c.setPath("/");
        resp.addCookie(c);
        return "redirect:/tasks";
    }
}
