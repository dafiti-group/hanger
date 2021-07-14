# Hanger [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
### Hanger is a graphical tool for process orchestration and data quality, responsible for the execution of [ETL](https://pt.wikipedia.org/wiki/Extract,_transform,_load) processes, dependency control and data validation.

## Instalation

##### REQUIREMENTS

- Tomcat 8 +
- MySQL 5.7 +
- Java 8 +
- Jenkins ( [https://jenkins.io/](https://jenkins.io/) )
- Jenkins Notification Plugin ( [https://wiki.jenkins.io/display/JENKINS/Notification+Plugin](https://wiki.jenkins.io/display/JENKINS/Notification+Plugin) )

##### CONSTRUCTION
Using [Maven](https://maven.apache.org/):

- Go to the directory where the Hanger sources are located.
- Type the **mvn package** command.
- The hanger.war file will be generated on the *target* subdirectory.

##### CONFIGURATION

- Create a database on Mysql/MariaDB and an user with full permition only for the database that will receive the Hanger's metadata.

```
# Example to run using Mysql/MariaDB root/admin user

# Create a database named hanger
CREATE DATABASE hanger default character set utf8 default collate utf8_bin;

# Create an user named hanger
CREATE USER hanger@localhost IDENTIFIED BY 'SetYourPassword';

# Releases hanger user access to the database which has the same name
GRANT ALL PRIVILEGES on hanger.* to 'hanger'@'localhost' identified by 'SetYourPassword';

# Update permitions
FLUSH PRIVILEGES;
```		


- Create the ~/.hanger/hanger.properties file with the follow content:

```
# Hanger MySQL
spring.datasource.url=jdbc:mysql://<host>:<port>/<db>?serverTimezone=GMT-3
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.username=<user>
spring.datasource.password=<password>
spring.datasource.validationQuery=SELECT 1
 
# Hanger Secret Key
hanger.encrypt.key=<Any key>
 
# Hanger Anonymous Access
hanger.anonymous.access=true

# Security
hanger.maximum.sessions=1

# Hanger watchdog cron execution
hanger.watchdog.cron=Cron expression (Example: 0 20 6-23 * * *. For more details visit: http://www.quartz-scheduler.org/documentation/quartz-2.3.0/tutorials/crontrigger.html)
 
# Log
logging.level.root=INFO
logging.level.org.springframework.web=WARN
logging.level.org.hibernate=INFO
logging.level.org.hibernate.SQL=WARN
 
# Timezone
spring.jackson.time-zone=America/Sao_Paulo
spring.jpa.properties.hibername.jdbc.time_zone=GMT-3
```

> To enable the slack integration, follow these documents:
> - Habilitar Slack bot e pegar token.
> - Habilitar Slack WebHook e pegar URL.

##### DEPLOY
Using [Apache Tomcat](http://tomcat.apache.org/):

- Copy the hanger.war file to the Apache Tomcat webapps directory.

##### TROUBLESHOOTING

> If there is a problem running a *job* through Hanger, verify if *Jenkins URL* field is set correctly in Jenkins:
> - Access Jenkins and go to the *Manage Jenkins* menu.
> - Click on *Configure System* option.
> - Setup the correct *Jenkins URL* on *Jenkins Location* section. 

> If there are permission issues between Hanger and Jenkins (Error 403), check if the option *Prevent site from intrusion* is checked and uncheck it:
> - Access Jenkins and go to the *Manage Jenkins* menu.
> - Click on *Configure Global Security* option.
> - On *CSRF Protection* section, uncheck the *Prevent site from intrusion* option.

## Monitor
*Monitor* is where the processes are monitored. The *dashboard* is organized as follows:

- On top are shown all the available *subjects* and a filter option:

	- At the left side of the *subject* name is displayed an icon that indcates that the logged user is subscribed. However, the icon only appears if the *subject* is not mandatory. A *subject* is defined as mandatory if the user wants that the *subject* is visible to all other Hanger users. 
	- To the right of the subject's name, the total number of *jobs* contained in it is informed.
	- Click on the chosen subject name to see the subject details.

		- The subject description is displayed.
		- Click on the ***Subscription*** checkbox to **subscribe** or **unsubscribe** on the current subject.

		- A list of all jobs related to the selected subject will be displayed with the following information:
			- **Status:** *job* status.
			- **Server:** Jenkins instance in which the *job* is executed.
			- **Job:** *job* name.
			- **Warning:** number of warning on *job* chain.
			- **Link:** link to redirect to *job* on Jenkins.
			- **Check-Up:** link to redirect to checkup execution list.
			- **Updated:** *job* execution date.
			- **Scope:** *job* execution scope.

		- Click on the displayed number on warning column and a list with all the jobs with problems and its respective status will be shown. 
		- Click on ***Build*** button to run a *job*.
		- Click on ***Flow*** button to be directed to the *job* dependency chain.
		- Click on ***See*** button to see the *job* details. 
		- Click on ***Remove*** button to remove a *job* from a *subject* 
		- Click on ***Edit subject*** option if you want to edit the *subject*.
		- Click on the ***Add Job*** option to add a *job* to the *subject*.

- On ***HOME*** tab the progress from each *subject* and the count of *jobs* by status will be displayed:

	- On graph: 
		- **Gray:** represents the percentage of jobs that have not yet been executed.  
		- **Green:** represents the percentage of jobs that have been successfully executed. 
		- **Red:** represents the percentage of jobs with failure or with problems on data validation.
		- **Orange:** represents the percentage of jobs with alerts in dependency chain. 

	- On table:
		- **Success:** total of jobs successfully executed.
		- **Waiting:** total of jobs not executed yet. 
		- **Building:** total of jobs ready to be executed. 
		- **Running:** total of jobs that are running.
		- **Warning:** total of jobs with alerts on dependency chain. 
		- **Failure:** total of jobs with failures or with problems on data validation. 
		- **Checkup:** health check validation of all the *job* checkups.

- On ***ALL*** tab are displayed all the registered jobs on Hanger and its respective status.

## Search

*Search* is the faster way to find and to access *job* status informations on Hanger.

- On the side menu, click on ***Search***.
- A search screen and a button ***Search*** with a magnifying glass symbol will be shown.
- Write the content to be searched and click on the button, it can be part of the name, full name or the *job* alias.

## Flow

*Flow* is a graphical representation of a *job's* dependency chain.

Each *job* represented on flow has a  status and the follow information:

- *Job* name.
- Link to *job* on Jenkins.
- Link to Jenkins console.
- Link to the *job* validation results page. 
	- Click on ***CHECKUP*** link and the page with the result of the last ten *job* checkups will be displayed.
	- Click on ***View*** button to see the *job* configuration.
	- Click on ***Flow*** button to be redirected to *job* dependency chain.
- Jenkins instance in which the *job* is executed.
- *Job* execution date.
- *Job* scope.
- Click on *job* status with the right button and a list will be displayed with the following options:
	- **Flow:** Shortcut to the selected *job flow*.
	- **Propagation:** Shortcut to the selected *job propagation*. 	
	- **Build history:**  Shows a list, limited by the Log retention configuration parameter, with the complete execution history of the selected *job*. The list contains these fields:
		- **Start:** Start date and time of execution.
		- **Finish:** Final date and time of execution.
		- **Duration in minutes:** Execution duration in minutes.
		- **Efficiency:**  Shows the percentage that the execution was queued.
	- **Actions:**
		- **Abort**: Allows to stop the selected *job* execution.
		- **Build:** Allows to execute the selected *job*. 
		- **Build Mesh:** Allows to execute all he selected *job* chain.
		- **Disable:** Allows to disable/enable a *job*.
		- **Rebuildable** Allows to set rebuildable or not the selected *job*.
		- **Parent:** Shows a list with all the Jenkins available servers, where it is possible to add one or more parents to the current *job*:	
			- Click on the chosen server.
			- A screen with all the available jobs in that server will be displayed.
			- It is possible to select the jobs on the list or simply type the name of the jobs on Jobs field (comma-separated).
			- Click on Add to conclude the action.
		- **Children:**  Shows a list with all the Jenkins available servers, where it is possible to add one or more children to the current *job*:
			- Click on the chosen server.
			- A screen with all the available jobs in that server will be displayed.
			- It is possible to select the jobs on the list or simply type the name of the jobs on Jobs field (comma-separated).
			- Click on Add to conclude the action.

During the Flow view, the available options are:

##### ZOOM OUT
Zoom out the flow.

##### ZOOM IN
Zoom in the flow.

##### EXPAND ALL
Expand all the *job* dependency chain.

##### COLLPASE ALL
Close all the dependency chain of *job*.

##### APPROVAL
Redirect to *job* approbation page. However, this option only will be available if the *job* status is BLOCKED or UNHEALTHY.

##### GLOSSARY
Shows the glossary of all the possible status of a *job* with the following information:

- ***Name*:**  Status name. 
- ***Description*:** Status description.

## Server
*Servers* are Jenkins instances that will be managed by Hanger. 

- To access the ***Server*** main page, on side menu, click on the ***Server*** option.
- All the servers will be displayed.
- If you want to edit a specific server, click on ***Edit*** button.

##### ADD SERVER
Allows to add a new Jenkins instance

- On ***Server*** side menu option, click on the ***down arrow*** and choose the ***Add server*** option.
- On ***Name*** field, define the server name.
- Inform the full server URL, with port and host on ***URL*** field.
- On ***Username*** field, inform the server administrator user.
- On ***Token*** field, inform either the password or the user [token](https://stackoverflow.com/questions/45466090/how-to-get-the-api-token-for-jenkins).
- Click on ***Save*** button to save.

##### IMPORT
This option allows to import all the *jobs* registered on Jenkins.

- On side menu, click on ***Server*** option.
- Select the chosen server and click on ***Import*** button.
- A confirmation message will be displayed. Click on ***Yes*** to confirm or click on ***No*** to cancel the operation.
- After the importation of all *jobs*, a message will be displayed on top informing that the *jobs* have been synced.

##### CONNECT
This option allows to test the connection of a registered Jenkins instance.

- On side menu, click on ***Server*** option.
- Select the server that you want to test and click on ***Connect*** button.
- A message will be displayed at the top, informing whether the server is connected or not.

##### EDIT
Allows to edit a *server*. This process is pretty similar to Add server.

- On side menu, click on ***Server***.
- Select the chosen server and click on ***Edit*** button.
- Fill in the information accordingly and, at the end, click on ***Save*** to save.

##### DELETE
Allows to remove a server.

- On ***Server*** main page, select a server that you want to remove and click on ***Delete*** button.

## Connection
*Connections* are the databases connections that can be used on data validation process and to do queries on Workbench.

- To access the ***Connection*** main page, on side menu, click on the ***Connection*** option.
- All the connections will be displayed.
- If you want to edit a specific connection, click on ***Edit*** button.

##### ADD CONNECTION
Allows to add a new connection.

- On ***Connection*** side menu option, click on the ***down arrow*** and choose the ***Add connection*** option.	
- On ***Name*** field, inform the connection name.
- Select the chosen database on ***Database*** field.
- On ***Class name*** field, inform the used class to execute the connection with a *Generic* database.
- On ***JDBC Url*** field, inform the URL to connect with JDBC. Must contain host and database.
- On ***Username*** field, inform the database user.
- On ***Password*** field, inform the user password.
- Click on ***Save*** button to save.

***Note:*** The supported databases and its JDBC Urls are:

    ***MYSQL:*** jdbc:mysql://<url>:<porta>/<database>
	***POSTGRES:*** jdbc:postgresql://<url>:<porta>/<database>
	***ATHENA:*** jdbc:awsathena://AwsRegion=<region>;S3OutputLocation=<bucket>
	***MSSQL:*** jdbc:sqlserver://<url>:<porta>;DataBaseName=<database>
	***HANA:*** jdbc:sap://<url>:<porta>/<database>
	***GENERIC:*** To use a generic connection, the URL must be put on ${CATALINA_HOME}/lib server directory and you have to inform the driver class name.

##### CHECK ALL CONNECTIONS
This option allows to validate the estate of all the registered connections. However, depending on the number of connections, this process can be time-consuming.

- On side menu, click on ***Connection*** option.
- At bottom, click on ***Check all connections***. 
- If all the connections are okay, a success message will be displayed.

##### CHECK
Allows to check a specific connection.

- On side menu, click on ***Connection*** option.
- Choose a connection to check and click on ***Check*** button.
- If the connection is okay, a success message will be displayed.

##### SCHEMA
This option allows to explore the catalogs and schemas of a connection.

- On side menu, click on ***Connection*** option.
- Select the chosen connection and click on ***Schema*** button.
- A table will be displayed with all the catalogs and schemas of that connection.
- Click on ***Table*** button to explore the chosen schema or catalog tables.
- Click on ***Column*** button to access the table metadata. Will be shown the primary key, index, fields and its types.

##### EDIT 
Allows to edit a *connection*. This process is pretty similar to Add connection.

- On side menu, click on ***Server***.
- Select the chosen connection and click on ***Edit*** button.
- Fill in the information accordingly and, at the end, click on ***Save*** to save.

##### DELETE
Allows to remove a connection.

- On ***Connection*** main page, select a connection that you want to remove and click on ***Delete*** button.

## Workbench
*Workbench* is a tool that allows to execute queries with all the available connections.

- On side menu, click on ***Workbench***.
- The ***Workbench*** page will be shown.
- Select the chosen ***connection*** on connection field.
- On ***Search*** field, search the database to be consulted or chose on the displayed list.
- Click on the chosen database name and, after that, click on the table name or schema to be consulted.
- After clicking on the name of the table or schema, a predefined query will be displayed in the text area on the side.
- It is possible to make changes to the query and then click on ***Play*** to execute it.
- The result will be displayed.

###### SAVE A QUERY
*Workbench* allows to save the query. To do that:

- Click on the menu button, located next to the ***Play*** button, in the lower right corner of the screen.
- Click ***Save a query*** button.
- In the ***Name*** field, enter the query name.
- The ***Connection*** field shows the connection chosen previously
- The ***Query*** field shows the query text to be saved and allows changes
- Click on ***Shared***, if you want to share the query with other Hanger users. 
- At the end, click on ***Save*** button to save the query.
- Click on ***Close*** to cancel the operation.

###### SAVED QUERIES
Allows to view all the saved queries.

- On ***Workbench*** side menu option, click on the ***down arrow*** and choose the ***Saved queries*** option.
- All the saved queries will be displayed.
- You can also view the saved queries through the ***Workbench*** page by clicking on the menu button, in the lower right corner of the screen and then choosing the ***Saved queries*** option.

***Note:*** queries saved by other users can only be viewed if they have been previously shared.

###### OPEN
This button allows to open a query and play it.

##### SEE
This button allows to view the query details.

***Note:*** On this page, you can click on ***Open in workbench*** button to execute the query o ***Workbench*** main page.
 
###### EDIT
This button allows to edit the information about a query.

- Inform the new query name on ***Name***  filed.
- The ***Connection*** field informs the current connection, but you can change it.
- In the text area ***Query***, the query to be edited is informed.
- Click on ***Shared***, if you want to share the query with the other Hanger users.
- At the end, click on ***Save***.
- Click on ***Close***, if you want to cancel the operation.

###### DELETE
This button allows to delete a query.

###### EXPORT AS CSV
To export the query result to a CSV file, you must click on ***Export as csv*** button on ***Workbench*** page.

###### SEND E-MAIL
On *Workbench* it is possible to send the resultset by e-mail to other users.

- On ***Workbench*** page, click on ***Save an e-mail*** button, available on side menu next to ***Play*** button.
- Inform the recipients in the ***Recipient*** field.
- Inform recipients external to Hanger in the ***External Recipient*** field.
- Inform the e-mail subject on ***Subject*** field.
- If it is necessary, write some message on ***Body*** field.
- Click on ***Send*** button to send.
- Click on ***Save*** to save the e-mail and send it later.
- Click on ***Close*** to cancel the operation.

**Note:** after clicking ***Save***, the page will be redirected to ***Saved e-mails***, where all saved e-mails are located.

###### SAVE AN E-MAIL
On *Workbench* is it possible to save an e-mail to send it later.

- In ***Workbench***, click on the ***Save an email*** button available in the menu next to the ***Play*** button.
- Fill in the fields presented previously.
- Click on ***Save*** para salvar o e-mail.
- After clicking on ***Save***, the page will be redirected to the ***Saved e-mails*** page.

###### SAVED E-MAILS
On *Saved e-mails* page you can view a list with all the saved e-mails.

- On ***Workbench*** side menu option, click on the ***down arrow*** and choose the ***Saved e-mails*** option.
- All the saved e-mails will be displayed.
- Click on e-mail Subject to see more details.
- After clicking on e-mail subject, the *jobs* related to this e-mail will be shown.
- In this page, you can send an saved e-mail by clicking on ***Send*** button.
- A success message will be displayed as soon as the email is sent.

**Note:** You can also view the saved e-mails through the ***Workbench*** page by clicking on the menu button, in the lower right corner of the screen and then choosing the ***Saved e-mails*** option. 

###### ADD EMAIL
On *Saved e-mails* page you can also add a new e-mail to the list.

- Click on ***+ Add Email*** and fill in the informations.
- Click on ***Save*** to save, then click on ***Go back*** to return to the saved e-mails page.
- You can send this new e-mail anytime by clicking on ***Send*** button.

###### OPEN WORKBENCH
If you are either in ***Saved queries*** page or in ***Saved e-mails*** page, you can return to the ***Workbench*** main page by clicking on ***Open workbench*** option.

## Subject
*Subjects* are groupers used for organizing and follow the *jobs* execution process.

- To access the ***Subject*** main page, on side menu, click on the ***Subject*** option.
- All the subjects will be displayed.
- If you want to edit a specific subject, click on ***Edit*** button.

##### EDIT
Allows to edit a subject. This process is pretty similar to Add Subject.

- On side menu, click on ***Subject***.
- Select the chosen subject and click on ***Edit*** button.
- Fill in the information accordingly and, at the end, click on ***Save*** to save.

##### ADD SUBJECT
Allows to add a new subject.

- On ***Subject*** side menu option, click on the ***down arrow*** and choose the ***Add subject*** option.
- On ***Subject*** field, inform the subject name.
- Define the subject description on ***Description*** field, if it is necessary. You can use *Markdown* language to write it.
- On ***Subscription*** section, check the option ***Mandatory***, if you want that the subject can be seen on monitor by all the users. Some subjects are required and can not be removed from monitor, so, these subjects are shown with the subscription checkbox disabled.
- Check the option ***Slack notification***, if you want to receive notifications of jobs belonging to this subject on slack.
- Click on ***Slack Channel*** button to define the channel that the notifications will be sent. A modal will be displayed and you just only have to select the chosen channels and click on ***Add***.
- Click on ***Swinlanes*** button to group the *jobs* that is in the same subject, by using *regexp*. This is a very useful resource when to subdivide the *jobs*.
- At the end, click on ***Save***.

***Note:*** if no channel is selected in the ***Slack channels*** modal, the channel configured on the ***Configuration*** tab will be used for notification of this *job*.

##### DELETE
Allows to remove a subject.

- On ***Subject*** main page, select a subject that you want to remove and click on ***Delete*** button.

## Jobs
On Hanger, *Jobs* are references to [Jenkins](https://jenkins.io/) *jobs*.

- To access the ***Job*** main page, on side menu, click on the ***Job*** option.
- All the jobs will be displayed.
- If you want to edit a specific *job*, click on ***Edit*** button.

##### ADD JOB
Allows to add a new *job*. You can import a *job* from Jenkins, create a new one by Hanger or migrate one from a specific server to another one.

##### IMPORT
- On ***Job*** side menu option, click on the ***down arrow*** and choose the ***Add job*** option.
- On top, select the option ***Import*** to import a *job* from Jenkins.
- On ***Server*** field, inform the chosen Jenkins instance.
	> To add a new *job*, you must have at least one registered server.
- Select the *job* on ***Name*** field.
- Click on ***Next*** button to continue.
- On ***Alias*** field it is possible to define a suggestive name for the *job*. The defined alias will replace the *job* name on mainly Hanger functionalities. 
- If you want, on ***Description*** field, define the *job* description.
	> You can use ***Markdown*** to type the description text.
- On ***Time Restriction*** field, inform the allowed period in [cron](http://www.quartz-scheduler.org/documentation/quartz-2.3.0/tutorials/crontrigger.html) format to execute the *job*.
- Define the content of the ***Eagerness*** field of the *job* between 0 and 12 hours. For a *job* dependency to be considered attended it must have been successfully carried out at least once a day. If some dependency wants to be executed before midnight, the ***Eagerness*** field must be filled with the number of hours, before midnight, so that, if the *job* runs successfully, the dependency is considered fulfilled.
- If you want to define a minimum interval to reexcute the *job*, write the minutes on ***Rebuild Interval*** field.
- On ***Node*** field, inform the node where the *job* will be executed.
- On ***Cron*** field, inform the trigger period for the job.
- On ***Blocking jobs*** field, inform the jobs that must be completed so that the job can be executed.
- On section ***Options***, check the option ***Enabled*** to enable the *job* or uncheck to disable it. 
- Check the ***Rebuildable*** option if it is necessary to execute the *job* more than once a day. When this option is checked, the *job* will be executed for the first time when all the dependencies are catered and will be executed again always when, during the day all the dependencies are successfully catered. If it is necesssary to execute the *job* again only when all the dependencies are successfully catered again, select which dependencies will be blockers on parents list. For default, a *job* can be executed only once a day.
- Check the option ***Build on any parent scope*** to allow that the *job* can be pushed, regardless of the parents execution scope.
- Click on ***Parent*** button if you want to add a dependency to the current *job*.
- Click on ***Shell Script*** button if you want to define or change the *job* shell script.
- Click on ***Subject*** button to classify the *job* in one or more subjects.
- Click on ***Checkup*** button to add a checkup to the *job*.
- Click on ***Slack*** button to receive notifications abou the *job* on Slack.
- Click on ***E-mail*** button if you want to relate an e-mail to the *job*.
- At the end, click on ***Save*** button to save.

##### PARENT
A dependency is nothing more than a *job* defined as the *parent* of another specific *job*. In other words, this means that in order for the *job* to be executed, it is necessary for its *parents* to be executed first.

- On the ***Add job*** page, click on ***Parent*** button.
- A modal will be displayed.
- Select the *jobs* that you want to define as *parents* for the current *job*.
- Check the ***Import upstream project*** option, if the *jobs* on Jenkins have upstream *jobs*, so that the list of dependencies represented by Jenkins upstream *jobs* is replicated to Hanger's dependency engine.
- Click on ***Add*** button to add the selected parents.
- Click on ***Close*** if you want to cancel the operation.

Once the *job* has some *parent*, you may need to change some dependencies's settings.

- On side menu, access the ***Job*** option.
- Select the *job* that you want to configure.
- Click on ***Edit*** button.
- If the *job* needs to be executed more than once a day, but only after all the dependencies are completed again, it is necessary to select which dependencies will be ***Blockers***. For that, you just need to check the ***Blocker*** option.
- Click on ***Build history*** button, represented by the "***+***" symbol, to view the *job's* build history.
- You also may need to change some dependency's scope. You can do that by selecting the scope that you want on ***Scope*** field.
	- We can say that scopes are like *job* execution categories, that is, the scope determines the type of execution of the *job* or dependency. There is three Scopes options in the Hanger:

		- ***FULL***: Identifies that all dependencies with FULL or PARTIAL scope must be met before the *job* can be executed.
		- ***PARTIAL***: Identifies that all dependencies with PARTIAL scope must be met before the *job* can be executed. The *job* will run with the partial scope, as not all of its dependencies have been met.
		- ***OPTIONAL***: Identifies that the dependencies is optional, that is, whether it is answered or not, the *job* will run normally. If all dependencies for a *job* have the OPTIONAL scope, it will be executed as soon as the first dependency is met.
	- In short, in this field just inform which of the scopes above the *job* has.

- If you want to remove some parent from the mainly *job*, just click on ***Remove*** button.

Finally, just run the *job* and view the *Flow* to confirm that everything went as expected.

Note: on ***Flow*** is possible to see all the *job's* dependencies in chain, as well as it execution process.

##### SHELL SCRIPT
This option allows to apply a template on the *job*.
On the ***Add job*** page, click on ***Shell Script button*** button, if you want to apply shell script commands on the *job*.

- A text area named ***Shell Script*** will be created.
- Inform the shell script commands that you want.
- Click on ***Remove*** button to delete the shell script, if it is necessary.

***Note***: rather than create the shell script commands, you can apply a ***Shell Script template*** by clicking on the ***up arrow*** button, then select the template that you want to use.

##### SUBJECT
On the ***Add job*** page, click on ***Subject*** button, if you want to classify the *job* in one or more subjects. The ***Subject*** modal will be displayed that allows to select one or more subjects for the *job*.

- After select the chosen subjects, click on ***Add*** button to add it.
- A section called ***Subjects*** will be created, where it is possible to manage the *job* subjects.
- Click on ***Remove*** button to delete a subject, if it is necessary.

##### CHECKUP
If it is necessary execute the validation of the data from *job* execution, on the ***Add job*** page click on ***Checkup*** button. Will be created a new section called *Checkup* that allows to define a SQL instruction for validation. Follow the steps below to create a checkup:

- On field ***Name***, inform the *checkup* name.
- On ***Description*** field, inform checkup description.
- On ***Connection*** field, select in which connection the validation will be executed.
- Define in which scope the validation will be executed. For that, select one of the ***Scope*** options:
	- ***FULL***: the *job* checkup will be executed only when all the dependencies with FULL and PARTIAL scope are concluded.
	- ***PARTIAL***: the *job* checkup will be executed only when all the dependencies with PARTIAL scope are concluded.
	- ***ANYONE***: the *job* checkup will be executed always when the dependencies with FULL or PARTIAL scope are concluded.
- Define the SQL validation instruction on ***SQL Select Statement*** field, is it possible to reference the result of another ckeckup, using the following sintax: ${{checkup ID}}.The resultset must return only an integer value.
- Select the condition on ***Condition***.
- Define the expected result on Threshold field. Is it possible to reference the result of another ckeckup, using the following sintax: ***${checkup ID}***.
- On ***On fail*** field, select the action that will be executed, whether the validation fails. The available options are:
	- ***NOTHING***: just block the chain execution.
	- ***REBUILD***: if it is a pos validation, the *job* is executed again. Otherwise, it has the same effect of NOTHING.
	- ***REBUILD_MESH***:  if it is a pos validation, all the *job* dependencies and the dependencies of each dependency are executed again. Otherwise, it has the same effect of NOTHING.
	- ***REBUILD_TRIGGER***: if it is a pos validation, all the specific dependencies are executed again by clicking on Add Trigger button. Otherwise, it has the same effect of NOTHING.
	- ***LOG_AND_CONTINUE***: record the checkup execution result and notify the selected Slack channels Slack. However, it does not block the chain execution.

	***Note***: To actions like ***NOTHING*** or ***LOG_AND_CONTINUE*** there is not attempts number.

- If the chosen option is ***REBUILD_TRIGGER***, click on ***Add Trigger*** button. The modal ***Trigger*** will be displayed and will be possible to select which dependencies must be executed again.
- If it is necessary to execute some comand when the checkup fails, click on ***Add Command*** button. The new section, ***Script on failure*** will be displayed:
	> Select the action between ***SHELL*** or ***SQL*** to be executed.
	> On ***Script*** field, inform the action to be executed. Is it possible to reference the result of another ckeckup, using the following sintax: ${{checkup ID}}
- A checkup can be executed as a pre validation or a pos validation. Pre validations happens when all the dependencies have been fullfilled and immediately before of the *job* execution, while pos validations happen after the *job* execution. To define the type of a checkup, click on toggle and chose between ***Pre-validation*** e ***Post-validation***.
- On ***Retries***, define the number of attempts of *job* execution.
- If it is necessary, select an approver for the checkup on ***Approver*** field.
- At the end, click on ***Save*** button to save.

##### SLACK
If you want to receive notifications about the *job* executions, click on ***Slack*** button.

- On the ***Add job*** page, define the channel that the notifications will be sent. The modal ***Slack*** will be displayed. Then, select one or more channels on Slack that the notifications must be sent.
	> If no channels are selected on ***Slack*** modal, the configured channel on ***Configuration*** side menu option, will be used as default channel to this *job* notification.
- After select the chosen channels, click on ***Add*** button to add it.

##### E-MAIL
This option allows to relate an specific e-mail to the current *job*.

- On the ***Add job*** page, click on ***E-mail*** button.
- The model ***E-mails list*** will be displayed. Select the e-mails that you want to relate to the *job* and click on ***Add*** button.
- The selected e-mails will be sent to the recipients as soon as the *job* has been successfuly executed, that is, as soon as the *build* has been carried out.
	> If you want to see more details, click on e-mail's subject and the page will be redirected to the informations about the chosen e-mail.

##### CREATE
- On ***Job*** side menu option, click on the ***down arrow*** and choose the ***Add job*** option.
- On top, select the ***Create*** option to create a *job* directely by Hanger.
- On ***Server*** field, inform the chosen Jenkins instance.
	> To add a new *job*, you must have at least one registered server.
- Select the *job* template on ***Template*** field.
- Inform the *job* name on ***Name*** field.
- Click on ***Next*** button to continue.
- After that, the proccess is pretty similar to the ***Import*** option.

##### MIGRATE
- On ***Job*** side menu option, click on the ***down arrow*** and choose the ***Add job*** option.
- On top, select the ***Migrate*** option to migrate a *job* from a server to another.
- On ***Source server*** field, inform the origin server.
- On ***Job*** field inform and select the *job* that you want to migrate.
- On ***Target server*** field, inform the destiny server.
- On ***Template*** field, select the formatting for the *job*.
- You can set the *job* name on ***Name*** field.
- Click on ***Next*** button to continue.
- After that, the proccess is pretty similar to the ***Import*** option.


##### BUILD 
This option allows to run a *job*.  

##### SEE
*See* option allows to view the *job* configurantion.

- On side menu, click on ***Job***.
- Select the chosen *job* and click on ***See*** button.

On view page this actions are available:

- Click on ***Edit*** button to edit the *job*.
- Click on ***Delete*** button to delete the *job*.
- Click on ***Propagation*** button to analyze which *jobs* are impacted by the *job* on dependencies chain.
- Click on ***Flow*** button to analyze which *jobs* that are part of the *job* dependencies chain.
- Click on ***Build*** button to execute the *job*.
- Click on ***Build mesh*** button to execute the *job* dependencies chain.

##### EDIT 
Allows to edit the *job* settings.

##### DELETE
Allows to delete a *job*.

##### SHELL SCRIPT TEMPLATE 
This option allows to create shell scripts template for the *job*. You can use it in any *job* that you want.

- On ***Job*** side menu option, click on the ***down arrow*** and choose the ***Shell Script template*** option.
- All the available templates will be displayed.
- You can view the template details by clicking on ***See*** button.
- If you want to edit a template, just click on ***Edit*** button. This process is similar to ***Add template***.
- You can remove a template by clicking on ***Delete*** button.

If you want to create a new template, click on ***Add template*** option.
- On ***Name*** field, inform the server name.
- You can describe the template on ***Description*** field.
- On ***Template*** text area, inform the shell script commands that you want.
- At the end, click on ***Save*** button to save.

##### GANTT
This option allows to view the behavior of a *job*, that is, whether or not it has queued and its execution time, in graph format.

- On ***Job*** side menu option, click on the ***down arrow*** and choose the ***Gantt*** option.
- The ***Gantt*** page will be displayed.

To view the behavior of the requested *job*, you must:
- On ***Period*** field, define the period to be analyzed.
- On ***Job*** field, select the *job* to be analyzed.
- Click on ***Filter*** button.

## Log
*Logs* are records of the executed activities by all the Hanger system users. To use the *Log* option, follow these steps.

- On the side menu, click in ***Log***.
- On top, select the chosen period to see the logs.
- Click the ***Filter*** button and all records will be displayed in a table.

Each logs table field has a meaning:

- ***Date:*** Action date.
- ***Type:*** Action type.
- ***User:*** User who did the action.
- ***Data:*** Changes that were made.

## User
*Users* are the users of Hanger.

- On side menu, click on ***User*** option.
- All the Hanger users will be displayed.

##### ADD USER
This option allows to add a new user on Hanger.

- On ***User*** side menu option, click on the ***down arrow*** and choose the ***Add user*** option.
- On ***E-mail*** field, inform the user e-mail address.
- On ***Username*** field, define the username.
- On ***First Name*** field, inform the user first name.
- On ***Last Name*** field, inform the user last name.
- On ***Role*** field, define the user level.
- Check the ***Enabled*** option to enable the user on system. If the option is unchecked, the user becomes inactive.
- Click on ***Privileges*** button to set the user's privileges.
- Click on ***Save*** button to save.

***Note:*** the user levels, that is, the ***Roles*** present in the Hangar are:

**HERO:** is the integral system administrator. A Hero user can execute all the available operations. 

**ADMIN:** this user can execute all the operations in Hanger, except create, edit or delete other users.

**USER:** this user can build a *job* and approve *jobs* that are in their name on ***Approval*** section.

##### EDIT
Allows to edit an user. This process is pretty similar to Add user.

- On side menu, click on ***User***.
- Select the chosen user and click on ***Edit*** button.
- Fill in the information accordingly and, at the end, click on ***Save*** to save.

##### DELETE
Allows to remove an user.

- On ***User*** main page, select an user that you want to remove and click on ***Delete*** button.

##### API TOKEN
This option allows the user to have their own Glove api access key. The generated token has no determined validity and can be used until the user chooses to renew it.

- To renew the token click on ***Refresh Token***.
- A new token will generated and you can view it by clicking again on ***API Token*** menu option.

##### CHANGE PASSWORD
This option allows the user to change their own password.

- On ***User*** side menu option, click on the ***down arrow*** and choose the ***Change password*** option.
- Fill in the information accordingly and click on ***Save*** button to save.

## Configuration
In *Configuration* are all the global Hanger configurations. To set according to your need, follow the guidelines below:

- On side menu, click on ***Configuration***.
- On ***Host*** field, inform the server used to send e-mails.
- Write the e-mail server port on the field ***Port***.
- On ***Address*** field, inform the e-mail address.
- Write the password on the ***Password*** field.
- On Log Retention field it is possible to define, in days, the period for cleaning the approvals and data validation log.
- On ***Default Channel*** field, inform the default channel that will be used by Slack.
- On ***Schema and table searchable*** field, define the maximum number of entities allowed on Workbench.
- Set the allowed domains to send e-mails on the ***E-mail filter (RegExp)*** field. If it is empty, any domain is allowed.  
- Define the maximum number of rows per query on the ***Max rows per query field***.
- Click on ***Update Logo*** button to change the tool logo to any image file.
- Click on ***Update cache*** button if you want to update the cache.
- Click on ***Update plugin*** button to update the notification plugin.
