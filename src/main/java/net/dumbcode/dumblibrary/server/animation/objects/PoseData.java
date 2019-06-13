package net.dumbcode.dumblibrary.server.animation.objects;

import com.google.common.collect.Maps;
import com.google.gson.*;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;
import net.dumbcode.dumblibrary.client.animation.ModelContainer;
import net.minecraft.util.JsonUtils;

import java.lang.reflect.Type;
import java.util.Map;

/**
 * Information class to hold infomation about the model name, and the time it takes to complete
 */
@Getter
@Data
public class PoseData {
    private final String modelName;
    private final float time;
    @Accessors(chain = true) private ModelContainer.ModelLocation location;
    private final Map<String, CubeReference> cubes = Maps.newHashMap();


    public enum Deserializer implements JsonDeserializer<PoseData> {
        INSTANCE;

        @Override
        public PoseData deserialize(JsonElement element, Type typeOfT, JsonDeserializationContext context) {
            JsonObject json = element.getAsJsonObject();
            return new PoseData(
                    JsonUtils.getString(json ,"pose"),
                    JsonUtils.getFloat(json, "time")
            );
        }
    }

}
