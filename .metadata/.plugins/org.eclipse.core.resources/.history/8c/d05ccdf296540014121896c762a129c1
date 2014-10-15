package simpledb;

import java.io.*;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;

/**
 * BufferPool manages the reading and writing of pages into memory from
 * disk. Access methods call into it to retrieve pages, and it fetches
 * pages from the appropriate location.
 * <p/>
 * The BufferPool is also responsible for locking;  when a transaction fetches
 * a page, BufferPool checks that the transaction has the appropriate
 * locks to read/write the page.
 *
 * @Threadsafe, all fields are final
 */
public class BufferPool {
	/*
	//private class to represent page and the most recent time it got accessed
	private static class TimedPage{
		private Page pg;
		private long time;
		
		public TimedPage(Page pg){
			this.pg = pg;
			this.time = System.currentTimeMillis();
		}
		
		public void updateTime(){
			this.time = System.currentTimeMillis();
		}
	}*/
	
    /**
     * Bytes per page, including header.
     */
    public static final int PAGE_SIZE = 4096;

    private static int pageSize = PAGE_SIZE;

    /**
     * Default number of pages passed to the constructor. This is used by
     * other classes. BufferPool should use the numPages argument to the
     * constructor instead.
     */
    public static final int DEFAULT_PAGES = 50;
    
    private HashMap<PageId, Page> cache;	//use a HashMap as cache
    private HashMap<PageId, Long> times;	//use a HashMap to keep track of access times	
    private int numPages;	//cache's size

    /**
     * Creates a BufferPool that caches up to numPages pages.
     *
     * @param numPages maximum number of pages in this buffer pool.
     */
    public BufferPool(int numPages) {
    	cache = new HashMap<PageId, Page>();
    	times = new HashMap<PageId, Long>();
    	this.numPages = numPages;
    }

    public static int getPageSize() {
        return pageSize;
    }

    // THIS FUNCTION SHOULD ONLY BE USED FOR TESTING!!
    public static void setPageSize(int pageSize) {
        BufferPool.pageSize = pageSize;
    }
    
    /**
     * Update the access time for a given page
     * @param pid	the ID of the page whose acces time needs updating
     */
    private void updateTime(PageId pid){
    	if (!times.containsKey(pid))
    		throw new NoSuchElementException("page not in cache");
    	
    	times.put(pid, System.currentTimeMillis());
    } 

    /**
     * Retrieve the specified page with the associated permissions.
     * Will acquire a lock and may block if that lock is held by another
     * transaction.
     * <p/>
     * The retrieved page should be looked up in the buffer pool.  If it
     * is present, it should be returned.  If it is not present, it should
     * be added to the buffer pool and returned.  If there is insufficient
     * space in the buffer pool, an page should be evicted and the new page
     * should be added in its place.
     *
     * @param tid  the ID of the transaction requesting the page
     * @param pid  the ID of the requested page
     * @param perm the requested permissions on the page
     */
    public Page getPage(TransactionId tid, PageId pid, Permissions perm)
            throws TransactionAbortedException, DbException {
    	// if the requested page is already cached
    	if (cache.containsKey(pid)){
    		updateTime(pid);
    		return cache.get(pid);
    	}
    	
    	// if the cache is full, evictPage
    	while (cache.size() >= numPages){
    		evictPage();
    	}
    	
    	// read the page, and put it in the cache
    	int tableId = pid.getTableId();
    	Page toReturn = Database.getCatalog().getDatabaseFile(tableId).readPage(pid);
    	cache.put(pid, toReturn);
    	times.put(pid, System.currentTimeMillis());
    	return toReturn;
    }
    

    /**
     * Releases the lock on a page.
     * Calling this is very risky, and may result in wrong behavior. Think hard
     * about who needs to call this and why, and why they can run the risk of
     * calling it.
     *
     * @param tid the ID of the transaction requesting the unlock
     * @param pid the ID of the page to unlock
     */
    public void releasePage(TransactionId tid, PageId pid) {
        // some code goes here
        // not necessary for lab1|lab2|lab3|lab4                                                         // cosc460
    }

    /**
     * Release all locks associated with a given transaction.
     *
     * @param tid the ID of the transaction requesting the unlock
     */
    public void transactionComplete(TransactionId tid) throws IOException {
        // some code goes here
        // not necessary for lab1|lab2|lab3|lab4                                                         // cosc460
    }

    /**
     * Return true if the specified transaction has a lock on the specified page
     */
    public boolean holdsLock(TransactionId tid, PageId p) {
        // some code goes here
        // not necessary for lab1|lab2|lab3|lab4                                                         // cosc460
        return false;
    }

    /**
     * Commit or abort a given transaction; release all locks associated to
     * the transaction.
     *
     * @param tid    the ID of the transaction requesting the unlock
     * @param commit a flag indicating whether we should commit or abort
     */
    public void transactionComplete(TransactionId tid, boolean commit)
            throws IOException {
        // some code goes here
        // not necessary for lab1|lab2|lab3|lab4                                                         // cosc460
    }

    /**
     * Add a tuple to the specified table on behalf of transaction tid.  Will
     * acquire a write lock on the page the tuple is added to and any other
     * pages that are updated (Lock acquisition is not needed until lab5).                                  // cosc460
     * May block if the lock(s) cannot be acquired.
     * <p/>
     * Marks any pages that were dirtied by the operation as dirty by calling
     * their markDirty bit, and updates cached versions of any pages that have
     * been dirtied so that future requests see up-to-date pages.
     *
     * @param tid     the transaction adding the tuple
     * @param tableId the table to add the tuple to
     * @param t       the tuple to add
     */
    public void insertTuple(TransactionId tid, int tableId, Tuple t)
            throws DbException, IOException, TransactionAbortedException {
        if (tid == null || t == null)
        	throw new NullPointerException();
        
        DbFile table = Database.getCatalog().getDatabaseFile(tableId);
                
        //there should only be one page in the ArrayList
        //HeapFile.insertTuple calls getPage, which updates the access time
        Page pg = table.insertTuple(tid, t).get(0);
        
        //markDirty that page
        pg.markDirty(true, tid);
        
        //update cache
        cache.put(pg.getId(), pg);
    }

    /**
     * Remove the specified tuple from the buffer pool.
     * Will acquire a write lock on the page the tuple is removed from and any
     * other pages that are updated. May block if the lock(s) cannot be acquired.
     * <p/>
     * Marks any pages that were dirtied by the operation as dirty by calling
     * their markDirty bit, and updates cached versions of any pages that have
     * been dirtied so that future requests see up-to-date pages.
     *
     * @param tid the transaction deleting the tuple.
     * @param t   the tuple to delete
     */
    public void deleteTuple(TransactionId tid, Tuple t)
            throws DbException, IOException, TransactionAbortedException {
    	if (tid == null || t == null)
    		throw new NullPointerException();
    	
    	//find the page where t is located
    	PageId pid = t.getRecordId().getPageId();
    	DbFile table =  Database.getCatalog().getDatabaseFile(pid.getTableId());
    	
    	//there should only be one page in the ArrayList
    	//HeapFile.deleteTuple calls getPage, which updates the access time
    	Page pg = table.deleteTuple(tid, t).get(0);
    	
    	//markDirty that page
    	pg.markDirty(true, tid);
    	
    	//update the cache
    	cache.put(pid, pg);
    }

    /**
     * Flush all dirty pages to disk.
     * NB: Be careful using this routine -- it writes dirty data to disk so will
     * break simpledb if running in NO STEAL mode.
     */
    public synchronized void flushAllPages() throws IOException {
    	//pass every pid into flushPage
    	//check dirty done in flushPage
    	for (PageId pid: cache.keySet()){
    		flushPage(pid);
    	}
    }

    /**
     * Remove the specific page id from the buffer pool.
     * Needed by the recovery manager to ensure that the
     * buffer pool doesn't keep a rolled back page in its
     * cache.
     */
    public synchronized void discardPage(PageId pid) {
        // some code goes here
        // only necessary for lab6                                                                            // cosc460
    }

    /**
     * Flushes a certain page to disk
     *
     * @param pid an ID indicating the page to flush
     */
    private synchronized void flushPage(PageId pid) throws IOException {
        if (pid == null)
        	throw new NullPointerException();
        if (!cache.containsKey(pid))
        	throw new NoSuchElementException("page to flush not in cache");
        
        Page pageToFlush = cache.get(pid);
        
        //check if dirty
        if (pageToFlush.isDirty() != null){
            DbFile table = Database.getCatalog().getDatabaseFile(pid.getTableId());
            
            //mark the page as not dirty
            pageToFlush.markDirty(false, null);
            
            //flush the page to disk
            table.writePage(pageToFlush);
        }
    }

    /**
     * Write all pages of the specified transaction to disk.
     */
    public synchronized void flushPages(TransactionId tid) throws IOException {
        // some code goes here
        // not necessary for lab1|lab2|lab3|lab4                                                         // cosc460
    }

    /**
     * Discards a page from the buffer pool.
     * Flushes the page to disk to ensure dirty pages are updated on disk.
     */
    private synchronized void evictPage() throws DbException {
        //Find the least recently used page
    	long LRUtime = 0;
    	PageId LRUid = null;
    	
    	for (PageId pid: times.keySet()){
    		if (times.get(pid) > LRUtime){
    			LRUid = pid;
    		}
    	}
    	
    	//flush the page
    	try{
    		flushPage(LRUid);
    	}
    	catch (IOException e){
    		e.printStackTrace();
    	}
    	 	
    	//remove from cache
    	cache.remove(LRUid);
    	times.remove(LRUid);
    }

}
