package com.chirikhin.cyclelist;

import org.apache.log4j.Logger;

import java.util.Iterator;
import java.util.LinkedList;

import org.apache.log4j.Logger;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class CycleLinkedList<T> implements Iterable<T> {
    private static final Logger logger = Logger.getLogger(CycleLinkedList.class.getName());

    private final BlockingQueue<T> linkedList;
    private final int maxSize;
    private int currentSize;

    public CycleLinkedList(int size) {
        this.maxSize = size;
        linkedList = new LinkedBlockingQueue<>(size);
    }

    public boolean add(T t) {
        if (currentSize + 1 < maxSize) {
            currentSize++;
            return linkedList.add(t);
        } else {
            return false;
        }
    }

    public void clear() {
        currentSize = 0;
        linkedList.clear();
    }

    public int size() {
        return currentSize;
    }

    @Override
    public Iterator<T> iterator() {
        return linkedList.iterator();
    }
}