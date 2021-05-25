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

import br.com.dafiti.hanger.exception.Message;
import br.com.dafiti.hanger.model.Command;
import br.com.dafiti.hanger.model.AuditorData;
import br.com.dafiti.hanger.model.JobCheckup;
import br.com.dafiti.hanger.model.Job;
import br.com.dafiti.hanger.model.JobDetails;
import br.com.dafiti.hanger.model.Server;
import br.com.dafiti.hanger.model.Subject;
import br.com.dafiti.hanger.model.Template;
import br.com.dafiti.hanger.model.WorkbenchEmail;
import br.com.dafiti.hanger.option.Flow;
import br.com.dafiti.hanger.option.Status;
import br.com.dafiti.hanger.service.ConnectionService;
import br.com.dafiti.hanger.service.AuditorService;
import br.com.dafiti.hanger.service.FlowService;
import br.com.dafiti.hanger.service.JenkinsService;
import br.com.dafiti.hanger.service.JobApprovalService;
import br.com.dafiti.hanger.service.JobDetailsService;
import br.com.dafiti.hanger.service.JobNotificationService;
import br.com.dafiti.hanger.service.JobService;
import br.com.dafiti.hanger.service.JobStatusService;
import br.com.dafiti.hanger.service.RetryService;
import br.com.dafiti.hanger.service.ServerService;
import br.com.dafiti.hanger.service.SlackService;
import br.com.dafiti.hanger.service.SubjectService;
import br.com.dafiti.hanger.service.TemplateService;
import br.com.dafiti.hanger.service.UserService;
import br.com.dafiti.hanger.service.WorkbenchEmailService;
import io.swagger.annotations.ApiOperation;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 *
 * @author Valdiney V GOMES
 */
@Controller
@RequestMapping(path = "/job")
public class JobController {

    private final JobService jobService;
    private final ServerService serverService;
    private final SubjectService subjectService;
    private final JenkinsService jenkinsService;
    private final ConnectionService connectionService;
    private final UserService userService;
    private final RetryService retryService;
    private final JobStatusService jobStatusService;
    private final JobNotificationService jobNotificationService;
    private final SlackService slackService;
    private final FlowService flowService;
    private final JobApprovalService jobApprovalService;
    private final JobDetailsService jobDetailsService;
    private final AuditorService auditorService;
    private final WorkbenchEmailService workbenchEmailService;
    private final TemplateService templateService;

    private static final Logger LOG = LogManager.getLogger(JobController.class.getName());

    @Autowired
    public JobController(JobService jobService,
            ServerService serverService,
            SubjectService subjectService,
            JenkinsService jenkinsService,
            ConnectionService connectionService,
            UserService userService,
            RetryService retryService,
            JobStatusService jobStatusService,
            JobNotificationService jobNotificationService,
            SlackService slackService,
            FlowService flowService,
            JobApprovalService jobApprovalService,
            JobDetailsService jobDetailsService,
            AuditorService auditorService,
            WorkbenchEmailService workbenchEmailService,
            TemplateService templateService) {

        this.jobService = jobService;
        this.serverService = serverService;
        this.subjectService = subjectService;
        this.jenkinsService = jenkinsService;
        this.connectionService = connectionService;
        this.userService = userService;
        this.retryService = retryService;
        this.jobStatusService = jobStatusService;
        this.jobNotificationService = jobNotificationService;
        this.slackService = slackService;
        this.flowService = flowService;
        this.jobApprovalService = jobApprovalService;
        this.jobDetailsService = jobDetailsService;
        this.auditorService = auditorService;
        this.workbenchEmailService = workbenchEmailService;
        this.templateService = templateService;
    }

    /**
     * Add a job.
     *
     * @param model Model
     * @return Job edit
     */
    @GetMapping(path = "/add")
    public String add(Model model) {
        this.modelDefault(model, new Job());
        return "job/edit";
    }

    /**
     * List all jobs.
     *
     * @param model Model
     * @return Job list
     */
    @GetMapping(path = "/list")
    public String list(Model model) {
        model.addAttribute("jobs", jobService.listFromCache());
        return "job/list";
    }

    /**
     * List job of a server.
     *
     * @param server Server
     * @param incremental Identify if should list only not imported jobs.
     * @return Job list modal
     */
    @GetMapping(path = "/list/{server}")
    @ResponseBody
    public List<String> listJobServer(
            @PathVariable(value = "server") Server server,
            @RequestParam(name = "incremental", defaultValue = "true") boolean incremental) {

        List<String> jobs = null;

        if (server != null) {
            try {
                jobs = jobService.listFromServer(
                        server,
                        incremental);
            } catch (URISyntaxException | IOException ex) {
                LOG.log(Level.ERROR,
                        "Fail listing jobs from server " + server.getName(), ex);
            }
        }

        return jobs;
    }

    /**
     * Edit a job.
     *
     * @param model Model
     * @param job Job
     * @return job edit
     */
    @GetMapping(path = "/edit/{id}")
    public String edit(
            Model model,
            @PathVariable(value = "id") Job job) {

        job.setShellScript(jenkinsService.getShellScript(job));
        job.setAssignedNode(jenkinsService.getAssignedNode(job));
        this.modelDefault(model, job);
        return "job/edit";
    }

    /**
     * Delete a job.
     *
     * @param redirectAttributes RedirectAttributes
     * @param job Job
     * @return Redirect to job list
     */
    @GetMapping(path = "/delete/{id}")
    public String delete(
            RedirectAttributes redirectAttributes,
            @PathVariable(name = "id") Job job) {

        try {
            jobService.delete(job.getId());
        } catch (Exception ex) {
            if (ex.getClass() == DataIntegrityViolationException.class) {
                redirectAttributes.addFlashAttribute("errorMessage", "This job is being used. Remove the dependencies before deleting the job!");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Fail deleting the job: " + ex.getMessage());
            }
        }

        return "redirect:/job/list";
    }

    /**
     * Edit a job.
     *
     * @param model Model
     * @param job Job
     * @return Job view
     */
    @GetMapping(path = "/view/{id}")
    public String view(
            Model model,
            @PathVariable(value = "id") Job job) {

        job.setShellScript(jenkinsService.getShellScript(job));
        job.setAssignedNode(jenkinsService.getAssignedNode(job));
        this.modelDefault(model, job, false);
        return "job/view";
    }

    /**
     * Build job.
     *
     * @param redirectAttributes RedirectAttributes
     * @param job Job
     * @return flow
     */
    @GetMapping(path = "/build/{id}")
    public String build(
            RedirectAttributes redirectAttributes,
            @PathVariable(value = "id") Job job) {

        if (jobBuild(job)) {
            redirectAttributes.addFlashAttribute("successMessage", "Job built successfully, refresh this page to see the build progress!");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage", "Fail building job!");
        }

        return "redirect:/flow/job/" + job.getId();
    }

    /**
     * Build a job.
     *
     * @param job Job
     * @return Identifies if the job was built successfully.
     */
    @GetMapping(path = "/build/silently/{id}")
    @ResponseBody
    public boolean build(@PathVariable(value = "id") Job job) {
        return jobBuild(job);
    }

    /**
     * Build a job.
     *
     * @param model
     * @param job
     * @return Identifies if the job was built successfully.
     */
    @ApiOperation(value = "Build a job")
    @PostMapping(path = "/api/build/{id}")
    @ResponseBody
    public ResponseEntity build(
            Model model,
            @PathVariable(value = "id") Job job) {

        if (jobBuild(job, true)) {
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
     * Build a job.
     *
     * @param job Job
     * @return boolean with status of job build.
     */
    private boolean jobBuild(Job job) {
        return jobBuild(job, false);
    }

    /**
     * Build a job.
     *
     * @param job Job
     * @param api Identifies api build.
     * @return boolean with status of job build.
     */
    private boolean jobBuild(Job job, boolean api) {
        boolean built = false;

        auditorService.publish(api ? "API_BUILD_JOB" : "BUILD_JOB",
                new AuditorData()
                        .addData("name", job.getName())
                        .getData());

        retryService.remove(job);

        try {
            built = jenkinsService.build(job);

            if (!built) {
                jobStatusService.updateFlow(job.getStatus(), Flow.ERROR);
                jobNotificationService.notify(job, true);
            } else {
                jobStatusService.updateFlow(job.getStatus(), Flow.QUEUED);
            }

        } catch (Exception ex) {
            LOG.log(Level.ERROR, "Fail building job " + job.getName() + " manually", ex);
        }

        return built;
    }

    /**
     * Build mesh.
     *
     * @param redirectAttributes RedirectAttributes
     * @param job Job
     * @return flow
     */
    @GetMapping(path = "/rebuild/{id}")
    public String buildMesh(
            RedirectAttributes redirectAttributes,
            @PathVariable(value = "id") Job job) {

        try {
            if (jenkinsService.isRunning(job.getServer())) {
                retryService.remove(job);
                jobService.rebuildMesh(job);

                HashSet<Job> parent = jobService.getMeshParent(job);

                auditorService.publish("BUILD_MESH",
                        new AuditorData()
                                .addData("name", job.getName())
                                .addData("javascript", parent.toString())
                                .getData());

                for (Job meshParent : parent) {
                    retryService.remove(meshParent);

                    if (!jenkinsService.build(meshParent)) {
                        jobStatusService.updateFlow(job.getStatus(), Flow.ERROR);
                        jobNotificationService.notify(job, true);
                    } else {
                        jobStatusService.updateFlow(job.getStatus(), Flow.REBUILD);
                    }
                }

                redirectAttributes.addFlashAttribute("successMessage", "Mesh built successfully, refresh this page to see the build progress!");
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Jenkins server is not running! ");
            }
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Fail building mesh, " + ex.getMessage() + "!");
        }

        return "redirect:/flow/job/" + job.getId();
    }

    /**
     * Save a job.
     *
     * @param job Job
     * @param model Model
     * @return Job view
     */
    @PostMapping(path = "/save")
    public String save(
            @Valid @ModelAttribute Job job,
            Model model) {

        try {
            jobService.saveAndUpdateJobConfig(job);
            jenkinsService.refresh();
        } catch (Exception ex) {
            model.addAttribute("errorMessage", new Message().getErrorMessage(ex));
            this.modelDefault(model, job);
            return "job/edit";
        }

        return "redirect:/job/view/" + job.getId();
    }

    /**
     * Save a job.
     *
     * @param importServer
     * @param createServer
     * @param migrateSourceServer
     * @param migrateTargetServer
     * @param importJob
     * @param createJob
     * @param createJobName
     * @param migrateSourceJob
     * @param migrateTargetJob
     * @param migrateJobName
     * @param action
     * @param model
     * @return Job edit
     */
    @PostMapping(path = "/save", params = {"partial_load_job"})
    public String loadJob(
            @RequestParam(name = "importServer", required = false) Server importServer,
            @RequestParam(name = "createServer", required = false) Server createServer,
            @RequestParam(name = "migrateSourceServer", required = false) Server migrateSourceServer,
            @RequestParam(name = "migrateTargetServer", required = false) Server migrateTargetServer,
            @RequestParam(name = "importJob", required = false) String importJob,
            @RequestParam(name = "createJob", required = false) String createJob,
            @RequestParam(name = "createJobName", required = false) String createJobName,
            @RequestParam(name = "migrateSourceJob", required = false) String migrateSourceJob,
            @RequestParam(name = "migrateTargetJob", required = false) String migrateTargetJob,
            @RequestParam(name = "migrateJobName", required = false) String migrateJobName,
            @RequestParam(name = "action", required = false) String action,
            Model model) throws URISyntaxException, IOException {

        Job job = new Job();

        try {
            switch (action) {
                case "import":
                    if (jenkinsService.isRunning(importServer)) {

                        job.setServer(importServer);
                        job.setName(importJob);
                        job.setShellScript(jenkinsService.getShellScript(job));
                    } else {
                        throw new URISyntaxException("Server is not running", "Jenkins");
                    }
                    break;
                case "create":
                    if (jenkinsService.isRunning(createServer)) {

                        job.setServer(createServer);
                        job.setName(createJobName);
                        jenkinsService.clone(job, createJob);
                        job.setShellScript(jenkinsService.getShellScript(job, createJob));
                    } else {
                        throw new URISyntaxException("Server is not running", "Jenkins");
                    }

                    break;
                default:

                    if (jenkinsService.isRunning(migrateSourceServer)
                            && jenkinsService.isRunning(migrateTargetServer)) {
                        Job sourceJob = new Job();
                        sourceJob.setServer(migrateSourceServer);
                        sourceJob.setName(migrateSourceJob);

                        job.setServer(migrateTargetServer);
                        job.setName(migrateJobName);
                        jenkinsService.clone(job, migrateTargetJob);
                        job.setShellScript(jenkinsService.getShellScript(sourceJob));
                    } else {
                        throw new URISyntaxException("Server is not running", "Jenkins");
                    }
            }

            job.setAssignedNode(jenkinsService.getAssignedNode(job));
            model.addAttribute("readOnly", true);

        } catch (URISyntaxException | IOException ex) {
            job.setServer(null);
            model.addAttribute("errorMessage", "Fail: " + ex.getMessage());
        } finally {
            this.modelDefault(model, job);
        }

        return "job/edit";
    }

    /**
     * Add a subject.
     *
     * @param job Job
     * @param subjectsID Subject ID list
     * @param model Model
     * @return Job edit
     */
    @PostMapping(path = "/save", params = {"partial_add_subject"})
    public String addSubject(
            @Valid @ModelAttribute Job job,
            @RequestParam(value = "subjects", required = false) String subjectsID,
            Model model) {

        if (subjectsID != null) {
            for (String subjectID : subjectsID.split(",")) {
                Subject subject = subjectService.load(Long.parseLong(subjectID));

                if (subject != null) {
                    if (!job.getSubject().contains(subject)) {
                        job.addSubject(subject);
                    }
                }
            }
        }

        this.modelDefault(model, job);

        return "job/edit";
    }

    /**
     * Remove a subject.
     *
     * @param job Job
     * @param index index
     * @param model Model
     * @return Job edit
     */
    @PostMapping(path = "/save", params = {"partial_remove_subject"})
    public String removeSubject(
            @ModelAttribute Job job,
            @RequestParam(value = "partial_remove_subject", required = false) int index,
            Model model) {

        job.getSubject().remove(index);
        this.modelDefault(model, job);

        return "job/edit";
    }

    /**
     * Add a shell script.
     *
     * @param job Job
     * @param template
     * @param parameters
     * @param model Model
     * @return Job edit
     */
    @PostMapping(path = "/save", params = {"partial_add_job_shell_script"})
    public String addShellScript(
            @Valid @ModelAttribute Job job,
            @RequestParam(name = "templateID", required = false) Template template,
            @RequestParam(name = "templateParameters", required = false) JSONArray parameters,
            Model model) {

        String shellScript = "";

        if (template != null) {
            shellScript = templateService.setParameters(
                    template.getModel(),
                    parameters
            );
        }

        job.addShellScript(shellScript);
        this.modelDefault(model, job);

        return "job/edit";
    }

    /**
     * Remove a shell script.
     *
     * @param job Job
     * @param index index
     * @param model Model
     * @return Job edit
     */
    @PostMapping(path = "/save", params = {"partial_remove_job_shell_script"})
    public String removeShellScript(
            @ModelAttribute Job job,
            @RequestParam(value = "partial_remove_job_shell_script", required = false) int index,
            Model model) {

        job.getShellScript().remove(index);
        this.modelDefault(model, job);

        return "job/edit";
    }

    /**
     *
     * @param template
     * @param model
     * @return
     */
    @GetMapping(path = "/modal/template/{id}")
    public String shellScriptTemplateModal(
            @PathVariable(value = "id") Template template,
            Model model) {

        try {
            model.addAttribute("template", template);
            model.addAttribute("parameters", templateService.getParameters(template.getModel()));
        } catch (Exception ex) {
            model.addAttribute("errorMessage", "Fail getting parameters " + new Message().getErrorMessage(ex));
        }

        return "job/modalShellScriptTemplate::parameter";
    }

    /**
     * Add a parent.
     *
     * @param job Job
     * @param parentServer Parent server
     * @param parentJobList Parent Job List
     * @param parentUpstream Parent Upstream
     * @param bindingResult
     * @param model model
     * @return Job edit
     */
    @PostMapping(path = "/save", params = {"partial_add_parent"})
    public String addParent(
            @Valid @ModelAttribute Job job,
            @RequestParam(value = "parentServer", required = true) Server parentServer,
            @RequestParam(value = "parentJobList", required = false) List<String> parentJobList,
            @RequestParam(value = "parentUpstream", required = false) boolean parentUpstream,
            BindingResult bindingResult,
            Model model) {

        List<String> errors = new ArrayList();

        try {
            jobService.addParent(job, parentServer, parentJobList, parentUpstream, errors);
        } catch (Exception ex) {
            model.addAttribute("errorMessage", new Message().getErrorMessage(ex));
        } finally {
            if (!errors.isEmpty()) {
                model.addAttribute("errorMessage", "The following jobs are invalid ou disabled on Jenkins: " + String.join(",", errors));
            }

            this.modelDefault(model, job);
        }

        return "job/edit";
    }

    /**
     * Remove a parent.
     *
     * @param job Job
     * @param index Index
     * @param model Model
     * @return Job edit
     */
    @PostMapping(path = "/save", params = {"partial_remove_parent"})
    public String removeParent(
            @Valid @ModelAttribute Job job,
            @RequestParam("partial_remove_parent") int index,
            Model model) {

        job.getParent().remove(index);
        this.modelDefault(model, job);

        return "job/edit";
    }

    /**
     * Add Slack channels.
     *
     * @param job Job
     * @param slackChannelList Slack channel list.
     * @param bindingResult
     * @param model Model
     * @return Job edit.
     */
    @PostMapping(path = "/save", params = {"partial_add_slack_channel"})
    public String addSlackChannel(
            @Valid @ModelAttribute Job job,
            @RequestParam(value = "slackChannelList", required = false) Set<String> slackChannelList,
            BindingResult bindingResult,
            Model model) {

        try {
            job.getChannel().addAll(slackChannelList);
        } catch (Exception ex) {
            model.addAttribute("errorMessage", new Message().getErrorMessage(ex));
        } finally {
            this.modelDefault(model, job);
        }

        return "job/edit";
    }

    /**
     * Remove a Slack Channel.
     *
     * @param job Job
     * @param slackChannel Slack channel
     * @param model Model
     * @return Job edit
     */
    @PostMapping(path = "/save", params = {"partial_remove_slack_channel"})
    public String removeSlackChannel(
            @ModelAttribute Job job,
            @RequestParam(value = "partial_remove_slack_channel", required = false) String slackChannel,
            Model model) {

        job.getChannel().remove(slackChannel);
        this.modelDefault(model, job);

        return "job/edit";
    }

    /**
     * Add a checkup.
     *
     * @param job Job
     * @param model Model
     * @return Job edit
     */
    @PostMapping(path = "/save", params = {"partial_add_job_checkup"})
    public String addCheckup(
            @Valid @ModelAttribute Job job,
            Model model) {

        job.addCheckup(new JobCheckup());
        model.addAttribute("triggers", jobService.getMesh(job, false));
        this.modelDefault(model, job);

        return "job/edit";
    }

    /**
     * Remove a checkup.
     *
     * @param job Job
     * @param index Index
     * @param model Model
     * @return Job edit
     */
    @PostMapping(path = "/save", params = {"partial_remove_job_checkup"})
    public String removeCheckup(
            @ModelAttribute Job job,
            @RequestParam("partial_remove_job_checkup") int index,
            Model model) {

        job.getCheckup().remove(index);
        this.modelDefault(model, job);

        return "job/edit";
    }

    /**
     * Add a checkup trigger.
     *
     * @param job Job
     * @param checkupIndex Checkup
     * @param triggers Triggers
     * @param model Model
     * @return Job edit
     */
    @PostMapping(path = "/save", params = {"partial_add_job_checkup_trigger"})
    public String addTrigger(
            @Valid @ModelAttribute Job job,
            @RequestParam("partial_add_job_checkup_trigger") int checkupIndex,
            @RequestParam(value = "triggers", required = false) List<String> triggers,
            Model model) {

        try {
            jobService.addTrigger(job, checkupIndex, triggers);
        } catch (Exception ex) {
            model.addAttribute("errorMessage", new Message().getErrorMessage(ex));
        } finally {
            this.modelDefault(model, job);
        }

        return "job/edit";
    }

    /**
     * Remove a trigger.
     *
     * @param job Job
     * @param checkupTriggerIndex Index
     * @param model Model
     * @return Job edit
     */
    @PostMapping(path = "/save", params = {"partial_remove_job_checkup_trigger"})
    public String removeTrigger(
            @Valid @ModelAttribute Job job,
            @RequestParam("partial_remove_job_checkup_trigger") List<Integer> checkupTriggerIndex,
            Model model) {

        if (checkupTriggerIndex.size() == 2) {
            int checkupIndex = checkupTriggerIndex.get(0);
            int triggerIndex = checkupTriggerIndex.get(1);
            job.getCheckup().get(checkupIndex).getTrigger().remove(triggerIndex);
        }

        this.modelDefault(model, job);

        return "job/edit";
    }

    /**
     * Add a job checkup command.
     *
     * @param job Job
     * @param checkupIndex Index
     * @param model Model
     * @return Job edit
     */
    @PostMapping(path = "/save", params = {"partial_add_job_checkup_command"})
    public String addJobCheckupCommand(
            @Valid @ModelAttribute Job job,
            @RequestParam("partial_add_job_checkup_command") int checkupIndex,
            Model model) {

        job.getCheckup().get(checkupIndex).addCommand(new Command());
        this.modelDefault(model, job, checkupIndex);
        return "job/edit";
    }

    /**
     * Remove a job checkup command.
     *
     * @param job Job
     * @param checkupCommandIndex
     * @param model Model
     * @return Job edit
     */
    @PostMapping(path = "/save", params = {"partial_remove_job_checkup_command"})
    public String removeJobCheckupCommand(
            @Valid @ModelAttribute Job job,
            @RequestParam("partial_remove_job_checkup_command") List<Integer> checkupCommandIndex,
            Model model) {

        if (checkupCommandIndex.size() == 2) {
            int checkupIndex = checkupCommandIndex.get(0);
            int commandIndex = checkupCommandIndex.get(1);
            job.getCheckup().get(checkupIndex).getCommand().remove(commandIndex);
        }

        this.modelDefault(model, job);

        return "job/edit";
    }

    /**
     * Add e-mail list.
     *
     * @param job
     * @param emailList
     * @param bindingResult BindingResult
     * @param model Model
     * @return Subject edit
     */
    @PostMapping(path = "/save", params = {"partial_add_email"})
    public String addEmailList(
            @Valid @ModelAttribute Job job,
            @RequestParam(value = "emailList", required = false) List<WorkbenchEmail> emailList,
            BindingResult bindingResult,
            Model model) {

        try {
            emailList.forEach((email) -> {
                job.addEmail(email);
            });
        } catch (Exception ex) {
            model.addAttribute("errorMessage", new Message().getErrorMessage(ex));
        } finally {
            this.modelDefault(model, job);
        }

        return "job/edit";

    }

    /**
     * Remove an e-mail.
     *
     * @param job Job
     * @param index index
     * @param bindingResult BindingResult
     * @param model Model
     * @return Job edit
     */
    @PostMapping(path = "/save", params = {"partial_remove_email"})
    public String removeEmail(
            @ModelAttribute Job job,
            @RequestParam(value = "partial_remove_email", required = false) int index,
            BindingResult bindingResult,
            Model model) {

        job.getEmail().remove(index);
        this.modelDefault(model, job);

        return "job/edit";
    }

    /**
     * Job list modal.
     *
     * @param server Server
     * @param model Model
     * @return Job list modal
     */
    @GetMapping(path = "/modal/list/{serverID}")
    public String jobListModal(
            @PathVariable(value = "serverID") Server server,
            Model model) {

        if (server != null) {
            try {
                model.addAttribute("server", server);
                model.addAttribute("jobs", jenkinsService.listJob(server));
            } catch (URISyntaxException | IOException ex) {
                model.addAttribute("errorMessage", "Fail listing jobs from Jenkins: " + ex.getMessage());
            }
        }

        return "job/modalJobList::job";
    }

    /**
     * Slack channel list modal.
     *
     * @param model Model
     * @return Slack channel modal
     */
    @GetMapping(path = "/modal/channel")
    public String slackChannelListModal(Model model) {
        model.addAttribute("channels", slackService.getChannels());
        return "job/modalSlackChannel::channel";
    }

    /**
     * E-mail list modal.
     *
     * @param model Model
     * @return E-mail list modal
     */
    @GetMapping(path = "/modal/email")
    public String emailListModal(Model model) {
        model.addAttribute("emails", workbenchEmailService.list());
        return "job/modalEmailList::email";
    }

    /**
     * Refresh system cache
     *
     * @param model Model
     * @param redirectAttributes
     * @return Job list template.
     */
    @GetMapping(path = "/refresh/")
    public String refreshCache(
            Model model,
            RedirectAttributes redirectAttributes) {

        auditorService.publish("REFRESH_CACHE");

        try {
            jobService.refresh();
            jenkinsService.refresh();
            slackService.refresh();
            connectionService.refresh();

            redirectAttributes.addFlashAttribute("successMessage", "Cache updated successfully!");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", "Fail update cache: " + ex.toString());
        }

        return "redirect:/configuration/edit";
    }

    /**
     * Update notification plugin.
     *
     * @return Job list
     */
    @GetMapping(path = "/maintenance/plugin")
    @ResponseBody
    public boolean pluginMaintenance() {
        List<Job> jobs = (List) jobService.list();

        auditorService.publish("PLUGIN_MAINTENANCE");

        jobs.forEach(job -> {
            LOG.log(Level.INFO, "Updating plugin for: " + job.getName());
            jenkinsService.updateJob(job);
        });

        return true;
    }

    /**
     * Update notification plugin for a specific server.
     *
     * @param server Server
     * @return Job list
     */
    @GetMapping(path = "/maintenance/plugin/{id}")
    @ResponseBody
    public boolean pluginMaintenance(
            @PathVariable(value = "id") Server server) {
        List<Job> jobs = jobService.findByServer(server);

        auditorService.publish("PLUGIN_MAINTENANCE",
                new AuditorData()
                        .addData("server", server.getName())
                        .getData());

        jobs.forEach(job -> {
            LOG.log(Level.INFO, "Updating plugin for: " + job.getName());
            jenkinsService.updateJob(job);
        });

        return true;
    }

    /**
     * Default model.
     *
     * @param model Model
     * @param job Job
     */
    private void modelDefault(Model model, Job job) {
        this.modelDefault(model, job, true);
    }

    /**
     * Default model.
     *
     * @param model Model
     * @param job Job
     * @param checkupIndex int
     */
    private void modelDefault(Model model, Job job, int checkupIndex) {
        model.addAttribute("expanded", checkupIndex);
        this.modelDefault(model, job, true);
    }

    /**
     * Default model
     *
     * @param model Model
     * @param job Job
     * @param jobList boolean
     */
    private void modelDefault(Model model, Job job, boolean jobList) {
        model.addAttribute("job", job);
        model.addAttribute("servers", serverService.list());
        model.addAttribute("subjects", subjectService.list());
        model.addAttribute("connections", connectionService.list());
        model.addAttribute("users", userService.list(true));
        model.addAttribute("templates", templateService.list());

        if (job.getId() != null) {
            model.addAttribute("children", jobService.getChildrenlist(job));
        }

        if (jobList) {
            if (!job.getCheckup().isEmpty()) {
                model.addAttribute("triggers", jobService.getMesh(job, false));
            }
        }

        if (job.getServer() != null) {
            model.addAttribute("connected", jenkinsService.isRunning(job.getServer()));
        } else {
            model.addAttribute("connected", false);
        }
    }

    /**
     * Update job chain modal.
     *
     * @param job Job
     * @param server Server
     * @param children Identify propagation or flow.
     * @param model Model
     *
     * @return Job add parent Modal
     */
    @GetMapping(path = "/modal/update/chain/{jobID}/{serverID}/{flow}")
    public String updateJobChainModal(
            @PathVariable(value = "jobID") Job job,
            @PathVariable(value = "serverID") Server server,
            @PathVariable(value = "flow") boolean children,
            Model model) {

        if (server != null) {
            try {
                model.addAttribute("server", server);
                model.addAttribute("children", children);
                model.addAttribute("job", job);
                model.addAttribute("jobs", jenkinsService.listJob(server));
            } catch (URISyntaxException | IOException ex) {
                model.addAttribute("errorMessage", "Fail listing jobs from Jenkins: " + ex.getMessage());
            }
        }

        return "flow/modalUpdateJobChain::updateJobChain";
    }

    /**
     * Update job chain, parent or children.
     *
     * @param job Job
     * @param server Server
     * @param jobList Parent or Children Job List
     * @param children Identify if propagation or flow
     * @param rebuildable Identify if should make all children rebuildable.
     * @param principal Logged User.
     * @param model model
     * @param request
     * @param redirectAttributes
     *
     * @return flow/display or propagation/display
     */
    @PostMapping(path = "/update/chain")
    public String updateJobChain(
            @RequestParam(value = "jobID", required = true) Job job,
            @RequestParam(value = "serverID", required = true) Server server,
            @RequestParam(value = "jobList", required = false) List<String> jobList,
            @RequestParam(value = "children", required = false) boolean children,
            @RequestParam(value = "rebuildable", required = false) boolean rebuildable,
            Principal principal,
            Model model,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        List<String> errors = new ArrayList();

        try {
            if (children) {
                jobService.addChildren(job, server, jobList, false, rebuildable, errors);
            } else {
                jobService.addParent(job, server, jobList, false, errors);
                jobService.save(job);
            }
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("errorMessage", new Message().getErrorMessage(ex));
        } finally {
            if (!errors.isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "The following jobs are invalid ou disabled on Jenkins: " + String.join(",", errors));
            }

            model.addAttribute("job", job);
            model.addAttribute("warnings", flowService.getFlowWarning(job));
            model.addAttribute("chart", flowService.getJobFlow(job, children, true));
            model.addAttribute("approval", this.jobApprovalService.hasApproval(job, principal));
            model.addAttribute("servers", this.serverService.list());

            this.modelDefault(model, job);
        }

        return "redirect:" + request.getHeader("referer");
    }

    /**
     * Enable or disable a job.
     *
     * @param job Job
     * @param enabled Job is enabled or not.
     * @return Job flow.
     */
    @GetMapping(path = "/enable/{job}/{enable}")
    public String enable(
            @PathVariable(value = "job") Job job,
            @PathVariable(value = "enable") boolean enabled) {

        job.setEnabled(!enabled);
        jobService.save(job);
        jenkinsService.updateJob(job);

        return "flow/display";
    }

    /**
     * Set a job as rebuildable or non rebuildable.
     *
     * @param job Job
     * @param rebuild
     * @return Job flow.
     */
    @GetMapping(path = "/rebuildable/{job}/{rebuildable}")
    public String rebuildable(
            @PathVariable(value = "job") Job job,
            @PathVariable(value = "rebuildable") boolean rebuild) {

        job.setRebuild(!rebuild);
        jobService.save(job);
        jenkinsService.updateJob(job);

        return "flow/display";
    }

    /**
     * Related email modal.
     *
     * @param job Job
     * @param model Model
     * @return flow modal
     */
    @GetMapping(path = "/emails/{id}")
    public String relatedEmailModal(
            @PathVariable("id") Job job,
            Model model) {

        this.modelDefault(model, job);

        return "flow/modalEmails::emails";
    }

    /**
     * Identify if job is enabled.
     *
     * @param job Job
     * @return boolean
     */
    @GetMapping(path = "/is/enabled/{job}")
    @ResponseBody
    public boolean isEnabled(
            @PathVariable(value = "job") Job job) {

        boolean isEnabled = true;

        if (job != null) {
            JobDetails jobDetails = jobDetailsService.getDetailsOf(job);
            isEnabled = jobDetails.getStatus() != Status.DISABLED;
        }

        return isEnabled;
    }

    /**
     * Identify if job is rebuildable.
     *
     * @param job Job
     * @return boolean
     */
    @GetMapping(path = "/is/rebuild/{job}")
    @ResponseBody
    public boolean isRebuildable(
            @PathVariable(value = "job") Job job) {

        boolean isRebuildable = true;

        if (job != null) {
            isRebuildable = job.isRebuild();
        }

        return isRebuildable;
    }

    /**
     * Abort a job execution.
     *
     * @param job Job
     * @return Identifies if the job was built successfully.
     */
    @GetMapping(path = "/build/abort/{id}")
    @ResponseBody
    public boolean abort(@PathVariable(value = "id") Job job) {
        JobDetails jobDetails = jobDetailsService.getDetailsOf(job);

        auditorService.publish("ABORT_JOB",
                new AuditorData()
                        .addData("name", job.getName())
                        .getData());

        return jenkinsService.abort(job, jobDetails.getBuildNumber());

    }

    /**
     * Get a job console log.
     *
     * @param model Model
     * @param job Job
     * @return Job console log.
     */
    @GetMapping(path = "/log/{job}")
    public String log(
            Model model,
            @PathVariable(value = "job") Job job) {
        model.addAttribute("log", jenkinsService.getLog(job));
        return "job/log";
    }
}
