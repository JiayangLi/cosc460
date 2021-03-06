package simpledb;

import java.io.IOException;

/**
 * The delete operator. Delete reads tuples from its child operator and removes
 * them from the table they belong to.
 */
public class Delete extends Operator {

    private static final long serialVersionUID = 1L;
    
    private TransactionId tid;
    private DbIterator dbi;
    private boolean fetchable;
    private TupleDesc td;

    /**
     * Constructor specifying the transaction that this delete belongs to as
     * well as the child to read from.
     *
     * @param t     The transaction this delete runs in
     * @param child The child operator from which to read tuples for deletion
     */
    public Delete(TransactionId t, DbIterator child) {
        this.tid = t;
        this.dbi = child;
        this.td = new TupleDesc(new Type[]{Type.INT_TYPE}, new String[]{"Counts"});
        this.fetchable = true;
    }

    public TupleDesc getTupleDesc() {
        return td;
    }

    public void open() throws DbException, TransactionAbortedException {
        super.open();
        dbi.open();
    }

    public void close() {
        super.close();
        dbi.close();
    }

    public void rewind() throws DbException, TransactionAbortedException {
        dbi.rewind();
        fetchable = true;
    }

    /**
     * Deletes tuples as they are read from the child operator. Deletes are
     * processed via the buffer pool (which can be accessed via the
     * Database.getBufferPool() method.
     *
     * @return A 1-field tuple containing the number of deleted records.
     * @see Database#getBufferPool
     * @see BufferPool#deleteTuple
     */
    protected Tuple fetchNext() throws TransactionAbortedException, DbException {
        if (!fetchable)
        	return null;
        
        int count = 0;
        
        while (dbi.hasNext()){
        	try{
        		Database.getBufferPool().deleteTuple(tid, dbi.next());
        		count++;
        	}
        	catch (IOException e){
        		e.printStackTrace();
        	}
        }
        
        fetchable = false;
        
        Tuple rv = new Tuple(td);
        rv.setField(0, new IntField(count));
        
        return rv;
    }

    @Override
    public DbIterator[] getChildren() {
        return new DbIterator[]{dbi};
    }

    @Override
    public void setChildren(DbIterator[] children) {
        if (children == null)
        	throw new NullPointerException();
        
        dbi = children[0];
    }

}
