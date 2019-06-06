package net.dumbcode.dumblibrary.client.model.tabula.baked;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.gson.*;
import lombok.Cleanup;
import lombok.Data;
import lombok.Value;
import net.dumbcode.dumblibrary.client.animation.TabulaUtils;
import net.dumbcode.dumblibrary.client.model.tabula.TabulaModelInformation;
import net.minecraft.client.renderer.block.model.ModelBlock;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ICustomModelLoader;
import net.minecraftforge.client.model.IModel;
import net.minecraftforge.client.model.PerspectiveMapWrapper;
import org.apache.commons.io.IOUtils;

import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public enum TabulaModelHandler implements ICustomModelLoader {
    INSTANCE;

    private static final JsonParser PARSER = new JsonParser();
    private static final Pattern PATTERN = Pattern.compile("layer(\\d+)$");
    private final Set<String> namespaces = Sets.newHashSet();
    private IResourceManager manager;


    public void allow(String namespace) {
        this.namespaces.add(namespace);
    }

    @Override
    public boolean accepts(ResourceLocation modelLocation) {
        return this.namespaces.contains(modelLocation.getNamespace()) && modelLocation.getPath().endsWith(".tbl");
    }

    @Override
    public IModel loadModel(ResourceLocation modelLocation) throws Exception {
        String path = modelLocation.getPath().replaceAll("\\.tbl", ".json");
        IResource resource = this.manager.getResource(new ResourceLocation(modelLocation.getNamespace(), path));
        @Cleanup InputStreamReader reader = new InputStreamReader(resource.getInputStream());
        String string = IOUtils.toString(reader);
        JsonObject json = PARSER.parse(string).getAsJsonObject();
        TabulaModelInformation information = TabulaUtils.getModelInformation(new ResourceLocation(JsonUtils.getString(json, "tabula")));
        ModelBlock modelBlock = ModelBlock.deserialize(string);
        List<TextureLayer> allTextures = Lists.newArrayList();
        Set<String> layers = Sets.newHashSet();
        for (String key : modelBlock.textures.keySet()) {
            Matcher matcher = PATTERN.matcher(key);
            if (matcher.matches()) {
                allTextures.add(new TextureLayer(key, new ResourceLocation(modelBlock.textures.get(key)), Integer.parseInt(matcher.group(1))));
                layers.add(key);
            }
        }
        String particle = modelBlock.textures.get("particle");
        ResourceLocation part = particle == null ? new ResourceLocation("missingno") : new ResourceLocation(particle);

        List<LightupData> lightupData = Lists.newArrayList();

        if(JsonUtils.isJsonArray(json, "lightup_data")) {
            for (JsonElement data : JsonUtils.getJsonArray(json, "lightup_data")) {
                lightupData.add(LightupData.parse(data.getAsJsonObject(), layers));
            }
        }

        return new TabulaIModel(Collections.unmodifiableList(allTextures), lightupData, part, PerspectiveMapWrapper.getTransforms(modelBlock.getAllTransforms()), information, modelBlock.ambientOcclusion, modelBlock.isGui3d(), modelBlock.getOverrides());
    }

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
        this.manager = resourceManager;
    }

    @Data public static class TextureLayer{ private final String name; private final ResourceLocation loc; private final int layer; private TextureAtlasSprite sprite; }
compiler error
    /**
     * Lightup data is used to apply fake light to the quads. Each entry goes in an array called "lightup_data". The following is an example
     *
     * <pre>{@code
     *     {
     *         "layers_applied": [          <--- These are the layers that this full-bright should apply to. These are defined before in your "textures" section of the json file
     *             "layer0",
     *             "layer1",
     *         ],
     *         "cubes_lit": [               <--- A list of the cubes lit. Each one of these entries, no matter the type, is a {@link LightupEntry}
     *             "headPiece1",            <--- Having them as strings will mean the whole cube is lit
     *             "headPiece2",
     *             {                        <--- If you don't want the whole cube lit up, you can define the faces for the full-bright to act on
     *                 "cube_name",
     *                 "faces": [           <--- The faces that will be light up with full-bright. Note that if the cube is rotated say 90* on the x axis, then UP will not be UP
     *                     "north",
     *                     "south",
     *                     "west"
     *                 ]
     *             }
     *         ],
     *         "sky_light": 12,              <--- The minimum block-light level that this quad will be set to. If left blank will be defaulted to 15
     *         "block_light": 3             <--- The minimum skylight level that the quad will be set to. If left blank will be defaulted to 15
     *     }
     * }
     * </pre>
     * Note that this is just one entry into a json array. You can have multiple entries for different light levels or different cubes on different layers.
     */
    @Value
    public static class LightupData {
        private Set<String> layersApplied;
        private List<LightupEntry> entry;
        private int blockLight;
        private int skyLight;

        public static LightupData parse(JsonObject json, Set<String> layers) {
            Set<String> layersApplied = Sets.newHashSet();
            if(JsonUtils.isJsonArray(json, "layers")) {
                JsonArray arr = JsonUtils.getJsonArray(json, "layers");
                for (int i = 0; i < arr.size(); i++) {
                    layersApplied.add(JsonUtils.getString(arr.get(i), "layers[" + i + "]"));
                }
            } else {
                layersApplied.addAll(layers);
            }


            List<LightupEntry> list = Lists.newArrayList();
            for (JsonElement cube : JsonUtils.getJsonArray(json, "cubes_lit")) {
                if(cube.isJsonPrimitive() && ((JsonPrimitive)cube).isString()) {
                    list.add(new LightupEntry(cube.getAsString(), EnumFacing.values()));
                } else if (cube.isJsonObject()) {
                    JsonObject cubeJson = cube.getAsJsonObject();
                    String name = JsonUtils.getString(cubeJson, "cube_name");
                    JsonArray faces = JsonUtils.getJsonArray(cubeJson, "faces");
                    String[] astr = new String[faces.size()];
                    for (int i = 0; i < faces.size(); i++) {
                        astr[i] = JsonUtils.getString(faces.get(i), "faces[" + i + "]");
                    }
                    Set<EnumFacing> facings = Sets.newHashSet();
                    for (EnumFacing value : EnumFacing.values()) {
                        for (String face : astr) {
                            if(value.getName2().equalsIgnoreCase(face)) {
                                facings.add(value);
                                break;
                            }
                        }
                    }
                    list.add(new LightupEntry(name, facings.toArray(new EnumFacing[0])));
                } else {
                    throw new JsonSyntaxException("Expected a String or a Json Object");
                }
            }

            int blockLight = JsonUtils.getInt(json, "block_light", 15);
            int skyLight = JsonUtils.getInt(json, "sky_light", 15);


            return new LightupData(layersApplied, list, blockLight, skyLight);
        }
    }

    @Value public static class LightupEntry { String cubeName; EnumFacing[] facing; }
}