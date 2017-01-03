package org.janusgraph.graphdb;

import org.janusgraph.CassandraStorageSetup;
import org.janusgraph.core.JanusFactory;
import org.janusgraph.diskstorage.cassandra.AbstractCassandraStoreManager;
import org.janusgraph.diskstorage.configuration.ConfigElement;
import org.janusgraph.diskstorage.configuration.WriteConfiguration;
import org.janusgraph.graphdb.database.StandardJanusGraph;
import org.janusgraph.graphdb.transaction.StandardJanusTx;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.janusgraph.diskstorage.cassandra.AbstractCassandraStoreManager.*;

/**
 * @author Joshua Shinavier (http://fortytwo.net)
 */
public abstract class CassandraGraphTest extends JanusGraphTest {

    @BeforeClass
    public static void startCassandra() {
        CassandraStorageSetup.startCleanEmbedded();
    }

    @Override
    protected boolean isLockingOptimistic() {
        return true;
    }

    @Test
    public void testHasTTL() throws Exception {
        assertTrue(features.hasCellTTL());
    }

    @Test
    public void testGraphConfigUsedByThreadBoundTx() {
        close();
        WriteConfiguration wc = getConfiguration();
        wc.set(ConfigElement.getPath(CASSANDRA_READ_CONSISTENCY), "ALL");
        wc.set(ConfigElement.getPath(CASSANDRA_WRITE_CONSISTENCY), "LOCAL_QUORUM");

        graph = (StandardJanusGraph) JanusFactory.open(wc);

        StandardJanusTx tx = (StandardJanusTx)graph.getCurrentThreadTx();
        assertEquals("ALL",
                tx.getTxHandle().getBaseTransactionConfig().getCustomOptions()
                        .get(AbstractCassandraStoreManager.CASSANDRA_READ_CONSISTENCY));
        assertEquals("LOCAL_QUORUM",
                tx.getTxHandle().getBaseTransactionConfig().getCustomOptions()
                        .get(AbstractCassandraStoreManager.CASSANDRA_WRITE_CONSISTENCY));
    }

    @Test
    public void testGraphConfigUsedByTx() {
        close();
        WriteConfiguration wc = getConfiguration();
        wc.set(ConfigElement.getPath(CASSANDRA_READ_CONSISTENCY), "TWO");
        wc.set(ConfigElement.getPath(CASSANDRA_WRITE_CONSISTENCY), "THREE");

        graph = (StandardJanusGraph) JanusFactory.open(wc);

        StandardJanusTx tx = (StandardJanusTx)graph.newTransaction();
        assertEquals("TWO",
                tx.getTxHandle().getBaseTransactionConfig().getCustomOptions()
                        .get(AbstractCassandraStoreManager.CASSANDRA_READ_CONSISTENCY));
        assertEquals("THREE",
                tx.getTxHandle().getBaseTransactionConfig().getCustomOptions()
                        .get(AbstractCassandraStoreManager.CASSANDRA_WRITE_CONSISTENCY));
        tx.rollback();
    }

    @Test
    public void testCustomConfigUsedByTx() {
        close();
        WriteConfiguration wc = getConfiguration();
        wc.set(ConfigElement.getPath(CASSANDRA_READ_CONSISTENCY), "ALL");
        wc.set(ConfigElement.getPath(CASSANDRA_WRITE_CONSISTENCY), "ALL");

        graph = (StandardJanusGraph) JanusFactory.open(wc);

        StandardJanusTx tx = (StandardJanusTx)graph.buildTransaction()
                .customOption(ConfigElement.getPath(CASSANDRA_READ_CONSISTENCY), "ONE")
                .customOption(ConfigElement.getPath(CASSANDRA_WRITE_CONSISTENCY), "TWO").start();

        assertEquals("ONE",
                tx.getTxHandle().getBaseTransactionConfig().getCustomOptions()
                        .get(AbstractCassandraStoreManager.CASSANDRA_READ_CONSISTENCY));
        assertEquals("TWO",
                tx.getTxHandle().getBaseTransactionConfig().getCustomOptions()
                        .get(AbstractCassandraStoreManager.CASSANDRA_WRITE_CONSISTENCY));
        tx.rollback();
    }
}
