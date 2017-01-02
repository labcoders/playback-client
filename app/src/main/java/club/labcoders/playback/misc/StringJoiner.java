package club.labcoders.playback.misc;

import java.util.List;

public class StringJoiner {
    public final String delimiter;

    public StringJoiner(String delimiter) {
        this.delimiter = delimiter;
    }

    public String join(List<String> strings) {
        if(strings.size() == 0)
            return "";

        final StringBuffer sb = new StringBuffer(strings.get(0));

        for(int i = 1; i < strings.size(); i++) {
            sb.append(delimiter);
            sb.append(strings.get(i));
        }

        return sb.toString();
    }
}
