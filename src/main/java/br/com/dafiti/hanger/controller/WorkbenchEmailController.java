/*
 * Copyright (c) 2020 Dafiti Group
 * 
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * 
 */
package br.com.dafiti.hanger.controller;

import br.com.dafiti.hanger.exception.Message;
import br.com.dafiti.hanger.model.Connection;
import br.com.dafiti.hanger.model.WorkbenchEmail;
import br.com.dafiti.hanger.model.User;
import br.com.dafiti.hanger.service.ConnectionService;
import br.com.dafiti.hanger.service.WorkbenchEmailService;
import br.com.dafiti.hanger.service.UserService;
import java.io.IOException;
import java.security.Principal;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 *
 * @author Fernando Saga
 * @author Helio Leal
 */
@Controller
@RequestMapping(path = "/email")
public class WorkbenchEmailController {

    private final UserService userService;
    private final WorkbenchEmailService workbenchEmailService;
    private final ConnectionService connectionService;

    @Autowired
    public WorkbenchEmailController(
            UserService userService,
            WorkbenchEmailService workbenchEmailService,
            ConnectionService connectionService) {
        this.userService = userService;
        this.workbenchEmailService = workbenchEmailService;
        this.connectionService = connectionService;
    }

    /**
     * Open email modal.
     *
     * @param connection Connection
     * @param query String
     * @param model Model
     * @param principal Principal
     *
     * @return
     *
     */
    @PostMapping(path = "/modal/{id}")
    public String modal(
            @PathVariable(name = "id") Connection connection,
            @RequestBody(required = true) String query,
            Model model,
            Principal principal) {
        User user = userService.findByUsername(principal.getName());

        if (user != null) {
            modelDefault(model, new WorkbenchEmail(
                    connection,
                    user,
                    "Generated by hanger",
                    query));
        }
        return "workbench/email/modalEmail::email";
    }

    /**
     * Open edit email modal.
     *
     * @param workbenchEmail WorkbenchEmail
     * @param model Model
     * @return
     */
    @PostMapping(value = "/modal/edit/{id}")
    public String editModal(
            @PathVariable(name = "id") WorkbenchEmail workbenchEmail,
            Model model) {
        modelDefault(model, workbenchEmail);
        return "workbench/email/modalEmail::email";
    }

    /**
     * Save e-mail template by modal.
     *
     * @param workbenchEmail WorkbenchEmail
     * @return
     * @throws java.io.IOException
     */
    @PostMapping(path = "/modal", params = {"save"})
    public String save(@Valid @ModelAttribute WorkbenchEmail workbenchEmail)
            throws IOException {
        workbenchEmailService.save(workbenchEmail);
        return "redirect:/email/list/";
    }

    /**
     * Send query resultset in e-mail by modal.
     *
     * @param redirectAttributes RedirectAttributes
     * @param workbenchEmail WorkbenchEmail
     * @param principal Principal
     * @return
     * @throws java.io.IOException
     */
    @PostMapping(path = "/modal", params = {"send"})
    public String send(
            RedirectAttributes redirectAttributes,
            @Valid @ModelAttribute WorkbenchEmail workbenchEmail,
            Principal principal) throws IOException {
        try {
            if (workbenchEmailService.toEmail(workbenchEmail, principal)) {
                redirectAttributes.addFlashAttribute("successMessage", "Email successfully sent!");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Error sending email");
            }
        } catch (Exception exception) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    new Message().getErrorMessage(exception));
        }
        return "redirect:/workbench/workbench/";
    }

    /**
     * Send query resultset in e-mail.
     *
     * @param model Model
     * @param workbenchEmail WorkbenchEmail
     * @param principal Principal
     * @return boolean
     * @throws java.io.IOException
     */
    @GetMapping(path = "/send/{id}")
    @ResponseBody
    public boolean send(
            Model model,
            @PathVariable(name = "id") WorkbenchEmail workbenchEmail,
            Principal principal) throws IOException, Exception {
        return workbenchEmailService.toEmail(workbenchEmail, principal);
    }

    /**
     * Send query resultset in e-mail by API.
     *
     * @param workbenchEmail WorkbenchEmail
     * @param principal Principal
     * @return
     * @throws java.io.IOException
     */
    @PostMapping(path = "api/send/{id}")
    public ResponseEntity send(
            @PathVariable(name = "id") WorkbenchEmail workbenchEmail,
            Principal principal) throws IOException, Exception {

        if (workbenchEmailService.toEmail(workbenchEmail, principal)) {
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body("OK");
        } else {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("BAD_REQUEST");
        }
    }

    /**
     * List emails.
     *
     * @param model Model
     * @param principal Principal
     * @return
     */
    @GetMapping(path = "/list")
    public String list(Model model, Principal principal) {
        if (principal != null) {
            User user = userService.findByUsername(principal.getName());

            if (user != null) {
                model.addAttribute("workbenchEmailList",
                        this.workbenchEmailService.findByUser(user));
            }
        }
        return "workbench/email/list";
    }

    /**
     * Delete email.
     *
     * @param id Long
     * @param model Model
     * @param principal Principal
     * @return
     */
    @GetMapping(path = "/delete/{id}")
    public String delete(
            @PathVariable(name = "id") Long id,
            Model model,
            Principal principal) {
        try {
            workbenchEmailService.delete(id);
        } catch (Exception ex) {
            model.addAttribute("errorMessage",
                    "Fail deleting mail: " + ex.getMessage());
        }
        return this.list(model, principal);
    }

    /**
     * Get invalid recipients.
     *
     * @param externalRecipient String
     * @return String
     */
    @PostMapping(path = "/invalid/recipients")
    @ResponseBody
    public String getInvalidRecipients(@RequestBody String externalRecipient) {
        return workbenchEmailService.getInvalidRecipients(externalRecipient);
    }

    /**
     * Default model
     *
     * @param model Model
     * @param workbenchEmail WorkbenchEmail
     */
    private void modelDefault(Model model, WorkbenchEmail workbenchEmail) {
        model.addAttribute("users", userService.list());
        model.addAttribute("email", workbenchEmail);
        model.addAttribute("connections", connectionService.list());
    }

    /**
     * Edit a WorkbenchEmail.
     *
     * @param model
     * @param id
     * @return
     */
    @GetMapping(path = "/edit/{id}")
    public String edit(
            Model model,
            @PathVariable(value = "id") Long id) {
        modelDefault(model, workbenchEmailService.load(id));
        return "workbench/email/edit";
    }

    /**
     * Save a server.
     *
     * @param workbenchEmail WorkbenchEmail
     * @param bindingResult BindingResult
     * @param principal
     * @param model Model
     * @return Server edit template.
     */
    @PostMapping(path = "/save")
    public String saveEmail(
            @Valid @ModelAttribute WorkbenchEmail workbenchEmail,
            BindingResult bindingResult,
            Model model,
            Principal principal) {

        if (bindingResult.hasErrors()) {
            modelDefault(model, workbenchEmail);
            return "workbench/email/edit";
        }

        try {
            modelDefault(model, workbenchEmail);
            workbenchEmailService.save(workbenchEmail);
        } catch (Exception ex) {
            model.addAttribute("errorMessage", new Message().getErrorMessage(ex));
        }

        return "redirect:/job/view/" + workbenchEmail.getId();
    }

    /**
     * Add a WorkbenchEmail.
     *
     * @param model Model
     * @param principal Principal
     * @return
     */
    @GetMapping(path = "/add")
    public String add(Model model, Principal principal) {
        User user = userService.findByUsername(principal.getName());

        if (user != null) {
            WorkbenchEmail workbenchEmail = new WorkbenchEmail();
            workbenchEmail.setUser(user);
            modelDefault(model, workbenchEmail);
        }

        return "workbench/email/edit";
    }

    /**
     * View a WorkbenchEmail.
     *
     * @param model Model
     * @param id Long workbenchemail id
     * @return
     */
    @GetMapping(path = "/view/{id}")
    public String view(
            Model model,
            @PathVariable(value = "id") Long id) {
        modelDefault(model, workbenchEmailService.load(id));
        return "workbench/email/view";
    }
}
