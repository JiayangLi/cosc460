package simpledb;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashSet;
import java.util.Set;

/**
 * @author mhay
 */
class LogFileRecovery {

    private final RandomAccessFile readOnlyLog;

    /**
     * Helper class for LogFile during rollback and recovery.
     * This class given a read only view of the actual log file.
     *
     * If this class wants to modify the log, it should do something
     * like this:  Database.getLogFile().logAbort(tid);
     *
     * @param readOnlyLog a read only copy of the log file
     */
    public LogFileRecovery(RandomAccessFile readOnlyLog) {
        this.readOnlyLog = readOnlyLog;
    }

    /**
     * Print out a human readable representation of the log
     */
    public void print() throws IOException {
        // since we don't know when print will be called, we can save our current location in the file
        // and then jump back to it after printing
        Long currentOffset = readOnlyLog.getFilePointer();

        readOnlyLog.seek(0);
        long lastCheckpoint = readOnlyLog.readLong(); // ignore this
        System.out.println("BEGIN LOG FILE");
        while (readOnlyLog.getFilePointer() < readOnlyLog.length()) {
            int type = readOnlyLog.readInt();
            long tid = readOnlyLog.readLong();
            switch (type) {
                case LogType.BEGIN_RECORD:
                    System.out.println("<T_" + tid + " BEGIN>");
                    break;
                case LogType.COMMIT_RECORD:
                    System.out.println("<T_" + tid + " COMMIT>");
                    break;
                case LogType.ABORT_RECORD:
                    System.out.println("<T_" + tid + " ABORT>");
                    break;
                case LogType.UPDATE_RECORD:
                    Page beforeImg = LogFile.readPageData(readOnlyLog);
                    Page afterImg = LogFile.readPageData(readOnlyLog);  // after image
                    System.out.println("<T_" + tid + " UPDATE pid=" + beforeImg.getId() +">");
                    break;
                case LogType.CLR_RECORD:
                    afterImg = LogFile.readPageData(readOnlyLog);  // after image
                    System.out.println("<T_" + tid + " CLR pid=" + afterImg.getId() +">");
                    break;
                case LogType.CHECKPOINT_RECORD:
                    int count = readOnlyLog.readInt();
                    Set<Long> tids = new HashSet<Long>();
                    for (int i = 0; i < count; i++) {
                        long nextTid = readOnlyLog.readLong();
                        tids.add(nextTid);
                    }
                    System.out.println("<T_" + tid + " CHECKPOINT " + tids + ">");
                    break;
                default:
                    throw new RuntimeException("Unexpected type!  Type = " + type);
            }
            long startOfRecord = readOnlyLog.readLong();   // ignored, only useful when going backwards thru log
        }
        System.out.println("END LOG FILE");

        // return the file pointer to its original position
        readOnlyLog.seek(currentOffset);

    }

    /**
     * Rollback the specified transaction, setting the state of any
     * of pages it updated to their pre-updated state.  To preserve
     * transaction semantics, this should not be called on
     * transactions that have already committed (though this may not
     * be enforced by this method.)
     *
     * This is called from LogFile.recover after both the LogFile and
     * the BufferPool are locked.
     *
     * @param tidToRollback The transaction to rollback
     * @throws java.io.IOException if tidToRollback has already committed
     */
    public void rollback(TransactionId tidToRollback) throws IOException {
    	System.out.println("hahaha rollback " + tidToRollback);
        readOnlyLog.seek(readOnlyLog.length()); // undoing so move to end of logfile
        
//        synchronized (Database.getBufferPool()) {
//        	synchronized (this){
        		long current = readOnlyLog.getFilePointer();
        		while (current >LogFile.LONG_SIZE) {
	        		readOnlyLog.seek(current - LogFile.LONG_SIZE);
	        		long recordOffset = readOnlyLog.readLong();
	        		readOnlyLog.seek(recordOffset);
	        		
	        		int type = readOnlyLog.readInt();
	                long tid = readOnlyLog.readLong();
	                
	                if (tid == tidToRollback.getId()){
	                	switch (type){
		                	case LogType.BEGIN_RECORD:
		                		Database.getLogFile().logAbort(tid);
			                	return;
		                    case LogType.COMMIT_RECORD:
		                    	throw new IOException("Cannot abort a committed transaction!");
		                    case LogType.ABORT_RECORD:
		                    	throw new IOException("Cannot commit a committed transaction!");
		                    case LogType.UPDATE_RECORD:
		                    	Page beforeImg = LogFile.readPageData(readOnlyLog);
			                    Page afterImg = LogFile.readPageData(readOnlyLog);		               
			                    PageId pid = beforeImg.getId();
			                    Database.getBufferPool().discardPage(pid);
			                    Database.getCatalog().getDatabaseFile(pid.getTableId()).writePage(beforeImg);
			                    Database.getLogFile().logCLR(tid, beforeImg);
		                        break;
		                    case LogType.CLR_RECORD:
		                        break;
		                    case LogType.CHECKPOINT_RECORD:
		                    	break;
		                    default: 
		                    	throw new RuntimeException("Unexpected type!  Type = " + type); 
	                	}
	                }
	                current = recordOffset;
        		}
        		readOnlyLog.seek(readOnlyLog.length()); // undoing so move to end of logfile
//        	}
//        }
        
    }

    /**
     * Recover the database system by ensuring that the updates of
     * committed transactions are installed and that the
     * updates of uncommitted transactions are not installed.
     *
     * This is called from LogFile.recover after both the LogFile and
     * the BufferPool are locked.
     */
    public void recover() throws IOException {
    	synchronized (Database.getBufferPool()) {
    		synchronized (this) {
    			HashSet<Long> losers = new HashSet<Long>();
    			
    			readOnlyLog.seek(0);	//go back to beginning
    			long lastCheckpoint = readOnlyLog.readLong();
    			
    			if (lastCheckpoint != -1){	//go to the last checkpoint if it exists
    				readOnlyLog.seek(lastCheckpoint);
    				readOnlyLog.skipBytes(LogFile.INT_SIZE + LogFile.LONG_SIZE); //skip type and tid
    				int count = readOnlyLog.readInt();
    				for (int i = 0; i < count; i++){
    					losers.add(readOnlyLog.readLong());
    				}
    				readOnlyLog.skipBytes(LogFile.LONG_SIZE); //skip the starting offset of this checkpoint
    			}
    			
    			redo(losers);
    			undo(losers);
    		}
    	}
    }
    
    /**
     * Perform the redo phase.
     * 
     * Only call this function in recover() because it assumes that the filepointer of readOnlyLog is
     * at appropriate position.
     */
    private void redo(HashSet<Long> losers) throws IOException {
    	while (readOnlyLog.getFilePointer() < readOnlyLog.length()){
			int type = readOnlyLog.readInt();
            long tid = readOnlyLog.readLong();
            switch (type){
            	case LogType.BEGIN_RECORD:
            		if (losers.contains(tid))
            			throw new IOException("already begun");
            		losers.add(tid);
            		break;
            	case LogType.COMMIT_RECORD:
            		if (!losers.contains(tid))
            			throw new IOException("can't commit, already committed or aborted");
            		losers.remove(tid);
            		break;
            	case LogType.ABORT_RECORD:
            		if (!losers.contains(tid))
            			throw new IOException("can't abort, already committed or aborted");
            		losers.remove(tid);
            		break;
            	case LogType.UPDATE_RECORD:
            		if (!losers.contains(tid))
            			throw new IOException("can't update, already committed or aborted");
            		Page beforeImg = LogFile.readPageData(readOnlyLog);
                    Page afterImg = LogFile.readPageData(readOnlyLog);  // after image
                    PageId pid = beforeImg.getId();
                    Database.getBufferPool().discardPage(pid);
                    Database.getCatalog().getDatabaseFile(pid.getTableId()).writePage(afterImg);
            		break;
            	case LogType.CLR_RECORD:
            		if (!losers.contains(tid))
            			throw new IOException("can't redo CLR, already committed or aborted");
            		afterImg = LogFile.readPageData(readOnlyLog);  // after image
            		pid = afterImg.getId();
            		Database.getCatalog().getDatabaseFile(pid.getTableId()).writePage(afterImg);
            		break;
            	case LogType.CHECKPOINT_RECORD:
            		throw new RuntimeException("Should not encounter any checkpoint!!");
            	default:
            		throw new RuntimeException("Unexpected type!  Type = " + type);
            }
            long startOfRecord = readOnlyLog.readLong();
		}
    }
    
    /**
     * Perform the undo phase
     * 
     * @param losers 
     */
    private void undo(HashSet<Long> losers) throws IOException {
    	 readOnlyLog.seek(readOnlyLog.length()); // undoing so move to end of logfile
         
         synchronized (Database.getBufferPool()) {
         	synchronized (this){
         		long current = readOnlyLog.getFilePointer();
         		while (current > LogFile.LONG_SIZE && !losers.isEmpty()) {
 	        		readOnlyLog.seek(current - LogFile.LONG_SIZE);
 	        		long recordOffset = readOnlyLog.readLong();
 	        		readOnlyLog.seek(recordOffset);
 	        		
 	        		int type = readOnlyLog.readInt();
 	                long tid = readOnlyLog.readLong();
 	                
 	                switch (type){
 	                
	 	                case LogType.UPDATE_RECORD: 
	 	                	if (losers.contains(tid)){
		 	                	Page beforeImg = LogFile.readPageData(readOnlyLog);
		 	                    Page afterImg = LogFile.readPageData(readOnlyLog);		               
		 	                    PageId pid = beforeImg.getId();
		 	                    Database.getBufferPool().discardPage(pid);
		 	                    Database.getCatalog().getDatabaseFile(pid.getTableId()).writePage(beforeImg);
		 	                    Database.getLogFile().logCLR(tid, beforeImg);
	 	                	}
	 	                    break;
	 	                case LogType.ABORT_RECORD:
	 	                	if (losers.contains(tid))
	 	                		throw new IOException("not possible");
	 	                case LogType.BEGIN_RECORD:
	 	                	if (losers.contains(tid)){
	 	                		Database.getLogFile().logAbort(tid);
	 	                		losers.remove(tid);
	 	                	}
	 	                	break;
	 	                case LogType.CLR_RECORD:
	 	                	break;
	 	                case LogType.COMMIT_RECORD:
	 	                	if (losers.contains(tid))
	 	                		throw new IOException("Cannot abort a committed transaction!");
	 	               case LogType.CHECKPOINT_RECORD:
	 	            	   	break;
	 	                default:
	 	                	throw new RuntimeException("Unexpected type!  Type = " + type);
 	                }
 	               current = recordOffset;
         		}
         	}
         }
    }
}
