package club.labcoders.playback.db;

/**
 * A simple query operation is one that just requires running a fixed
 * (non-parametric) string of SQL.
 * @param <T> The object that results from the query operation.
 */
public interface SimpleQueryOperation<T> {
    String getQueryString();
}
