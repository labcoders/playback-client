package club.labcoders.playback.misc.fp;

import rx.functions.Func1;

public class Cast<A, B> implements Func1<A, B> {
    @Override
    public B call(A a) {
        return (B)a;
    }
}
