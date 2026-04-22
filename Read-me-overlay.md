#Opencell with talend integration 
This is special branch for building a war with support for talend jobs. 

## Extending the opencell application
The Goal of this document to introduce the recommended methodology to extend the opencell application. Extending opencell may be useful in some projects to:
* define new classes and shared code between scripts
    * implement a specific business logic 
    * reduce script's complexity and code length
* define new rest services 
* extend the database model
* define new job templates 

### how to?

The code will be encapsulated in a **jars** file that will be **packaged** in the **opencell war**.
So when the project is ready :  
* change the version of opencell in the pom.xml
* create  yours apis, dtos, jobs, and so on... 
* add needed opencell's dependencies 
* generate the war file with `mvn clean package`.
* copy the `opencell-overlay/target/opencell.war` to wildfly deployment directory
* start the application server.



## 1. clone the meveo project template
Clone the [maven template project ](https://opencell.assembla.com/spaces/meveo-enterprise/git-6/source): 

Here is the structure of the tempate project
![](https://opencell.assembla.com/spaces/dE8Z0QjqOr5zBcdmr6bg7m/documents/cihg6mou8r6OLZaIC_Qgzw/download/cihg6mou8r6OLZaIC_Qgzw)

In the pom.xml, use needed opencell modules as dependencies with provided scope.

```xml
<dependencies>
    <dependency>
        <groupId>com.opencellsoft</groupId>
        <artifactId>opencell-admin-ejbs</artifactId>
        <version>${opencell.version}</version>
        <scope>provided</scope>
    </dependency>
    <dependency>
        <groupId>com.opencellsoft</groupId>
        <artifactId>opencell-api</artifactId>
        <version>${opencell.version}</version>
        <scope>provided</scope>
    </dependency>
    <!-- others dependencies -->
</dependencies> 
```

## 2. Define a Custom class :
A custom class can be defined, then used in **opencell scripts**. This class can use opencell services, APIs and do any database operation.

Below is a sample of a class that uses opencell services and can be called from a script. 
```java
public class CustomClassUsedInScript {
    private final Logger log = LoggerFactory.getLogger(CustomClassUsedInScript.class);
    private AccessService accessService = (AccessService) getServiceInterface("AccessService");
    private SubscriptionService subscriptionService = (SubscriptionService) getServiceInterface("SubscriptionService");

    public void addAccess(String code) {
        log.info("adding access to subscription");

        Subscription subscription = subscriptionService.findByCode(code);
        if (subscription == null) {
            throw new BusinessException(String.format("subscription code %s not found ", code));
        }
        log.info("subscription found ={}", subscription);

        log.info("creating access from {}", subscription.getCode());

        Access access = new Access();
        access.setSubscription(subscription);
        access.setAccessUserId(subscription.getCode());
        access.setStartDate(subscription.getSubscriptionDate());
        access.setEndDate(null);
        accessService.create(access);
        log.info("Access created {}", access);
    }
}
```

## 3. Create a new REST Endpoint. 
### 3.1 create the DTO :
```java
public class PersonDto {
  
  private Long id;
  private String name;
  private String firstname;
  private Integer age;
}
```

### 3.2 Create an API Service :

The api class must extend `BaseCrudApi` : 
```java
@Stateless
public class PersonApi extends BaseCrudApi<Person, PersonDto> {

    @Inject
    private PersonService personService;

    public PersonDto find(Long id) throws MeveoApiException {
        Person person = personService.findById(id);
        if (person == null) {
            throw new EntityNotFoundException(id);
        }
        return toDTO(person);
    }
}
```


### 3.3 Create the REST interface : 

The interface:
* must extend the `IBaseRs`
* must be in the package  `org.meveo.api.rest.X`
* will have the full path like [http://server:port/opencell/api/rest/extended/person/](http://server:port/opencell/api/rest/extended/person/)

```java
package org.meveo.api.rest.person;

// import ....;

@Path("/extended/person")
@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
public interface PersonRs extends IBaseRs {

    @GET
    @Path("/")
    PersonResponseDto find(@QueryParam("code") String code);

    @PUT
    @Path("/")
    ActionStatus create(PersonDto personDto);

    @POST
    @Path("/")
    ActionStatus update(PersonDto personDto);

    @DELETE
    @Path("/")
    ActionStatus delete(@QueryParam("code") String code);
}
```
### 3.4 Implement the REST interface : 
The class:
* must extend the `BaseRs`
* must be in the package  `org.meveo.api.rest.X.impl`

```java
package org.meveo.api.rest.person.impl;

// import ....;

@RequestScoped
@Interceptors({ WsRestApiInterceptor.class })
public class PersonRsImpl extends BaseRs implements PersonRs {

    @Inject
    private PersonApi personApi;

    public ActionStatus create(PersonDto personDto) {
        ActionStatus result = new ActionStatus(ActionStatusEnum.SUCCESS, "");
        try {
            personApi.create(personDto);
        } catch (Exception e) {
            processException(e, result);
        }
        return result;
    }
}
```

## 4. Create a job template 
### 4.1 Define a job bean : 
To define the class: 
* the job bean must extend the `BaseJobBean` class
* any opencell service can be injected
* to get CustomField use `this.getParamOrCFValue(jobInstance, "CF_Code", 0L);`
```java
@Stateless
public class PersonJobBean extends BaseJobBean {

    @Inject
    protected Logger log;

    @Inject
    private PersonService personService;

    @Inject
    private JobExecutionService jobExecutionService;

    @Interceptors({ JobLoggingInterceptor.class, PerformanceInterceptor.class })
    @TransactionAttribute(TransactionAttributeType.NEVER)
    public void execute(JobExecutionResultImpl result, JobInstance jobInstance) {
        log.info("Running for parameter={}", jobInstance.getParametres());
        Long nbRuns = (Long) this.getParamOrCFValue(jobInstance, "nbRuns", -1L);
        //...
        Long waitingMillis = (Long) this.getParamOrCFValue(jobInstance, "waitingMillis", 0L);
        String codePrefix= (String) this.getParamOrCFValue(jobInstance, "codePrefix", "");

        try {
           result.setNbItemsToProcess(personList.size());
           //...
           // put your code here 
           // ....
            result.setNbItemsProcessedWithError(errors);
            result.setNbItemsCorrectlyProcessed(success);
            result.registerSucces();
        } catch (Exception e) {
            log.error("Failed to run person", e);
        }
    }
}
```
### 4.2 Define the job : 
This job implementation class: 
* must extend the Job class
* must choose the job category of the job from the `JobCategoryEnum`.
* add all custom fields that you will need in your job. 

```java
@Stateless
public class PersonJob extends Job {

    @Inject
    private PersonJobBean personJobBean;

    @Override
    @TransactionAttribute(TransactionAttributeType.NEVER)
    protected void execute(JobExecutionResultImpl result, JobInstance jobInstance) throws BusinessException {
        personJobBean.execute(result, jobInstance);
    }


    @Override
    public JobCategoryEnum getJobCategory() {
        return JobCategoryEnum.INVOICING;
    }


    @Override
    public Map<String, CustomFieldTemplate> getCustomFields() {
        Map<String, CustomFieldTemplate> result = new HashMap<String, CustomFieldTemplate>();

        CustomFieldTemplate customFieldNbRuns = new CustomFieldTemplate();
        customFieldNbRuns.setCode("nbRuns");
        customFieldNbRuns.setAppliesTo("JobInstance_PersonJob");
        customFieldNbRuns.setActive(true);
        customFieldNbRuns.setDescription(resourceMessages.getString("jobExecution.nbRuns"));
        customFieldNbRuns.setFieldType(CustomFieldTypeEnum.LONG);
        customFieldNbRuns.setValueRequired(false);
        customFieldNbRuns.setDefaultValue("-1");
        result.put("nbRuns", customFieldNbRuns);

        CustomFieldTemplate customFieldNbWaiting = new CustomFieldTemplate();
        customFieldNbWaiting.setCode("waitingMillis");
        customFieldNbWaiting.setAppliesTo("JobInstance_PersonJob");
        customFieldNbWaiting.setActive(true);
        customFieldNbWaiting.setDescription(resourceMessages.getString("jobExecution.waitingMillis"));
        customFieldNbWaiting.setFieldType(CustomFieldTypeEnum.LONG);
        customFieldNbWaiting.setDefaultValue("0");
        customFieldNbWaiting.setValueRequired(false);
        result.put("waitingMillis", customFieldNbWaiting);

        CustomFieldTemplate customFieldNbPersons = new CustomFieldTemplate();
        customFieldNbPersons.setCode("randNumber");
        customFieldNbPersons.setAppliesTo("JobInstance_PersonJob");
        customFieldNbPersons.setActive(true);
        customFieldNbPersons.setDescription("Number of person to create");
        customFieldNbPersons.setFieldType(CustomFieldTypeEnum.LONG);
        customFieldNbPersons.setDefaultValue("10");
        customFieldNbPersons.setValueRequired(false);
        result.put("randNumber", customFieldNbPersons);

        CustomFieldTemplate recordVariableName = new CustomFieldTemplate();
        recordVariableName.setCode("codePrefix");
        recordVariableName.setAppliesTo("JobInstance_PersonJob");
        recordVariableName.setActive(true);
        recordVariableName.setDefaultValue("record");
        recordVariableName.setDescription("Record variable name");
        recordVariableName.setFieldType(CustomFieldTypeEnum.STRING);
        recordVariableName.setValueRequired(true);
        recordVariableName.setMaxValue(50L);
        result.put("codePrefix", recordVariableName);

        return result;
    }
}
```

## 5. Adding a new business class
Sometimes we may need to add new models to opencell, it is recommended to avoid overusing this capability because features may already exists in opencell.

### 5.1 Define the entity :
To add a new model class to opencell, these steps must be followed:
* create a class in the module`opencell-elec-model`
* must implement `org.meveo.model.IEntity`
* annotate the class with `@Entity`
* specify the table name with `@Table`
* use `Sequence Generator` to generate identifier values based on a sequence-style database structure.
* you must create the table and sequence for that class manually. 

Below is an entity example : 

```java
@Entity
@Table(name = "person")
public class Person implements IEntity {

    @Column(name = "id")
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "person_seq")
    @SequenceGenerator(name="person_seq", sequenceName="person_seq", allocationSize=100)
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "firstname")
    private String firstname;

    @Column(name = "age")
    private Integer age;
//...
// constructors 
// getters and setters
}
```
### 5.2 adding a model jar
the current file `opencell-overlay/src/main/resources/META-INF/persistence.xml`,  will add all opencell classes model and the current client model. 

```xml
<persistence-unit name="MeveoAdmin">
        <jta-data-source>java:jboss/datasources/MeveoAdminDatasource</jta-data-source>
        <jar-file>lib/opencell-model-${opencell.version}.jar</jar-file>
        <jar-file>lib/opencell-elec-model-${project.version}.jar</jar-file>
        <!--....-->
</persistence-unit>
```

### 5.3 Create the script the table definition Script : 
Here is an example of the table creation and the sequence : 
* the id must be of type `bigint`
* Full example : 
```sql
create table person
(
  id          bigint not null constraint person_pk primary key,      
  name        varchar(255),
  firstname   varchar(255),
  age         integer
);

alter table person owner to meveo;
create sequence person_seq;
alter sequence person_seq owner to meveo;
```

### 5.4 Create a business layer and data access layer.
The business service must extend the **PersistenceService** class that provides the implementation for persistence methods (CRUD).
Example : 
```java
@Stateless
public class PersonService extends PersistenceService<Person>  {
    // other methods
}
```

## 6. how to debug
### 6.1 Configure wildfly server :  

* create a wildfly server configuration 

![](https://www.assembla.com/spaces/cRAcZ4D1Cr4PP6acwqjQWU/documents/dTLk4koU4r6RJccK-zJOy8/download/dTLk4koU4r6RJccK-zJOy8)

* add the war in the deployement section using a war (external source): 
  * click on the plus button and select External source  ![](https://www.assembla.com/spaces/cRAcZ4D1Cr4PP6acwqjQWU/documents/dOQGHSoU8r6QRcbK8JiBFu/download/dOQGHSoU8r6QRcbK8JiBFu)

  * select the war file ![](https://www.assembla.com/spaces/cRAcZ4D1Cr4PP6acwqjQWU/documents/bDbonIoVar6PddcP_HzTya/download/bDbonIoVar6PddcP_HzTya)
  * click save
  * run with debug (MAJ + F9).

### 6.2 Configure wildfly server using the HotSwap mechanism : 
Sometimes, when you're making minor changes to your code, you want to immediately see how they will behave in a working application without shutting down the process. The HotSwap mechanism lets you reload classes changed during a debugging session without having to restart the entire application.

So instead of select the external source: 
* select artifact : 
![](https://www.assembla.com/spaces/cRAcZ4D1Cr4PP6acwqjQWU/documents/dkoRPAoVer6OoYaH8tHBnc/download/dkoRPAoVer6OoYaH8tHBnc)
* click save
* run with debug (MAJ + F9).
* to update click (MAJ + F10), to update without restarting the application 