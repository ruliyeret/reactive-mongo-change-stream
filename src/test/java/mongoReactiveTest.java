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
import org.awaitility.Awaitility;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import reactiveApi.IKeyValueProducer;
import reactiveApi.MongoReactive;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;
import static org.bson.codecs.pojo.Conventions.ANNOTATION_CONVENTION;
import static org.bson.codecs.pojo.Conventions.CLASS_AND_PROPERTY_CONVENTION;
import static org.junit.Assert.*;

public class mongoReactiveTest {
    final static String DB_NAME = "test";
    final static String PERSON_COLLECTION_NAME = "people";
    final static String JOB_COLLECTION_NAME = "jobs";
    static MongoClient mongoClient;
    static MongoDatabase database;
    static MongoReactive mongoReactive;

    @BeforeClass
    public static void setup(){
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

         mongoClient = MongoClients.create(clientSettings);
         database = mongoClient.getDatabase(DB_NAME);
         mongoReactive = new MongoReactive();
    }

    @Before
    public  void before(){
        database.drop();
    }

    public <T> int addListenerTest(MongoCollection<T> mongoCollection,
                                    IKeyValueProducer iKeyValueProducer, T entity){

        ConcurrentHashMap<String, T> entitiesMap = new ConcurrentHashMap<>();
        mongoReactive.addListener(mongoCollection, entitiesMap, iKeyValueProducer);
        mongoCollection.insertOne(entity);

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return entitiesMap.size();
    }

    @Test
    public void addMultiListener(){
        addSingleListener();
        MongoCollection<Job> jobCollection =
                database.getCollection(JOB_COLLECTION_NAME, Job.class);
        IKeyValueProducer jobEntry = new JobEntry();

        Job job =new Job("Developer", 20000);
        assertTrue(addListenerTest(jobCollection, jobEntry, job) == 1);
    }

    @Test
    public void removeListenerTest() throws InterruptedException {
        MongoCollection<Person> personCollection =
                database.getCollection(PERSON_COLLECTION_NAME, Person.class);
        IKeyValueProducer personEntry = new PersonEntry();
        Person p = new Person("testRemove", 23);


        ConcurrentHashMap<String, Person> entitiesMap = new ConcurrentHashMap<>();
        mongoReactive.addListener(personCollection, entitiesMap, personEntry);
        assertEquals(mongoReactive.getCursorMap().size(), 1);
        Thread.sleep(1000);
        personCollection.insertOne(p);
        Awaitility.await().untilAsserted(() ->
                assertEquals(1, entitiesMap.size()));

        mongoReactive.removeListener(personCollection);
        Awaitility.await().untilAsserted(() ->
                assertEquals(0, mongoReactive.getCursorMap().size()));
        entitiesMap.clear();

        personCollection.insertOne(p);
        Awaitility.await().pollDelay(1, TimeUnit.SECONDS).untilAsserted(() ->
                assertEquals(0, entitiesMap.size()));

    }

    @Test
    public void addExistingListener(){
        addSingleListener();
        addSingleListener();
    }

    @Test
    public void addSingleListener(){
        MongoCollection<Person> personCollection =
                database.getCollection(PERSON_COLLECTION_NAME, Person.class);
        IKeyValueProducer personEntry = new PersonEntry();

        Person p = new Person("testAdd", 23);
        int sizeMap = addListenerTest(personCollection, personEntry, p);
        assertEquals(1, sizeMap);
    }

//    @AfterClass
//    public static void close(){
//        mongoReactive.close();
//    }




}
