package net.dumbcode.dumblibrary.server.ecs.component.additionals;

import net.dumbcode.dumblibrary.server.ecs.component.impl.data.FlattenedLayerProperty;
import net.dumbcode.dumblibrary.server.utils.IndexedObject;

import java.util.function.Consumer;

public interface RenderFlattenedLayerComponent {
    void gatherComponents(Consumer<IndexedObject<FlattenedLayerProperty>> registry);
}
