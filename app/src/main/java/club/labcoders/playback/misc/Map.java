package club.labcoders.playback.misc;

import java.util.ArrayList;
import java.util.List;

import rx.functions.Func1;

public class Map<A, B> implements Func1<List<A>, List<B>> {
    private final Func1<A, B> function;

    public Map(Func1<A, B> function) {
        this.function = function;
    }

    @Override
    public List<B> call(List<A> as) {
        final List<B> bs = new ArrayList<B>(as.size());
        for(final A a : as) {
            bs.add(function.call(a));
        }
        return bs;
    }
}
