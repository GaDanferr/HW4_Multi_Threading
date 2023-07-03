import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;


public class Database {
    private Map<String, String> data;
    private final int maxReaders;
    private final Thread[] readers;
    private  Thread writer;
    private final ReentrantLock myLock;
    private int readerCount;
    private final Condition hasReaders;
    private final Condition hasWriter;


    public Database(int maxNumOfReaders) {
        data = new HashMap<>();  // Note: You may add fields to the class and initialize them in here. Do not add parameters!
        maxReaders = maxNumOfReaders;
        readers = new Thread[maxNumOfReaders];
        readerCount = 0;
        myLock = new ReentrantLock();
        hasWriter = myLock.newCondition();
        hasReaders = myLock.newCondition();



    }

    public void put(String key, String value) {
        data.put(key, value);
    }

    public String get(String key) {
        return data.get(key);
    }

    public boolean readTryAcquire() {
        myLock.lock();
        if((writer == null) && (readerCount < maxReaders)) {
            readerCount++;
            associateReader();
            myLock.unlock();
            return true;
        }
        else{
                myLock.unlock();
                return false;
            }
    }


    public void readAcquire() {
        myLock.lock();
        try {
            while ((writer != null) || (readerCount >= maxReaders)) {
                hasReaders.await();
            }
            readerCount++;
            associateReader();
        }catch (InterruptedException ignored){}
        finally {
            myLock.unlock();
        }
        
    }

    public void readRelease() {
        Thread currThread = Thread.currentThread();
        for(int i = 0 ; i < maxReaders ; i++){
            if( readers[i] == currThread){
                readers[i] = null;
                myLock.lock();
                readerCount--;
                hasReaders.signalAll();
                hasWriter.signalAll();
                myLock.unlock();
                return;
            }
        }
        throw new IllegalMonitorStateException("Illegal read release attempt");
    }

    public void writeAcquire() {
        myLock.lock();
        try {
            while((readerCount > 0) || (writer!=null))
                hasReaders.await();
            writer = Thread.currentThread();
        }catch (InterruptedException ignored){}
        finally{
            myLock.unlock();}
    }

    public boolean writeTryAcquire() {
        myLock.lock();
        if((readerCount > 0) || (writer!=null)) {
            writer = Thread.currentThread();
            myLock.unlock();
            return true;
        }
        else {
            myLock.unlock();
            return false;
        }

    }

    public void writeRelease() {
        if(writer != Thread.currentThread()){
            throw new IllegalMonitorStateException("Illegal write release attempt");
        }
        myLock.lock();
        writer = null;
        hasReaders.signalAll();
        myLock.unlock();
    }
    private synchronized void associateReader(){
        for(int i = 0 ; i < maxReaders ; i++){
            if( readers[i] == null){
                readers[i] = Thread.currentThread();
                return;
            }
        }
    }

}