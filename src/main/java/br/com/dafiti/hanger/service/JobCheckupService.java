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
package br.com.dafiti.hanger.service;

import br.com.dafiti.hanger.model.Blueprint;
import br.com.dafiti.hanger.model.Command;
import br.com.dafiti.hanger.model.CommandLog;
import br.com.dafiti.hanger.model.JobCheckup;
import br.com.dafiti.hanger.model.Job;
import br.com.dafiti.hanger.model.JobCheckupLog;
import br.com.dafiti.hanger.option.Action;
import br.com.dafiti.hanger.option.CommandType;
import br.com.dafiti.hanger.option.Flow;
import br.com.dafiti.hanger.option.Scope;
import br.com.dafiti.hanger.repository.JobCheckupRepository;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import java.util.stream.Collectors;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 *
 * @author Valdiney V GOMES
 */
@Service
public class JobCheckupService {

    private final JdbcTemplate jdbcTemplate;
    private final JobCheckupRepository jobCheckupRepository;
    private final ConnectionService connectionService;
    private final JenkinsService jenkinsService;
    private final JobService jobService;
    private final RetryService retryService;
    private final CommandLogService commandLogService;
    private final MailService mailService;
    private final JobStatusService jobStatusService;
    private final SlackService slackService;
    private final TemplateService templateService;

    private static final Logger LOG = LogManager.getLogger(JobBuildPushService.class.getName());

    @Autowired
    public JobCheckupService(
            JdbcTemplate jdbcTemplate,
            JobCheckupRepository jobCheckupRepository,
            ConnectionService connectionService,
            JenkinsService jenkinsService,
            JobService jobService,
            RetryService retryService,
            CommandLogService commandLogService,
            MailService mailService,
            JobStatusService jobStatusService,
            SlackService slackService,
            TemplateService templateService) {

        this.jdbcTemplate = jdbcTemplate;
        this.jobCheckupRepository = jobCheckupRepository;
        this.connectionService = connectionService;
        this.jenkinsService = jenkinsService;
        this.jobService = jobService;
        this.retryService = retryService;
        this.commandLogService = commandLogService;
        this.mailService = mailService;
        this.jobStatusService = jobStatusService;
        this.slackService = slackService;
        this.templateService = templateService;
    }

    /**
     * List all checkups.
     *
     * @return
     */
    public Iterable<JobCheckup> list() {
        return jobCheckupRepository.findAll();
    }

    /**
     * Load a checkup.
     *
     * @param id JobCheckup ID.
     * @return JobCheckup.
     */
    public JobCheckup load(Long id) {
        return jobCheckupRepository.findById(id).orElse(null);
    }

    /**
     * Save a checkup.
     *
     * @param checkup JobCheckup.
     */
    public void save(JobCheckup checkup) {
        jobCheckupRepository.save(checkup);
    }

    /**
     * Delete a checkup.
     *
     * @param id JobCheckup ID.
     */
    public void delete(Long id) {
        jobCheckupRepository.deleteById(id);
    }

    /**
     * Identify if there are prevalidation.
     *
     * @param job Job.
     * @return Identify if a job has prevalidation.
     */
    public boolean hasPrevalidation(Job job) {
        List<JobCheckup> prevalidation = job.getCheckup()
                .stream()
                .filter(x -> (x.isPrevalidation()))
                .collect(Collectors.toList());

        return !(prevalidation.isEmpty());
    }

    /**
     * Evaluate a job.
     *
     * @param job Job.
     * @param scope Scope.
     * @return Identify if job is healthy.
     */
    public boolean evaluate(Job job, Scope scope) {
        return this.evaluate(job, false, scope);
    }

    /**
     * Evaluate a job.
     *
     * @param job Job.
     * @param prevalidation Identify if is a prevalidation.
     * @param scope Scope.
     * @return Identify if job is healthy.
     */
    public boolean evaluate(Job job, boolean prevalidation, Scope scope) {
        boolean log;
        boolean validated = true;

        //Identifies if the job has checkup.
        if (!job.getCheckup().isEmpty()) {
            //Log the job status before checkup evaluation.
            LOG.log(Level.INFO, "{} status before checkup evaluation", new Object[]{job.getName()});

            //Identifies the checkup on the status. 
            jobStatusService.updateFlow(job.getStatus(), Flow.CHECKUP);

            //Filters checkup by scope.
            List<JobCheckup> checkups = job.getCheckup()
                    .stream()
                    .filter(x -> (x.getScope().equals(scope) || x.getScope().equals(Scope.ANYONE)))
                    .filter(x -> (x.isPrevalidation() == prevalidation))
                    .collect(Collectors.toList());

            //Identifies if the job has checkups.
            if (!checkups.isEmpty()) {
                int retry = retryService.get(job);

                for (JobCheckup checkup : checkups) {
                    //Identifies the query scope and if it is enabled. 
                    if (checkup.isEnabled()) {
                        JobCheckupLog checkupLog = new JobCheckupLog();

                        checkupLog.setCheckup(checkup);
                        checkupLog.setQuery(checkup.getQuery());
                        checkupLog.setConditional(checkup.getConditional());
                        checkupLog.setAction(checkup.getAction());
                        checkupLog.setScope(checkup.getScope());

                        //Runs the query. 
                        String value = this.executeQuery(checkup);

                        //Compares value and threshold. 
                        validated = this.check(checkup, value);

                        //Identifies if is just a log. 
                        log = checkup.getAction().equals(Action.LOG_AND_CONTINUE);

                        //Identifies if should execute something. 
                        if (!validated) {
                            boolean commandResult = false;

                            //Executes the checkup command.
                            for (Command command : checkup.getCommand()) {
                                commandResult = this.executeCommand(checkup, command, checkupLog);

                                if (!commandResult) {
                                    break;
                                }
                            }

                            //Identifies if should revalidate the checkup.
                            if (commandResult) {
                                value = this.executeQuery(checkup);
                                validated = this.check(checkup, value);
                            }
                        }

                        //Defines the checkup status.                         
                        checkupLog.setThreshold(this.getMacro(checkup.getThreshold()));
                        checkupLog.setValue(value);
                        checkupLog.setSuccess(validated);

                        //Adds the log to the checkup.
                        checkup.addLog(checkupLog);
                        this.save(checkup);

                        //Identifies if should retry.
                        if (retry < (job.getRetry() == 0 ? 1 : job.getRetry())) {
                            if (!validated && !log) {
                                //Increases the retry counter.
                                retryService.increase(job);

                                //Executes the checkup related action. 
                                this.executeAction(job, checkup);
                            }
                        } else {
                            retryService.remove(job);
                        }

                        //Verify if this check failed. 
                        if (!validated) {
                            this.notify(checkup, value);

                            if (!log) {
                                break;
                            }
                        }

                        //Checked will be always true when is LOG_AND_CONTINUE.
                        if (log) {
                            validated = true;
                        }
                    }
                }

                if (validated) {
                    retryService.remove(job);
                }
            }

            //Log the job status after checkup evaluation.
            LOG.log(Level.INFO, "{} status after checkup evaluation", new Object[]{job.getName()});
        }

        return validated;
    }

    /**
     * Execute an action.
     *
     * @param job Job
     * @param checkup JobCheckup
     */
    @Async
    private void executeAction(Job job, JobCheckup checkup) {
        switch (checkup.getAction()) {
            case REBUILD:
                try {
                //Rebuild the job.
                jobStatusService.updateFlow(job.getStatus(), Flow.QUEUED);
                jenkinsService.build(job);
            } catch (Exception ex) {
                LogManager.getLogger(EyeService.class).log(Level.ERROR, "Fail building job: " + job.getName(), ex);
            }

            break;
            case REBUILD_MESH:
                HashSet<Job> parent = jobService.getMeshParent(job);
                jobService.rebuildMesh(job);

                try {
                    //Rebuild the mesh. 
                    for (Job meshParent : parent) {
                        jenkinsService.build(meshParent);
                    }
                } catch (Exception ex) {
                    LogManager.getLogger(EyeService.class).log(Level.ERROR, "Fail building job mesh: " + job.getName(), ex);
                }

                break;
            case REBUILD_TRIGGER:
                List<Job> trigger = checkup.getTrigger();

                try {
                    //Rebuild a selected job path. 
                    if (!trigger.isEmpty()) {
                        trigger.stream().map((jobFrom) -> jobService.getRelationPath(job, jobFrom)).forEach((jobPath) -> {
                            jobPath.stream().forEach((jobInPath) -> {
                                if (!job.equals(jobInPath)) {
                                    jobStatusService.updateFlow(jobInPath.getStatus(), Flow.REBUILD);
                                }
                            });
                        });

                        for (Job jobTriggered : trigger) {
                            jenkinsService.build(jobTriggered);
                        }
                    }
                } catch (Exception ex) {
                    LogManager.getLogger(EyeService.class).log(Level.ERROR, "Fail building trigger: " + job.getName(), ex);
                }

                break;
            default:
                retryService.remove(job);
                break;
        }
    }

    /**
     * Execute a checkup query.
     *
     * @param checkup JobCheckup.
     * @return Identify if the result match the threshold.
     */
    private String executeQuery(JobCheckup checkup) {
        String value = "";

        try {
            //Set a connection to database.
            jdbcTemplate.setDataSource(connectionService.getDataSource(checkup.getConnection()));
            jdbcTemplate.setMaxRows(1);

            //Execute a query. 
            value = jdbcTemplate
                    .queryForObject(
                            this.replaceParameter(checkup.getQuery()),
                            (ResultSet rs, int row) -> rs.getString(1)
                    );
        } catch (DataAccessException ex) {
            value = ex.getMessage();
        } finally {
            try {
                //Close the connection. 
                jdbcTemplate.getDataSource().getConnection().close();
            } catch (SQLException ex) {
                LOG.log(Level.ERROR, "Fail closing connection ", ex);
            }
        }

        return value;
    }

    /**
     * Execute a command.
     *
     * @param checkup JobCheckup.
     * @param command Command.
     * @return Identify if the command run successfully.
     */
    private boolean executeCommand(JobCheckup checkup, Command command, JobCheckupLog jobCheckupLog) {
        boolean success;

        //Identify the command type.
        if (command.getCommandType().equals(CommandType.SQL)) {
            success = this.sqlCommand(checkup, command, jobCheckupLog);
        } else {
            success = this.shellCommand(command, jobCheckupLog);
        }

        return success;
    }

    /**
     * Execute a sql command.
     *
     * @param checkup JobCheckup.
     * @param command Command.
     * @param jobCheckupLog JobCheckupLog.
     * @return Identify if the command run successfully.
     */
    private boolean sqlCommand(JobCheckup checkup, Command command, JobCheckupLog jobCheckupLog) {
        int affected;
        String log = "";
        boolean success = true;

        try {
            //Set a connection to database.
            jdbcTemplate
                    .setDataSource(
                            connectionService
                                    .getDataSource(checkup.getConnection())
                    );

            //Execute a query.
            affected = jdbcTemplate
                    .update(
                            this.replaceParameter(command.getCommand())
                    );

            //Log the affected rows.
            log = "Affected record[s]: " + affected;
        } catch (DataAccessException ex) {
            success = false;
            log = ex.getMessage();
            LOG.log(Level.ERROR, "Fail executing SQL command ", ex);
        } finally {
            try {
                //Close the connection.
                jdbcTemplate.getDataSource().getConnection().close();
            } catch (SQLException ex) {
                LOG.log(Level.ERROR, "Fail closing connection ", ex);
            }

            try {
                //Define the command log.
                CommandLog commandLog = commandLogService.save(
                        new CommandLog(
                                command,
                                log,
                                success));

                //Add the command log to checkup log.
                if (jobCheckupLog != null) {
                    jobCheckupLog.addCommandLog(commandLog);
                }
            } catch (Exception ex) {
                LOG.log(Level.ERROR, "Fail recording sql command log " + ex.getMessage(), ex);
            }
        }

        return success;
    }

    /**
     * Execute a Shell command.
     *
     * @param checkup JobCheckup.
     * @param command Command.
     * @param jobCheckupLog JobCheckupLog.
     * @return Identify if the command run successfully.
     */
    private boolean shellCommand(Command command, JobCheckupLog jobCheckupLog) {
        String log = "";
        boolean success = true;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            //Create a temp sh file.
            File tmp = File.createTempFile("hanger_", ".sh");

            //Define file permisssion.
            tmp.setExecutable(true);
            tmp.setReadable(true);
            tmp.setWritable(true);

            //Write shell command to sh file.
            try (FileWriter writer = new FileWriter(tmp)) {
                writer
                        .write(
                                this.replaceParameter(command.getCommand()).replaceAll("\r", "")
                        );
            }

            //Parse the command to run.
            CommandLine cmdLine = CommandLine.parse("sh " + tmp);

            //Define the executor.
            DefaultExecutor executor = new DefaultExecutor();

            //Define the timeout.
            executor.setWatchdog(new ExecuteWatchdog(300000));

            //Define the work directory.
            Path sandbox = Paths.get(System.getProperty("user.home") + "/.hanger/sandbox/");
            Files.createDirectories(sandbox);
            executor.setWorkingDirectory(new File(sandbox.toString()));

            //Define the log.
            PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream, outputStream, null);
            executor.setStreamHandler(streamHandler);

            //Execute the sh file.
            success = (executor.execute(cmdLine) == 0);

            //Get the command log. 
            log = outputStream.toString();

            //Delete the sh file.
            tmp.delete();
        } catch (IOException ex) {
            success = false;
            log = ex.getMessage();
            LOG.log(Level.ERROR, "Fail executing shell command " + ex.getMessage(), ex);
        } finally {
            try {
                //Define the command log.
                CommandLog commandLog = commandLogService.save(
                        new CommandLog(
                                command,
                                log,
                                success));

                //Add the command log to checkup log.
                if (jobCheckupLog != null) {
                    jobCheckupLog.addCommandLog(commandLog);
                }
            } catch (Exception ex) {
                LOG.log(Level.ERROR, "Fail recording shell command log " + ex.getMessage(), ex);
            }
        }

        return success;
    }

    /**
     * Compare the checkup threshold with a value.
     *
     * @param checkup JobCheckup.
     * @param value Value.
     * @return Identify if passed on test.
     */
    public boolean check(JobCheckup checkup, String value) {
        boolean checked = false;
        Float finalValue = null;
        Float finalThreshold = null;
        String threshold = checkup.getThreshold();

        //Remove white spaces. 
        value = (value == null) ? "" : value.trim();
        threshold = (threshold == null) ? "" : threshold.trim();
        threshold = this.getMacro(threshold);

        //Try to convert the value to float. 
        try {
            finalValue = Float.parseFloat(value);
        } catch (NumberFormatException ex) {
            LOG.log(Level.ERROR, "Fail converting value " + value + " to float", ex);
        }

        //Try to convert the threshold to float. 
        try {
            finalThreshold = Float.parseFloat(threshold);
        } catch (NumberFormatException ex) {
            LOG.log(Level.ERROR, "Fail converting threshold " + threshold + " to float", ex);
        }

        //Log the checkup value and threshold.
        LOG.log(Level.INFO, "Checkup {} evaluated with value {} and threshold {}", new Object[]{checkup.getName(), finalValue, finalThreshold});

        //Check if the threshold is numeric.
        if (finalValue != null && finalThreshold != null) {
            //Check if the resultset match the threshold.
            switch (checkup.getConditional()) {
                case EQUAL:
                    checked = Objects.equals(finalValue, finalThreshold);
                    break;
                case NOT_EQUAL:
                    checked = !Objects.equals(finalValue, finalThreshold);
                    break;
                case LOWER_THAN:
                    checked = finalValue < finalThreshold;
                    break;
                case LOWER_THAN_OR_EQUAL:
                    checked = finalValue <= finalThreshold;
                    break;
                case GREATER_THAN:
                    checked = finalValue > finalThreshold;
                    break;
                case GREATER_THAN_OR_EQUAL:
                    checked = finalValue >= finalThreshold;
                    break;
                default:
                    break;
            }
        }

        return checked;
    }

    /**
     * Identify if the threshold is a macro.
     *
     * @param threshold String
     * @return String macro value or threshold itself value.
     */
    private String getMacro(String threshold) {

        if (threshold.startsWith("${") && threshold.endsWith("}")) {
            //Remove all non numeric characters.
            String id = threshold.replaceAll("[^\\d.]", "");

            if (!id.isEmpty()) {
                JobCheckup checkupRelation = this.load(Long.valueOf(id));

                if (checkupRelation != null) {
                    if (!checkupRelation.getLog().isEmpty()) {
                        //Identifies the last checkup value evaluated.
                        threshold = checkupRelation.getLog()
                                .stream()
                                .max(Comparator.comparing(JobCheckupLog::getDate))
                                .get()
                                .getValue();
                    }
                }
            }
        }

        return threshold;
    }

    /**
     * Replace template parameters.
     *
     * @param template String
     * @return String Replaced template.
     */
    private String replaceParameter(String template) {
        JSONArray values = new JSONArray();
        Map<String, Map<String, String>> parameters = templateService.getParameters(template);

        if (!parameters.isEmpty()) {
            parameters.entrySet().forEach(entry -> {
                String id = entry.getValue().get("name");
                String value = entry.getValue().get("default");

                JobCheckup checkup = this.load(Long.valueOf(id));

                if (checkup != null) {
                    if (!checkup.getLog().isEmpty()) {
                        value = checkup.getLog()
                                .stream()
                                .max(Comparator.comparing(JobCheckupLog::getDate))
                                .get()
                                .getValue();
                    }
                }
                values.put(
                        new JSONObject()
                                .put("name", entry.getKey())
                                .put("value", value)
                );
            });

            template = templateService.setParameters(template, values);
        }

        return template;
    }

    /**
     * Checkup failure notification.
     *
     * @param checkup Checkup
     * @param value Evaluated value
     */
    public void notify(JobCheckup checkup, String value) {
        Job job = checkup.getJob();

        //Identifies if the job has approver. 
        if (job.getApprover() != null) {
            Blueprint blueprint = new Blueprint(job.getApprover().getEmail(), "Hanger Checkup failure", "checkupFailure");
            blueprint.addVariable("approver", job.getApprover().getFirstName());
            blueprint.addVariable("job", job.getName());
            blueprint.addVariable("checkup", checkup.getDescription());

            mailService.send(blueprint, checkup.getJob().getName());
        }

        //Identifies if checkup notification is enabled.
        if (job.isCheckupNotified()) {
            StringBuilder message = new StringBuilder();

            message
                    .append(":syringe: *")
                    .append(job.getDisplayName())
                    .append("* > *")
                    .append(checkup.getDescription())
                    .append("* > ")
                    .append(checkup.isPrevalidation() ? "PRE_VALIDATION" : "POST_VALIDATION")
                    .append(" > ")
                    .append(checkup.getAction())
                    .append(" > ")
                    .append("failed because the result was *")
                    .append(value)
                    .append("* but the expected is ")
                    .append(checkup.getConditional())
                    .append(" *")
                    .append(checkup.getThreshold())
                    .append("*");

            slackService.send(message.toString(), job.getChannel());
        }
    }
}
