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

import br.com.dafiti.hanger.model.CommandLog;
import br.com.dafiti.hanger.model.Job;
import br.com.dafiti.hanger.service.JobApprovalService;
import br.com.dafiti.hanger.service.JobCheckupLogService;
import br.com.dafiti.hanger.service.JobCheckupService;
import br.com.dafiti.hanger.service.JobDetailsService;
import java.security.Principal;
import java.util.Date;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 *
 * @author Valdiney V GOMES
 */
@Controller
@RequestMapping(path = "/checkup")
public class JobCheckupController {

    private final JobApprovalService jobApprovalService;
    private final JobDetailsService jobDetailsService;
    private final JobCheckupService jobCheckupService;
    private final JobCheckupLogService jobCheckupLogService;
    

    @Autowired
    public JobCheckupController(
            JobApprovalService jobApprovalService,
            JobDetailsService jobDetailsService,
            JobCheckupService jobCheckupService,
            JobCheckupLogService jobCheckupLogService) {

        this.jobApprovalService = jobApprovalService;
        this.jobDetailsService = jobDetailsService;
        this.jobCheckupService = jobCheckupService;
        this.jobCheckupLogService = jobCheckupLogService;
    }

    /**
     * Show the checkup list of a job.Show a list of approvals for a job.
     *
     * @param principal logged user
     * @param job Job
     * @param dateFrom
     * @param dateTo
     * @param model Model
     * @return Checkup list
     */
    @GetMapping(path = "/job/{id}/list")
    public String list(
            Principal principal,
            @PathVariable(name = "id") Job job,
            Model model) {

        model.addAttribute("approvals", jobApprovalService.findByJobOrderByCreatedAtDesc(job));
        model.addAttribute("job", job);
        model.addAttribute("jobDetail", this.jobDetailsService.getDetailsOf(job));
        model.addAttribute("approval", this.jobApprovalService.hasApproval(job, principal));

        return "checkup/list";
    }

    /**
     * Show the log of a job checkup command.
     *
     * @param commandLog CommandLog
     * @param model Model
     * @return Checkup log
     */
    @GetMapping(path = "/log/{id}")
    public String log(
            @PathVariable(name = "id") CommandLog commandLog,
            Model model) {

        model.addAttribute("log", commandLog);
        return "checkup/log";
    }

    @PostMapping(path = "/filter")
    public String filter(
            @RequestParam("job") Job job,
            @RequestParam("dateFrom") @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date dateFrom,
            @RequestParam("dateTo") @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date dateTo,
            Principal principal,
            Model model) {
        model.addAttribute("approvals", jobApprovalService.findByJobOrderByCreatedAtDesc(job));
        model.addAttribute("job", job);
        model.addAttribute("jobDetail", this.jobDetailsService.getDetailsOf(job));
        model.addAttribute("approval", this.jobApprovalService.hasApproval(job, principal));
//        model.addAttribute("chekups", this.jobCheckupService.findByDate(dateFrom, dateTo));
//        model.addAttribute("period", this.jobCheckupLogService.findAllCheckupLogByDateBetween(dateFrom, dateTo));
//        model.addAttribute("checkups", this.jobCheckupService.findJobCheckupByJobId(job));
        model.addAttribute("period", this.jobCheckupService.findAllByDateBetween(dateFrom, dateTo));
        return "checkup/list";
    }
}
 