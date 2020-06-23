package demo.modules;

import org.bson.codecs.pojo.annotations.BsonProperty;

public class Job {

    @BsonProperty(value = "name")
    private String name;

    public Job(String name, int salary) {
        this.name = name;
        this.salary = salary;
    }
    public Job(){

    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSalary(int salary) {
        this.salary = salary;
    }

    public String getName() {
        return name;
    }

    public int getSalary() {
        return salary;
    }

    @BsonProperty(value = "salary")
    private int salary;




}
