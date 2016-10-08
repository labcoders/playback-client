package club.labcoders.playback.misc;

/**
 * Created by jake on 2016-09-18.
 */

public class Box<T> {
    private T value;

    public Box() {
        value = null;
    }

    public Box(T value) {
        this.value = value;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }
}
