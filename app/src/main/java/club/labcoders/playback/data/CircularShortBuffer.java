package club.labcoders.playback.data;

import timber.log.Timber;

/**
 * Created by chokboy on 10/7/16.
 */

public class CircularShortBuffer {
    private int size;
    private int maxSize;
    private int start;
    private short[][] shorts;

    /*
        Initialize the circular buffer with a given size
        which is the total number of short arrays that
        this buffer can contain.
     */
    public CircularShortBuffer(int size) {
        if (size <= 0) {
            Timber.wtf("Cannot allocate circular buffer with size <= 0");
            throw new RuntimeException("Shitting fucks");
        }
        this.start = 0;
        this.size = 0;
        this.maxSize = size;
        shorts = new short[size][];
    }

    /*
        Returns an ordered array of short arrays. This will only
        return the arrays that are currently in use, and will
        not contain any of the ones containing garbage data.
     */
    public short[][] toArray() {
        short[][] orderedShorts = new short[size][];
        for (int i = 0; i < size; i++) {
            int internalIdx;
            if (start + i >= maxSize) {
                internalIdx = start + i - maxSize;
            } else {
                internalIdx = start + i;
            }
            orderedShorts[i] = shorts[internalIdx];
        }
        return orderedShorts;
    }

    /*
        Put a short[] in the circular buffer.
     */
    public void add(short[] arr) {
        if (size == maxSize) {
            // put at current start.
            shorts[start++] = arr;
            if (start == maxSize) {
                start = 0;
            }
        } else {
            // put at index indicated by size.
            shorts[size++] = arr;
        }
    }

    /*
        Get the total number of in-use short arrays.
     */
    public int getSize() {
        return size;
    }

    /*
        Clears the buffer.
     */
    public void reset() {
        start = 0;
        size = 0;
    }
}
