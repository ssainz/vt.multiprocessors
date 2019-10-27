package edu.vt.ece.hw4.locks;

import java.util.concurrent.atomic.AtomicInteger;

public class SpinSleepLock implements Lock {
    ThreadLocal<Integer> mySlotIndex = new ThreadLocal<Integer>(){
        protected Integer initialValue(){
            return 0;
        }
    };
    public AtomicInteger tail ;
    public AtomicInteger numberOfThreadsInLock ;
    public volatile boolean[] flag;
    public volatile Thread[] threads = null;
    public int maxSpin = 0;
    public int size = 0;

    public SpinSleepLock(int capacity, int maxSpin) {
        tail = new AtomicInteger(0);
        numberOfThreadsInLock = new AtomicInteger(0);
        threads = new Thread[capacity];
        flag = new boolean[capacity];
        flag[0] = true;
        this.maxSpin = maxSpin <= 0 ? 1: maxSpin;
        this.size = capacity;
    }



    @Override
    public void lock() {
        int slot = tail.getAndIncrement() % size;
        threads[slot] = Thread.currentThread();
        int threadsInLock = numberOfThreadsInLock.getAndIncrement() + 1;
        mySlotIndex.set(slot);

        if(threadsInLock  - 1 > maxSpin){
            try {

                synchronized (Thread.currentThread()){
                    Thread.currentThread().wait();
                }
            } catch (InterruptedException e) {
                // Awake, continue:
            }
        }

        while(!flag[slot]){}

    }

    @Override
    public void unlock() {
        int slot = mySlotIndex.get();
        int threadsInLock = numberOfThreadsInLock.getAndDecrement() - 1; // without itself.
        if(threadsInLock  > maxSpin ){
            // Here idea is that soon another thread that was sleeping will wake up (once lag[(slot + 1) % size] = true)
            // We need to see if there are any threads waiting (threadsInLock)
            // Awake some thread :
            while(threads[(slot + maxSpin + 1) % size].getState() == Thread.State.RUNNABLE){} // Wait until it becomes waiting

            synchronized (threads[(slot + maxSpin + 1) % size]){
                threads[(slot + maxSpin + 1) % size].notify();
            }
        }

        flag[slot] = false;
        flag[(slot + 1) % size] = true;

    }

    @Override
    public boolean trylock() {
        return false;
    }

}
