package network.darkhelmet.prism.actions.typeadapter;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import network.darkhelmet.prism.Prism;
import org.bukkit.Material;

import java.io.IOException;

public class MaterialIdAdapter extends TypeAdapter<Material> {

    @Override
    public Material read(JsonReader in) throws IOException {
        JsonToken token = in.peek();
        if (token == JsonToken.NULL) {
            in.nextNull();
            return Material.STONE;
        }
        if (token == JsonToken.STRING) {
            return Material.matchMaterial(in.nextString());
        }
        if (token == JsonToken.NUMBER) {
            return Prism.getItems().idsToMaterial(in.nextInt(), 0, false).material;
        }
        in.nextNull();
        return null;
    }

    @Override
    public void write(JsonWriter out, Material data) throws IOException {
        if (data == null || data.isAir()) {
            out.nullValue();
            return;
        }
        out.value(Prism.getItems().materialToIds(data, "0").first);
    }
}