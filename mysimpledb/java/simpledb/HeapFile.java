package simpledb;

import java.io.*;
import java.util.*;

/**
 * HeapFile is an implementation of a DbFile that stores a collection of tuples
 * in no particular order. Tuples are stored on pages, each of which is a fixed
 * size, and the file is simply a collection of those pages. HeapFile works
 * closely with HeapPage. The format of HeapPages is described in the HeapPage
 * constructor.
 *
 * @author Sam Madden
 * @see simpledb.HeapPage#HeapPage
 */
public class HeapFile implements DbFile {
	
	private File f;
	private TupleDesc td;

    /**
     * Constructs a heap file backed by the specified file.
     *
     * @param f the file that stores the on-disk backing store for this heap
     *          file.
     */
    public HeapFile(File f, TupleDesc td) {
    	this.f = f;
    	this.td = td;
    }

    /**
     * Returns the File backing this HeapFile on disk.
     *
     * @return the File backing this HeapFile on disk.
     */
    public File getFile() {
        return this.f;
    }

    /**
     * Returns an ID uniquely identifying this HeapFile. Implementation note:
     * you will need to generate this tableid somewhere ensure that each
     * HeapFile has a "unique id," and that you always return the same value for
     * a particular HeapFile. We suggest hashing the absolute file name of the
     * file underlying the heapfile, i.e. f.getAbsoluteFile().hashCode().
     *
     * @return an ID uniquely identifying this HeapFile.
     */
    public int getId() {
        return f.getAbsoluteFile().hashCode();
    }

    /**
     * Returns the TupleDesc of the table stored in this DbFile.
     *
     * @return TupleDesc of this DbFile.
     */
    public TupleDesc getTupleDesc() {
    	return this.td;
    }
    
    // see DbFile.java for javadocs
    public Page readPage(PageId pid) {
        if (pid.getTableId() != getId())	//if different pages
        	throw new IllegalArgumentException("The given page is not in this table");
        
        try{
        	InputStream input = new BufferedInputStream(new FileInputStream(f));
        	
        	byte[] data = new byte[BufferPool.getPageSize()];
        	
        	int offset = pid.pageNumber() * BufferPool.getPageSize();
        	
        	input.skip(offset);
        	
        	input.read(data);
        	input.close();
        	
        	return new HeapPage((HeapPageId) pid, data);
        	}
        catch (FileNotFoundException e){
        	System.err.println("File not found");
        }
        catch (IOException e1){
        	e1.printStackTrace();
        }
        
        //should not get here
        return null;
    }

    // see DbFile.java for javadocs
    public void writePage(Page page) throws IOException {
    	if (page.getId().getTableId() != getId())
    		throw new NoSuchElementException("page not in this table");
    	
        RandomAccessFile file = new RandomAccessFile(f, "rw");
        
        file.seek(page.getId().pageNumber() * BufferPool.getPageSize());
        file.write(page.getPageData());
        file.close();
    }

    /**
     * Returns the number of pages in this HeapFile.
     */
    public int numPages() {
        return (int) Math.ceil(f.length() / BufferPool.getPageSize());
    }

    // see DbFile.java for javadocs
    public ArrayList<Page> insertTuple(TransactionId tid, Tuple t)
            throws DbException, IOException, TransactionAbortedException {
        if (t == null)
        	throw new NullPointerException();
        
        //if (!t.getTupleDesc().equals(t))
     	 //  throw new DbException("Tuple is not compatible with this table");
        
        ArrayList<Page> rv = new ArrayList<Page>();
        
        int pageNo = 0;
        //go through every existing pages to find an open slot
        while (pageNo < numPages()){
        	HeapPageId pid = new HeapPageId(getId(), pageNo);
        	HeapPage pg = (HeapPage) Database.getBufferPool().getPage(tid, pid, Permissions.READ_WRITE);
        	
        	if (pg.getNumEmptySlots() != 0){
        		pg.insertTuple(t);
        		rv.add(pg);
        		return rv;
        	}
        	pageNo++;
        }
        
        //no empty slot on any page, make a new page
        HeapPageId newPageId = new HeapPageId(getId(), pageNo);
        HeapPage newPage = new HeapPage(newPageId, HeapPage.createEmptyPageData());
        newPage.insertTuple(t);
        rv.add(newPage); 
        
        //write to file
        OutputStream output = new BufferedOutputStream(new FileOutputStream(f, true));	
        output.write(newPage.getPageData());
        output.close();
        
        return rv;
    }

    // see DbFile.java for javadocs
    public ArrayList<Page> deleteTuple(TransactionId tid, Tuple t) throws DbException,
            TransactionAbortedException {
       if (t == null)
    	   throw new NullPointerException();
       
       PageId pid = t.getRecordId().getPageId();
       int tableId = pid.getTableId();
       
       if (tableId != getId())
    	   throw new NoSuchElementException("Tuple is not in this table");
       
       ArrayList<Page> rv = new ArrayList<Page>();
       
       HeapPage pg = (HeapPage) Database.getBufferPool().getPage(tid, pid, Permissions.READ_WRITE);
       
       pg.deleteTuple(t);
       
       rv.add(pg);
       
       return rv;
    }

    // see DbFile.java for javadocs
    public DbFileIterator iterator(TransactionId tid) {
        return new fileIterator(tid);
    }
    
    //private inner helper class for iterator
    private class fileIterator implements DbFileIterator{
    	
    	private int currPageNo;	//the page number of the page currently being iterated over
    	private Iterator<Tuple> tuplesCurrPage;	//the tuple iterator of the current page
    	private TransactionId tid;
    	
    	//constructor
    	public fileIterator(TransactionId tid){
    		this.currPageNo = 0;
    		this.tuplesCurrPage = null;
    		this.tid = tid;
    	}
    	
    	public void open() throws DbException, TransactionAbortedException{
    		if (currPageNo >= numPages())
    			throw new DbException("No page to open an iterator");
    		
    		//Able to open
    		HeapPage pg = (HeapPage) Database.getBufferPool().getPage(tid, new HeapPageId(getId(), currPageNo), Permissions.READ_ONLY);
    		tuplesCurrPage = pg.iterator();
    	}
    	
    	public boolean hasNext() throws DbException, TransactionAbortedException{
    		//not opened
    		if (tuplesCurrPage == null)	
    			return false;
    		
    		//the current page has next tuple
    		if (tuplesCurrPage.hasNext())
    			return true;
    		
    		//No tuple left on this page, move to the next page
    		currPageNo++;
    		
    		//keep checking through every page to find a tuple
    		while (currPageNo < numPages()){    		
	    		HeapPage pg = (HeapPage) Database.getBufferPool().getPage(tid, new HeapPageId(getId(), currPageNo), Permissions.READ_ONLY);
	    		tuplesCurrPage = pg.iterator();
	    		if (tuplesCurrPage.hasNext())
	    			return true;
	    		else
	    			currPageNo++;
    		}
    		
    		//finished checking every page but failed to find a tuple
    		return false;
    	}
    	
    	public Tuple next() throws DbException, TransactionAbortedException, NoSuchElementException{
    		if (!hasNext())	//calling hasNext() ensures tuplesCurrPage is the most current page with at least one tuple
    			throw new NoSuchElementException();
    		
    		return tuplesCurrPage.next();
    	}
    	
    	public void rewind() throws DbException, TransactionAbortedException{
    		close();
    		open();
    	}
    	
    	public void close(){
    		currPageNo = 0;
    		tuplesCurrPage = null;
    		tid = null;
    	}
    }

}

