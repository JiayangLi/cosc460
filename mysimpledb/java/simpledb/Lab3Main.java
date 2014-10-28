package simpledb;

import java.io.IOException;
import java.util.ArrayList;

public class Lab3Main {

    public static void main(String[] argv) 
       throws DbException, TransactionAbortedException, IOException {

        System.out.println("Loading schema from file:");
        // file named college.schema must be in mysimpledb directory
        Database.getCatalog().loadSchema("college.schema");

        TransactionId tid = new TransactionId();
        
        //Catalog.getTableId() is case sensitive for now
        //SeqScans on students, takes and profs
        SeqScan scanStudents = new SeqScan(tid, Database.getCatalog().getTableId("students"));
        SeqScan scanTakes = new SeqScan(tid, Database.getCatalog().getTableId("takes"));
        SeqScan scanProfs = new SeqScan(tid,Database.getCatalog().getTableId("profs"));
        
        //join students and takes on sid
        JoinPredicate jp = new JoinPredicate(0, Predicate.Op.EQUALS, 0);
        Join iterator = new Join(jp, scanStudents, scanTakes);
        
        //join the previous iterator and profs on favorite course id
        jp = new JoinPredicate(2, Predicate.Op.EQUALS, 3);
        iterator = new Join(jp, scanProfs, iterator);
        
        //filter out tuples without "hay"
        StringField hay = new StringField("hay", Type.STRING_LEN);
        Predicate p = new Predicate(1, Predicate.Op.EQUALS, hay);
        Filter filter = new Filter(p, iterator);
        
        //project only student names
        ArrayList<Integer> fieldList = new ArrayList<Integer>();
        fieldList.add(4);
        Type[] typeList = new Type[]{Type.STRING_TYPE};
        Project project = new Project(fieldList, typeList, filter);

        // query execution: we open the iterator of the root and iterate through results
        System.out.println("Query results:");
        project.open();
        while (project.hasNext()) {
            Tuple tup = project.next();
            System.out.println("\t"+tup);
        }
        project.close();
        Database.getBufferPool().transactionComplete(tid);
    }

}