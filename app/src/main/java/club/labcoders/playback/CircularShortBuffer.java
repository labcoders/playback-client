package club.labcoders.playback;

import java.util.concurrent.locks.ReentrantLock;

import timber.log.Timber;

/**
 * Created by jake on 16-09-13.
 */
public class CircularShortBuffer {
    short[] buffer;
    final int bufferSize;
    Integer head, tail;
    ReentrantLock lock;

    public CircularShortBuffer(final int bufferSize) {
        this.bufferSize = bufferSize;
        buffer = new short[bufferSize];
        head = null;
        tail = null;
        lock = new ReentrantLock(true); // use fair locking
    }

    public int available() {
        try{
            lock.lock();
            if(head == null) // buffer is empty
                return bufferSize;
            // otherwise head != tail, and if there's just one item in the buffer, tail = head + 1 % bufferSize
            return bufferSize - ((tail - head + 1) % bufferSize);
        }
        finally {
            if(lock.isHeldByCurrentThread())
                lock.unlock();
        }
    }

    public boolean isFull() {
        return available() == 0;
    }

    /**
     * Reads one short from the front of the circular buffer. If the buffer is empty, the return value is null.
     * @return The short.
     */
    public Short get() {
        try {
            lock.lock();

            if(head == tail)
                return null;

            final short result = buffer[head];
            incrementHead();
            return result;
        }
        finally {
            if(lock.isHeldByCurrentThread())
                lock.unlock();
        }
    }

    /**
     * Writes one short onto the end of the circular buffer.
     * If the buffer is full, then the result depends on the overwrite flag.
     * Otherwise, the value of the overwrite flag is irrelevant.
     * If the overwrite flag is set and the buffer is full, then the oldest sample in the buffer is
     * overwritten; else, the put is not performed, and false is the return value. In all other cases,
     * the return value is true.
     * @param value The sample to write into the buffer.
     * @return Whether the sample was written.
     */
    public boolean put(short value, boolean overwrite) {
        try {
            lock.lock();

            if(isFull()) {
                if(!overwrite)
                    return false;

                incrementTail();
                buffer[tail] = value;
                incrementHead();
            }
        }
        finally {
            if(lock.isHeldByCurrentThread())
                lock.unlock();
        }

        return true;
    }

    private void incrementHead() {
        head++;
        if(head > bufferSize)
            head = 0;
    }

    private void incrementTail() {
        tail++;
        if(tail > bufferSize)
            tail = 0;
    }
}