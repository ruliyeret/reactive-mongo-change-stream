package demo.enrties;

import demo.modules.Person;
import reactiveApi.IKeyValueProducer;

public class PersonEntry implements IKeyValueProducer<Person, String, Person> {


    @Override
    public String getKey(Person person) {
        return person.getName();
    }

    @Override
    public Person getValue(Person person) {
        return person;
    }
}
