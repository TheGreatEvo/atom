package org.shotrush.atom.model;

import lombok.Data;
import org.bukkit.Material;
import java.util.ArrayList;
import java.util.List;

@Data
public class DisplayModel {
    private String id;
    private String name;
    private String author;
    private long created;
    private List<DisplayPart> parts;
    private ModelMetadata metadata;
    private BoundingBox boundingBox;
    
    public DisplayModel() {
        this.parts = new ArrayList<>();
        this.created = System.currentTimeMillis();
        this.metadata = new ModelMetadata();
    }
    
    public DisplayModel(String id, String name) {
        this();
        this.id = id;
        this.name = name;
    }
    
    @Data
    public static class DisplayPart {
        private String type;
        private Material material;
        private String blockState;
        private String itemNbt;
        private TransformData transform;
        private List<String> tags;
        
        public DisplayPart() {
            this.transform = new TransformData();
            this.tags = new ArrayList<>();
        }
    }
    
    @Data
    public static class TransformData {
        private float[] translation;
        private float[] leftRotation;
        private float[] scale;
        private float[] rightRotation;
        private float[] matrix;
        
        public TransformData() {
            this.translation = new float[]{0, 0, 0};
            this.leftRotation = new float[]{0, 0, 0, 1};
            this.scale = new float[]{1, 1, 1};
            this.rightRotation = new float[]{0, 0, 0, 1};
        }
    }
    
    @Data
    public static class ModelMetadata {
        private int partCount;
        private String description;
        private List<String> tags;
        private boolean animated;
        private float defaultRotationSpeed;
        
        public ModelMetadata() {
            this.tags = new ArrayList<>();
            this.animated = false;
            this.defaultRotationSpeed = 1.0f;
        }
    }
    
    
    @Data
    public static class BoundingBox {
        private float minX, minY, minZ;
        private float maxX, maxY, maxZ;
        
        public float getWidth() {
            return maxX - minX;
        }
        
        public float getHeight() {
            return maxY - minY;
        }
        
        public float getDepth() {
            return maxZ - minZ;
        }
    }
}
