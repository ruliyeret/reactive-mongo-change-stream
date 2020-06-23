package demo.enrties;

import demo.modules.Job;
import reactiveApi.IKeyValueProducer;


public  class JobEntry implements IKeyValueProducer<Job, String, Job> {

    @Override
    public String getKey(Job job) {
        return job.getName();
    }

    @Override
    public Job getValue(Job job) {
        return job;
    }
}
