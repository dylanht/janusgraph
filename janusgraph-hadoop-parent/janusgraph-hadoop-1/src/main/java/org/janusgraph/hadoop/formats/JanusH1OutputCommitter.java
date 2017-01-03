package org.janusgraph.hadoop.formats;

import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.OutputCommitter;
import org.apache.hadoop.mapreduce.TaskAttemptContext;

import java.io.IOException;

public class JanusH1OutputCommitter extends OutputCommitter {
    private final JanusH1OutputFormat tof;

    public JanusH1OutputCommitter(JanusH1OutputFormat tof) {
        this.tof = tof;
    }

    @Override
    public void setupJob(JobContext jobContext) throws IOException {

    }

    @Override
    public void setupTask(TaskAttemptContext taskContext) throws IOException {

    }

    @Override
    public boolean needsTaskCommit(TaskAttemptContext taskContext) throws IOException {
        return tof.hasModifications(taskContext.getTaskAttemptID());
    }

    @Override
    public void commitTask(TaskAttemptContext taskContext) throws IOException {
        tof.commit(taskContext.getTaskAttemptID());
    }

    @Override
    public void abortTask(TaskAttemptContext taskContext) throws IOException {
        tof.abort(taskContext.getTaskAttemptID());
    }
}