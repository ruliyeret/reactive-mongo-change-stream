package demo.modules;

import org.bson.codecs.pojo.annotations.BsonProperty;


public class Person {
        @BsonProperty(value = "name")
        private String name;

        @BsonProperty(value = "age")
        private int age;


        public void setName(String name) {
            this.name = name;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public int getAge() {
            return age;
        }

        public Person(String name, int age){
            this.name  =name;
            this.age = age;
        }

        public Person(){

        }



}

