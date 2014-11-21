package simpledb.systemtest;

import java.io.IOException;

import simpledb.*;
import static org.junit.Assert.*;

import org.junit.Test;

public class AbortEvictionTest extends SimpleDbTestBase {
    /**
     * Aborts a transaction and ensures that its effects were actually undone.
     * This requires dirty pages to <em>not</em> get flushed to disk.
     */
    @Test
    public void testDoNotEvictDirtyPages()
            throws IOException, DbException, TransactionAbortedException {
        // Allocate a file with ~10 pages of data
        HeapFile f = SystemTestUtil.createRandomHeapFile(2, 512 * 10, null, null);
        Database.resetBufferPool(2);

        // BEGIN TRANSACTION
        Transaction t = new Transaction();
        t.start();

        // Insert a new row
        TransactionTestUtil.insertRow(f, t);

        // The tuple must exist in the table
        boolean found = TransactionTestUtil.findMagicTuple(f, t);
        assertTrue(found);
        // ABORT
        t.transactionComplete(true);
        //System.out.println("hahaha");
        // A second transaction must not find the tuple
        t = new Transaction();
        t.start();
        //System.out.println("before find tuple");
        found = TransactionTestUtil.findMagicTuple(f, t);
        //System.out.println("found before: " + found);
        assertFalse(found);
        //System.out.println("found after: " + found);
        t.commit();
    }

    /**
     * Make test compatible with older version of ant.
     */
    public static junit.framework.Test suite() {
        return new junit.framework.JUnit4TestAdapter(AbortEvictionTest.class);
    }
}
