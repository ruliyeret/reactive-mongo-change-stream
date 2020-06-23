package reactiveApi;

import com.mongodb.client.ChangeStreamIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.changestream.ChangeStreamDocument;
import com.mongodb.client.model.changestream.FullDocument;
import com.mongodb.client.model.changestream.OperationType;
import org.bson.conversions.Bson;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;

import static java.util.Arrays.asList;

public class MongoReactive implements IMongoReactive, AutoCloseable {


    private ExecutorService executorService;
    private Map<String, MongoCursor<ChangeStreamDocument>> cursorMap;
    private Map<String, Runnable> runnableMap;


    public Map<String, MongoCursor<ChangeStreamDocument>> getCursorMap() {
        return cursorMap;
    }

    public MongoReactive() {
        executorService = Executors.newFixedThreadPool(10);
        cursorMap = new ConcurrentHashMap<>();
        runnableMap = new ConcurrentHashMap<>();
    }

    @Override
    public <T, K, V> void addListener(MongoCollection<T> collection,
                                      ConcurrentHashMap<K, V> map,
                                      IKeyValueProducer<T, K, V> keyValueProducer) {
        List<Bson> pipeline = asList(Aggregates.match(Filters.in("operationType",
                asList("insert", "update", "replace", "delete"))));

        this.addListener(collection, map, keyValueProducer, pipeline);
    }

    private <T, K, V> MongoCursor<ChangeStreamDocument> computeFunction(MongoCollection<T> collection,
                                                                        ConcurrentHashMap<K, V> map,
                                                                        IKeyValueProducer<T, K, V> keyValueProducer,
                                                                        List<Bson> list) {

        ChangeStreamIterable cursor =
                collection.watch(list).fullDocument(FullDocument.UPDATE_LOOKUP);
        MongoCursor<ChangeStreamDocument> iterator =
                cursor.maxAwaitTime(3, TimeUnit.MINUTES).iterator();

        String collectionName = collection.toString();
        Runnable nextHandler = runnableMap.computeIfAbsent(collectionName, k -> () -> {
            ChangeStreamDocument<T> doc = null;
            if(cursorMap.containsKey(collectionName)){
                doc = cursorMap.get(collectionName).tryNext();
            }
            if (doc != null) {
                this.handleOperationType(doc, map, keyValueProducer);
            }
            System.out.println("map " + collection.getDocumentClass().getName() +
                    " size is: " + map.size());

            if (this.runnableMap.containsKey(collectionName)) {
                executorService.execute(this.runnableMap.get(collectionName));
            }

        });

        this.executorService.execute(nextHandler);

        return iterator;


    }

    @Override
    public <T, K, V> void addListener(MongoCollection<T> collection,
                                      ConcurrentHashMap<K, V> map,
                                      IKeyValueProducer<T, K, V> keyValueProducer,
                                      List<Bson> list) {

        cursorMap.computeIfAbsent(collection.toString(), (k) ->
                this.computeFunction(collection, map, keyValueProducer, list));
    }

    @Override
    public <T> void removeListener(MongoCollection<T> collection) {

        Objects.requireNonNull(collection);
        String collectionModule = collection.toString();
        synchronized (cursorMap) {
            this.runnableMap.remove(collectionModule);
            MongoCursor<ChangeStreamDocument> iterator = cursorMap.remove(collectionModule);
            if (iterator != null) {
                iterator.close();
            }
        }
    }

    private <T, K, V> void handleOperationType(ChangeStreamDocument<T> document,
                                               ConcurrentHashMap<K, V> map,
                                               IKeyValueProducer<T, K, V> keyValueProducer) {

        T doc = document.getFullDocument();
        switch (document.getOperationType()) {
            case INSERT:
                map.computeIfAbsent(keyValueProducer.getKey(doc),
                        (k) -> keyValueProducer.getValue(doc));
                break;

            case DELETE:
                map.remove(keyValueProducer.getKey(doc));
                break;

            case UPDATE:
                map.computeIfPresent(keyValueProducer.getKey(doc), (key, value) ->
                        keyValueProducer.getValue(doc));
                break;

            case REPLACE:
                map.replace(keyValueProducer.getKey(doc),
                        keyValueProducer.getValue(doc));
                break;
            default:
                System.out.println(OperationType.INVALIDATE.getValue());
        }
    }

    @Override
    public void close() {
        this.cursorMap.forEach((key, value) -> value.close());
        this.cursorMap.clear();

        this.executorService.shutdown();
    }
}