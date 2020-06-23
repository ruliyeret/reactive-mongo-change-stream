package reactiveApi;

import com.mongodb.client.MongoCollection;
import org.bson.conversions.Bson;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public interface IMongoReactive {

      <T, K, V> void addListener(MongoCollection<T> collection,
                                      ConcurrentHashMap<K, V> map,
                                      IKeyValueProducer<T, K, V> keyValueProducer);

      default <T> void removeListener(MongoCollection<T> collection){
           collection.watch().iterator().close();
      }

     <T, K, V> void addListener(MongoCollection<T> collection,
                                ConcurrentHashMap<K, V> map,
                                IKeyValueProducer<T, K, V> keyValueProducer,
                                List<Bson> list);

}
