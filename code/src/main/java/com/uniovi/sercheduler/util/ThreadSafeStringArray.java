package com.uniovi.sercheduler.util;

import com.uniovi.sercheduler.dto.analysis.MultiResult;
import com.uniovi.sercheduler.service.FitnessCalculatorMulti;
import java.util.Arrays;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThreadSafeStringArray {
  static final Logger LOG = LoggerFactory.getLogger(ThreadSafeStringArray.class);
  // Size of the array, can be set to a default or loaded from a configuration
  private static final int DEFAULT_SIZE = 10;
  private final ReadWriteLock lock = new ReentrantReadWriteLock();
  private MultiResult[] array;
  private int currentIndex = 0; // To track the next available position

  // Private constructor to prevent instantiation from outside
  private ThreadSafeStringArray() {
    this.array = new MultiResult[DEFAULT_SIZE];
  }

  // Static method to get the instance of the class
  public static ThreadSafeStringArray getInstance() {
    return InstanceHolder.instance;
  }

  /**
   * Updates the array with the new values.
   *
   * @param method The method tha was used.
   * @param makespan The value of the makespan.
   */
  public void setValue(String method, Double makespan) {
    lock.writeLock().lock();
    try {
      if (currentIndex >= array.length) {
        // Array is full, you might want to handle this case
        // e.g., throw an exception or resize the array
      } else {

        var currentMulti = array[currentIndex];
        if (currentMulti == null) {
          currentMulti = new MultiResult();
        }
        currentMulti.setMakespan(currentMulti.getMakespan() + makespan);
        switch (method) {
          case "simple" -> currentMulti.setSimple((short) (currentMulti.getSimple() + 1));
          case "heft" -> currentMulti.setHeft((short) (currentMulti.getHeft() + 1));
          case "rank" -> currentMulti.setRank((short) (currentMulti.getRank() + 1));
          default -> LOG.debug("Unknown fitness function: {}", method);
        }
        array[currentIndex] = currentMulti;
        currentIndex++; // Increment the index for the next insertion
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  public MultiResult getValue(int index) {
    lock.readLock().lock();
    try {
      return array[index];
    } finally {
      lock.readLock().unlock();
    }
  }

  public MultiResult[] getArray() {
    lock.readLock().lock();
    try {
      return Arrays.copyOf(array, array.length);
    } finally {
      lock.readLock().unlock();
    }
  }

  public void recreateArray(int newSize) {
    lock.writeLock().lock();
    try {
      array = new MultiResult[newSize];
      currentIndex = 0;
    } finally {
      lock.writeLock().unlock();
    }
  }

  public void restartIndex() {
    lock.writeLock().lock();
    try {
      currentIndex = 0;
    } finally {
      lock.writeLock().unlock();
    }
  }

  private static final class InstanceHolder {
    // The single instance of the class
    private static final ThreadSafeStringArray instance = new ThreadSafeStringArray();
  }

  // Additional methods...
}
