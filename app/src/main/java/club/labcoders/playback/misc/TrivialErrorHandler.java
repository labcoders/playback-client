package club.labcoders.playback.misc;

import rx.functions.Action1;

public class TrivialErrorHandler implements Action1<Throwable> {
    private final String message;

    public TrivialErrorHandler() {
        this.message = null;
    }

    public TrivialErrorHandler(String message) {
        this.message = message;
    }

    private void raise() {
        if(message == null)
            throw new RuntimeException();
        else
            throw new RuntimeException(message);
    }

    @Override
    public void call(Throwable throwable) {
        throwable.printStackTrace();
        raise();
    }
}
