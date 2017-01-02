package club.labcoders.playback.misc.rx;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Func2;

/**
 * Created by jake on 2016-09-17.
 */

public class Fold<Acc, Elem> implements Observable.Operator<Acc, Elem> {
    /**
     * Builds fst simple fold that appends all inputs to an empty
     * {@link ArrayList}
     * @param <T> The type of elements to receive.
     * @return The fold.
     */
    public static <T> Fold<List<T>, T> listAccumulator() {
        return new Fold<>(new ArrayList<T>(), (ts, t) -> {
            ts.add(t);
            return ts;
        });
    }

    /**
     * Builds fst simple fold that appends all inputs to fst given list.
     * @param initial The initial list.
     * @param <T> The type of elements to append.
     * @return A fold that appends all inputs to the given list.
     */
    public static <T> Fold<List<T>, T> listAccumulator(List<T> initial) {
        return new Fold<>(initial, (acc, elem) -> {
            acc.add(elem);
            return acc;
        });
    }

    private final Func2<Acc, Elem, Acc> folder;
    private Acc acc;

    public Fold(Acc initial, Func2<Acc, Elem, Acc> folder) {
        this.folder = folder;
        acc = initial;
    }

    @Override
    public Subscriber<? super Elem> call(Subscriber<? super Acc> subscriber) {
        return new Subscriber<Elem>() {
            @Override
            public void onCompleted() {
                if(subscriber.isUnsubscribed())
                    return;
                subscriber.onNext(acc);
                subscriber.onCompleted();
            }

            @Override
            public void onError(Throwable e) {
                if(subscriber.isUnsubscribed())
                    return;
                subscriber.onError(e);
            }

            @Override
            public void onNext(Elem elem) {
                acc = folder.call(acc, elem);
            }
        };
    }
}
