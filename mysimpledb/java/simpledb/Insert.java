package simpledb;

import java.io.IOException;

/**
 * Inserts tuples read from the child operator into the tableid specified in the
 * constructor
 */
public class Insert extends Operator {

    private static final long serialVersionUID = 1L;
    
    private TransactionId tid;
    private DbIterator dbi;
    private int tableid;
    private boolean fetchable;
    private TupleDesc td;

    /**
     * Constructor.
     *
     * @param t       The transaction running the insert.
     * @param child   The child operator from which to read tuples to be inserted.
     * @param tableid The table in which to insert tuples.
     * @throws DbException if TupleDesc of child differs from table into which we are to
     *                     insert.
     */
    public Insert(TransactionId t, DbIterator child, int tableid)
            throws DbException {
        this.tid = t;
        this.dbi = child;
        this.tableid = tableid;
        this.fetchable = true;
        this.td = new TupleDesc(new Type[]{Type.INT_TYPE}, new String[]{"Counts"});
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
     * Inserts tuples read from child into the tableid specified by the
     * constructor. It returns a one field tuple containing the number of
     * inserted records. Inserts should be passed through BufferPool. An
     * instances of BufferPool is available via Database.getBufferPool(). Note
     * that insert DOES NOT need check to see if a particular tuple is a
     * duplicate before inserting it.
     *
     * @return A 1-field tuple containing the number of inserted records, or
     * null if called more than once.
     * @see Database#getBufferPool
     * @see BufferPool#insertTuple
     */
    protected Tuple fetchNext() throws TransactionAbortedException, DbException {
        if (!fetchable)
        	return null;
        
        int count = 0;
        
        while (dbi.hasNext()){
        	try{
        		Database.getBufferPool().insertTuple(tid, tableid, dbi.next());
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
