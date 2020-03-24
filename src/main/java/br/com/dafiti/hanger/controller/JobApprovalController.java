/*
 * Copyright (c) 2018 Dafiti Group
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

import java.security.Principal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import javax.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import br.com.dafiti.hanger.model.Job;
import br.com.dafiti.hanger.model.JobApproval;
import br.com.dafiti.hanger.model.JobDetails;
import br.com.dafiti.hanger.model.Role;
import br.com.dafiti.hanger.model.User;
import br.com.dafiti.hanger.option.Flow;
import br.com.dafiti.hanger.service.JobApprovalService;
import br.com.dafiti.hanger.service.JobDetailsService;
import br.com.dafiti.hanger.service.RoleService;
import br.com.dafiti.hanger.service.UserService;

/**
 * Manages all job approval requests.
 *
 * @author Guilherme ALMEIDA
 * @author Helio LEAL
 */
@Controller
@RequestMapping("/approval")
public class JobApprovalController {

    private final JobApprovalService jobApprovalService;
    private final UserService userService;
    private final RoleService roleService;
    private final JobDetailsService jobDetailsService;

    @Autowired
    public JobApprovalController(
            JobApprovalService jobApprovalService,
            UserService userService,
            RoleService roleService,
            JobDetailsService jobDetailsService) {

        this.jobApprovalService = jobApprovalService;
        this.userService = userService;
        this.roleService = roleService;
        this.jobDetailsService = jobDetailsService;
    }

    /**
     * Show a list with jobs detail that contains an approver and should be
     * approved if it is UNHEALTHY
     *
     * @param model Model
     * @return Approval list template
     */
    @GetMapping("/list")
    public String list(Model model) {
        HashSet<Job> jobs = this.jobApprovalService.findPending();
        List<JobDetails> details = jobDetailsService.getDetailsOf(new ArrayList<>(jobs));

        model.addAttribute("username", SecurityContextHolder.getContext().getAuthentication().getName());
        model.addAttribute("jobDetails", details);

        return "/approval/list";
    }

    /**
     * Approval the user and job and than open the form to approver approval
     *
     * @param model Model
     * @param redirectAttributes RedirectAttributes
     * @param principal Principal
     * @param job Job
     * @return Approval list
     */
    @GetMapping(path = "/approval/{job_id}")
    public String approval(
            Model model,
            RedirectAttributes redirectAttributes,
            Principal principal,
            @PathVariable(value = "job_id") Job job) {

        String approverName = "";
        User user = userService.findByUsername(principal.getName());

        if (job != null) {
            User approver = job.getApprover();

            if (approver != null) {
                approverName = approver.getUsername();
            }
        } else {
            redirectAttributes.addFlashAttribute("warningMessage", "Sorry, but you tried to access a non existing job!");
            return "redirect:/approval/list";
        }

        if (!user.getUsername().equals(approverName)) {
            Role userRole = roleService.findByName("USER");

            if (user.getRoles().contains(userRole)) {
                redirectAttributes.addFlashAttribute("warningMessage", "Sorry, but you don't have permission for approval!");
                return "redirect:/approval/list";
            }
        }

        if (job.getStatus().getFlow() == Flow.UNHEALTHY || job.getStatus().getFlow() == Flow.BLOCKED) {
            model.addAttribute("job", job);
            model.addAttribute("jobApproval", new JobApproval());
            return "/approval/approval";
        }

        redirectAttributes.addFlashAttribute("warningMessage", "Sorry, but can't approve a Job that is not UNHEALTHY or BLOCKED");

        return "redirect:/approval/list";
    }

    /**
     * Approve job, save status and run its flow.
     *
     * @param redirectAttributes RedirectAttributes
     * @param model Model
     * @param jobApproval JobApproval
     * @param job Job
     * @return Approval list
     */
    @PostMapping(path = "/submit/{job_id}", params = {"approve_job"})
    public String approve(
            RedirectAttributes redirectAttributes,
            Model model,
            @Valid @ModelAttribute JobApproval jobApproval,
            @PathVariable("job_id") Job job) {

        Flow flow = job.getStatus().getFlow();

        this.jobApprovalService.approve(jobApproval, job, true);

        if (this.jobApprovalService.push(job, flow)) {
            redirectAttributes.addFlashAttribute("successMessage", "Job " + job.getName() + " approved!");
        } else {
            redirectAttributes.addFlashAttribute("warningMessage", "Job approved, but fail building!");
        }

        return "redirect:/checkup/job/" + job.getId() + "/list/";
    }

    /**
     * Disapprove job, save status and don't run its flow.
     *
     * @param redirectAttributes RedirectAttributes
     * @param model Model
     * @param jobApproval Job Approval
     * @param job Job
     * @return Approval list
     */
    @PostMapping(path = "/submit/{job_id}", params = {"disapprove_job"})
    public String disapprove(
            RedirectAttributes redirectAttributes,
            Model model,
            @Valid @ModelAttribute JobApproval jobApproval,
            @PathVariable("job_id") Job job) {

        this.jobApprovalService.approve(jobApproval, job, false);

        redirectAttributes.addFlashAttribute("successMessage", "Job " + job.getName() + " disapproved!");
        return "redirect:/checkup/job/" + job.getId() + "/list/";
    }
}
