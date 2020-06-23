package demo;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import demo.enrties.JobEntry;
import demo.enrties.PersonEntry;
import demo.modules.Job;
import demo.modules.Person;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import reactiveApi.IKeyValueProducer;
import reactiveApi.MongoReactive;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;
import static org.bson.codecs.pojo.Conventions.ANNOTATION_CONVENTION;
import static org.bson.codecs.pojo.Conventions.CLASS_AND_PROPERTY_CONVENTION;


public class Demo {


    public static void main(String[] args) throws InterruptedException {

        final String DB_NAME = "test";
        final String PERSON_COLLECTION_NAME = "people";
        final String JOB_COLLECTION_NAME = "jobs";
        ConnectionString connectionString = new ConnectionString("mongodb://localhost:27017/?replicaSet=rs0");
        CodecRegistry pojoCodecRegistry =
                fromProviders(PojoCodecProvider.builder().conventions(Arrays.asList(CLASS_AND_PROPERTY_CONVENTION,
                        ANNOTATION_CONVENTION)).register(Person.class).automatic(true).build());
        CodecRegistry codecRegistry =
                fromRegistries(MongoClientSettings.getDefaultCodecRegistry(), pojoCodecRegistry);
        MongoClientSettings clientSettings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .codecRegistry(codecRegistry)
                .build();


        MongoClient mongoClient = MongoClients.create(clientSettings);
        MongoDatabase database = mongoClient.getDatabase(DB_NAME);
        MongoCollection<Person> personsCollection = database.getCollection(PERSON_COLLECTION_NAME, Person.class);
        MongoCollection<Job> jobCollection = database.getCollection(JOB_COLLECTION_NAME, Job.class);

        personsCollection.drop();
        Person p = new Person("ruli", 23);
        IKeyValueProducer personEntry = new PersonEntry();
        ConcurrentHashMap<String, Person> mapPerson = new ConcurrentHashMap<>();

        jobCollection.drop();
        Job job = new Job("computer developer", 30000);
        IKeyValueProducer jobEntry = new JobEntry();
        ConcurrentHashMap<String, Job> mapJob = new ConcurrentHashMap<>();

        MongoReactive mongoReactive = new MongoReactive();

        mongoReactive.addListener(personsCollection, mapPerson, personEntry);
        Thread.sleep(1000);
        System.out.println("Person map size: " + mapPerson.size());
        System.out.println("Insert new Person to db");
        personsCollection.insertOne(p);

        System.out.println("Person map size: " + mapPerson.size());


        mongoReactive.addListener(jobCollection, mapJob, jobEntry);
        Thread.sleep(1000);
        System.out.println("Job map size: " + mapJob.size());
        System.out.println("Insert new Jon to db");
        jobCollection.insertOne(job);
        System.out.println("Job map size: " + mapJob.size());

        System.out.println("hgj");


    }
}
