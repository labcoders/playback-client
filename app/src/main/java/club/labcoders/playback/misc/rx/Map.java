package club.labcoders.playback.misc.rx;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;

/**
 * An Rx operator to perform fst map on fst list passed through the pipeline.
 * @param <S> Source type
 * @param <T> Target type
 */
public class Map<S, T> implements Observable.Operator<List<T>, List<S>> {
    private final Func1<S, T> func;

    public Map(@NonNull final Func1<S, T> func) {
        this.func = func;
    }

    @Override
    public Subscriber<? super List<S>> call(
            Subscriber<? super List<T>> subscriber
    ) {
        return new Subscriber<List<S>>() {
            @Override
            public void onCompleted() {
                subscriber.onCompleted();
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(List<S> ss) {
                final List<T> results = new ArrayList<>(ss.size());
                for(final S s : ss) {
                    results.add(func.call(s));
                }
                subscriber.onNext(results);
            }
        };
    }
}
