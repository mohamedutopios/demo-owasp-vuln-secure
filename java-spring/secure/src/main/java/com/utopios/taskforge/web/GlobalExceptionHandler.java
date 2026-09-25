package com.utopios.taskforge.web;

import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

/**
 * A10 - les conditions exceptionnelles sont gerees explicitement : on echoue de
 * maniere sure et on ne divulgue aucune trace au client (page generique).
 * A09 - toute erreur inattendue est journalisee cote serveur.
 */
@ControllerAdvice
class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger("taskforge.audit");

    // Statuts explicites leves par les controleurs (403/404/400) : page generique.
    @ExceptionHandler(ResponseStatusException.class)
    public String handleStatus(ResponseStatusException ex, Model model, HttpServletResponse resp) {
        resp.setStatus(ex.getStatusCode().value());
        model.addAttribute("status", ex.getStatusCode().value());
        return "error";
    }

    // Toute autre exception : 500 generique, trace uniquement dans les journaux.
    @ExceptionHandler(Exception.class)
    public String handleUnexpected(Exception ex, Model model, HttpServletResponse resp) {
        LOG.error("erreur_non_geree", ex);
        resp.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        model.addAttribute("status", 500);
        return "error";
    }
}
