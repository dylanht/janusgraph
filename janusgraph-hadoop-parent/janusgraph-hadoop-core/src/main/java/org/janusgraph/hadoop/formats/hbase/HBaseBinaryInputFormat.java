package org.janusgraph.hadoop.formats.hbase;

import org.janusgraph.diskstorage.Entry;
import org.janusgraph.diskstorage.PermanentBackendException;
import org.janusgraph.diskstorage.StaticBuffer;
import org.janusgraph.diskstorage.hbase.HBaseStoreManager;
import org.janusgraph.diskstorage.keycolumnvalue.SliceQuery;
import org.janusgraph.graphdb.configuration.GraphDatabaseConfiguration;
import org.janusgraph.hadoop.config.JanusHadoopConfiguration;
import org.janusgraph.hadoop.formats.util.AbstractBinaryInputFormat;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.filter.Filter;
import org.apache.hadoop.hbase.mapreduce.TableInputFormat;
import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;
import org.apache.hadoop.hbase.mapreduce.TableRecordReader;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.JobContext;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;

public class HBaseBinaryInputFormat extends AbstractBinaryInputFormat {

    private static final Logger log = LoggerFactory.getLogger(HBaseBinaryInputFormat.class);

    private final TableInputFormat tableInputFormat = new TableInputFormat();
    private TableRecordReader tableReader;
    private byte[] inputCFBytes;
    private RecordReader<StaticBuffer, Iterable<Entry>> janusRecordReader;

    @Override
    public List<InputSplit> getSplits(final JobContext jobContext) throws IOException, InterruptedException {
        return this.tableInputFormat.getSplits(jobContext);
    }

    @Override
    public RecordReader<StaticBuffer, Iterable<Entry>> createRecordReader(final InputSplit inputSplit, final TaskAttemptContext taskAttemptContext) throws IOException, InterruptedException {
        tableReader =
                (TableRecordReader) tableInputFormat.createRecordReader(inputSplit, taskAttemptContext);
        janusRecordReader =
                new HBaseBinaryRecordReader(tableReader, inputCFBytes);
        return janusRecordReader;
    }

    @Override
    public void setConf(final Configuration config) {
        super.setConf(config);

        //config.set(TableInputFormat.SCAN_COLUMN_FAMILY, Backend.EDGESTORE_NAME);
        config.set(TableInputFormat.INPUT_TABLE, janusConf.get(HBaseStoreManager.HBASE_TABLE));
        //config.set(HConstants.ZOOKEEPER_QUORUM, config.get(TITAN_HADOOP_GRAPH_INPUT_TITAN_STORAGE_HOSTNAME));
        config.set(HConstants.ZOOKEEPER_QUORUM, janusConf.get(GraphDatabaseConfiguration.STORAGE_HOSTS)[0]);
//        if (basicConf.get(TITAN_HADOOP_GRAPH_INPUT_TITAN_STORAGE_PORT, null) != null)
        if (janusConf.has(GraphDatabaseConfiguration.STORAGE_PORT))
            config.set(HConstants.ZOOKEEPER_CLIENT_PORT, String.valueOf(janusConf.get(GraphDatabaseConfiguration.STORAGE_PORT)));
        config.set("autotype", "none");
        log.debug("hbase.security.authentication={}", config.get("hbase.security.authentication"));
        Scan scanner = new Scan();
        String cfName = mrConf.get(JanusHadoopConfiguration.COLUMN_FAMILY_NAME);
        // TODO the space-saving short name mapping leaks from HBaseStoreManager here
        if (janusConf.get(HBaseStoreManager.SHORT_CF_NAMES)) {
            try {
                cfName = HBaseStoreManager.shortenCfName(cfName);
            } catch (PermanentBackendException e) {
                throw new RuntimeException(e);
            }
        }
        scanner.addFamily(cfName.getBytes());
        inputCFBytes = Bytes.toBytes(cfName);

        //scanner.setFilter(getColumnFilter(janusSetup.inputSlice(this.vertexQuery))); // TODO
        //TODO (minor): should we set other options in http://hbase.apache.org/apidocs/org/apache/hadoop/hbase/client/Scan.html for optimization?
        Method converter;
        try {
            converter = TableMapReduceUtil.class.getDeclaredMethod("convertScanToString", Scan.class);
            converter.setAccessible(true);
            config.set(TableInputFormat.SCAN, (String) converter.invoke(null, scanner));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        this.tableInputFormat.setConf(config);
    }

    public TableRecordReader getTableReader() {
        return tableReader;
    }

    public byte[] getEdgeStoreFamily() {
        return inputCFBytes;
    }

    private Filter getColumnFilter(SliceQuery query) {
        return null;
        //TODO: return HBaseKeyColumnValueStore.getFilter(janusSetup.inputSlice(inputFilter));
    }

    @Override
    public Configuration getConf() {
        return tableInputFormat.getConf();
    }
}
