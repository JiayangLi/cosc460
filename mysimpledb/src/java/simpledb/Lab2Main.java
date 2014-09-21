package simpledb;

import java.io.File;

public class Lab2Main {
	/**
	 * Return a new tuple by changing the value of a given tuple at a given index
	 * to a specified new value
	 * @param t	the old tuple to be updated
	 * @param index	the index of the field that needs updating
	 * @param newValue	the new value that the new tuple should have
	 * @return	a updated tuple
	 */
	private static Tuple updateTuple(Tuple t, int index, int newValue){
		TupleDesc td = t.getTupleDesc();
		IntField newField = new IntField(newValue);
		Tuple newTuple = new Tuple(td);
		
		for(int i = 0; i < td.numFields(); i++){
			if (i == index){
				newTuple.setField(i, newField);
			}
			else{
				newTuple.setField(i, t.getField(i));
			}
		}
		
		return newTuple;
	}
	
	/**
	 * Return a new tuple based on the schema and values
	 * @param td	the schema that the return tuple is going to have
	 * @param values	the valus that the return tuple is going to have
	 * @return	a new tuple based on the given schema and values
	 */
	private static Tuple makeTuple(TupleDesc td, int[] values){
		Tuple newTuple = new Tuple(td);
		
		for (int i = 0; i < values.length; i++){
			IntField f = new IntField(values[i]);
			newTuple.setField(i, f);
		}
		
		return newTuple;
	}
	
	public static void main(String[] args){
		// construct a 3-column table schema
        Type types[] = new Type[]{ Type.INT_TYPE, Type.INT_TYPE, Type.INT_TYPE };
        String names[] = new String[]{ "field0", "field1", "field2" };
        TupleDesc descriptor = new TupleDesc(types, names);
        
        // create the table, associate it with some_data_file.dat
        // and tell the catalog about the schema of this table.
        HeapFile table1 = new HeapFile(new File("some_data_file.dat"), descriptor);
        Database.getCatalog().addTable(table1, "test");
        
        // construct the query: we use a simple SeqScan, which spoonfeeds
        // tuples via its iterator.
        TransactionId tid = new TransactionId();        
        SeqScan f = new SeqScan(tid, table1.getId());
        
        BufferPool bp = Database.getBufferPool();
        
        try {
            // and run it
            f.open();
            while (f.hasNext()) {
                Tuple tup = f.next();
  
                IntField field = (IntField) tup.getField(1);
                if (field.getValue() < 3){
                	//update tuple 
                	Tuple update = updateTuple(tup, 1, 3);
                	System.out.println("Update tuple: " + tup + "to be: " + update);
                	
                	bp.deleteTuple(tid,tup);
                	bp.insertTuple(tid, table1.getId(), update);
                }
            }
            
            //insert tuple 99 99 99
            Tuple insert = makeTuple(descriptor, new int[]{99,99,99});
            System.out.println("Insert tuple: " + insert);
            bp.insertTuple(tid, table1.getId(), insert);
            
            f.close();
            
            //flush all dirty pages
            Database.getBufferPool().flushAllPages();
            
            
            Database.getBufferPool().transactionComplete(tid);
        } catch (Exception e) {
            System.out.println ("Exception : " + e);
        }
	}
}
