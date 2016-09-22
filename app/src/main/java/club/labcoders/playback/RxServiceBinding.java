package club.labcoders.playback;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import rx.Observable;
import rx.subjects.BehaviorSubject;
import timber.log.Timber;

public class RxServiceBinding<T> {
    private final Context context;
    private final BehaviorSubject<T> subject;

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            subject.onNext((T)service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            subject.onCompleted();
        }
    };

    public RxServiceBinding(
            final Context context,
            final Intent intent,
            final int flags) {
        this.context = context;
        this.subject = BehaviorSubject.create();
        context.bindService(intent, connection, flags);

    }

    public ServiceConnection getConnection() {
        return connection;
    }

    public Observable<T> binder(boolean cleanup) {
        if(cleanup)
            return subject.asObservable().doOnUnsubscribe(
                    () -> {
                        Timber.d("Unbinding service connection from RxServiceBinding.");
                        context.unbindService(connection);
                    }
            );
        else
            return subject.asObservable();
    }
}
