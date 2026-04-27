package com.infinite.file.util;

import com.infinite.file.entity.FileResource;
import lombok.Data;

import java.util.Arrays;

public class PathParser {
    
    public static PathInfo parse(String objectPath) {
        if (objectPath == null || objectPath.isEmpty()) {
            throw new IllegalArgumentException("Object path cannot be null or empty");
        }
        
        String[] segments = objectPath.split("/");
        PathInfo info = new PathInfo();
        
        // Set name (last segment)
        info.setName(segments[segments.length - 1]);
        
        // Set path depth (number of segments before name)
        info.setPathDepth(segments.length - 1);
        
        // Set path_1 to path_10
        for (int i = 0; i < Math.min(segments.length - 1, 10); i++) {
            info.setPath(i + 1, segments[i]);
        }
        
        // Set parent path
        if (segments.length > 1) {
            String[] parentSegments = Arrays.copyOf(segments, segments.length - 1);
            info.setParentPath(String.join("/", parentSegments));
        }
        
        // Set bucket name (first segment)
        if (segments.length > 0) {
            info.setBucketName(segments[0]);
        }
        
        return info;
    }
    
    public static void applyPathInfo(FileResource resource, String objectPath) {
        PathInfo info = parse(objectPath);
        resource.setName(info.getName());
        resource.setPathDepth(info.getPathDepth());
        resource.setParentPath(info.getParentPath());
        resource.setBucketName(info.getBucketName());
        resource.setPath1(info.getPath1());
        resource.setPath2(info.getPath2());
        resource.setPath3(info.getPath3());
        resource.setPath4(info.getPath4());
        resource.setPath5(info.getPath5());
        resource.setPath6(info.getPath6());
        resource.setPath7(info.getPath7());
        resource.setPath8(info.getPath8());
        resource.setPath9(info.getPath9());
        resource.setPath10(info.getPath10());
    }
    
    @Data
    public static class PathInfo {
        private String name;
        private Integer pathDepth;
        private String parentPath;
        private String bucketName;
        private String path1;
        private String path2;
        private String path3;
        private String path4;
        private String path5;
        private String path6;
        private String path7;
        private String path8;
        private String path9;
        private String path10;
        
        public void setPath(int index, String value) {
            switch (index) {
                case 1: path1 = value; break;
                case 2: path2 = value; break;
                case 3: path3 = value; break;
                case 4: path4 = value; break;
                case 5: path5 = value; break;
                case 6: path6 = value; break;
                case 7: path7 = value; break;
                case 8: path8 = value; break;
                case 9: path9 = value; break;
                case 10: path10 = value; break;
            }
        }
    }
}
