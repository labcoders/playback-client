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

    /**
     * Computes the number of bytes held in the buffer.
     * @return The number of bytes held in the buffer.
     */
    public int available() {
        try{
            lock.lock();
            if(head == null) // buffer is empty
                return 0;
            // otherwise head != tail, and if there's just one item in the buffer, tail = head + 1 % bufferSize
            return ((tail - head) % bufferSize);
        }
        finally {
            if(lock.isHeldByCurrentThread())
                lock.unlock();
        }
    }

    public boolean isFull() {
        return available() == bufferSize;
    }

    /**
     * Reads one short from the front of the circular buffer. If the buffer is empty, the return value is null.
     * @return The short.
     */
    public Short get() {
        try {
            lock.lock();

            if(available() == 0)
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

    /**
     * Reads at most the given number of samples from the circular buffer, and stores them in the given
     * destination buffer, starting at index zero. The destination buffer's size must be at least the
     * give size. The number of samples moved into the destination buffer is returned.
     * @param size The number of samples to move from the circular buffer into the array.
     * @param dest The array to move the samples into.
     * @return The number of bytes moved into the array.
     */
    public int get(final int size, short[] dest) {
        try {
            lock.lock();
            for(int i = 0; i < size; i++) {
                final Short sample = get();
                if(sample == null)
                    return i;
                dest[i] = sample;
            }
        }
        finally {
            if(lock.isHeldByCurrentThread())
                lock.unlock();
        }

        return size;
    }

    /**
     * Bulk copy bytes into the circular buffer.
     *
     * The total number of samples copied into the buffer is returned. If the overwrite flag is set,
     * then the return value will always equal the size parameter. If the overwrite flag is not set,
     * then the copying will stop at the first sample that would cause an overwrite.
     *
     * @param size The number of samples to copy from the source array.
     * @param src The source array to copy from
     * @param overwrite Whether to overwrite unread samples in the circular buffer.
     * @return The number of bytes written into the circular buffer
     */
    public int put(final int size, final short[] src, boolean overwrite) {
        try {
            lock.lock();

            for(int i = 0; i < size; i++) {
                if(!put(src[i], overwrite)) {
                    return i;
                }
            }
        }
        finally {
            if(lock.isHeldByCurrentThread())
                lock.unlock();
        }

        return size;
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