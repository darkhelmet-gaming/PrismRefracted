package network.darkhelmet.prism.actions.typeadapter;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public class SlotIgnoreNegativeOneAdapter extends TypeAdapter<String> {
    private static final String NEGATIVE_ONE = "-1";

    @Override
    public String read(JsonReader in) throws IOException {
        if (in.peek() == JsonToken.NULL) {
            in.nextNull();
            return "-1";
        }

        return in.nextString();
    }

    @Override
    public void write(JsonWriter out, String data) throws IOException {
        if (data == null || data.equals(NEGATIVE_ONE)) {
            out.nullValue();
            return;
        }

        out.value(data);
    }
}