package reactiveApi;

public interface IKeyValueProducer<T, K, V> {

     K getKey(T t);

     V getValue(T t);
}
