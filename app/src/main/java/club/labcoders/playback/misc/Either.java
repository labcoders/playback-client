package club.labcoders.playback.misc;

import rx.functions.Func1;

public class Either<L, R> {
    private final L leftValue;
    private final R rightValue;
    private final EitherState state;

    public Either(
            final L leftValue,
            final R rightValue,
            final EitherState state) {
        this.leftValue = leftValue;
        this.rightValue = rightValue;
        this.state = state;
    }

    public static <L, R> Either<L, R> left(final L value) {
        return new Either<L, R>(value, null, EitherState.LEFT);
    }

    public static <L, R> Either<L, R> right(final R value) {
        return new Either<L, R>(null, value, EitherState.RIGHT);
    }

    public <T> T either(Func1<L, T> fLeft, Func1<R, T> fRight) {
        switch(state) {
            case LEFT:
                return fLeft.call(leftValue);
            case RIGHT:
                return fRight.call(rightValue);
            default:
                throw new RuntimeException("wtf");
        }
    }

    private enum EitherState {
        LEFT,
        RIGHT,
    }
}
