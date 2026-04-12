package com.infinite.common.constant;

public final class CommonConstant {
    public static final String STAR = "*";
    public static final String SPACE = "\\s+";
    public static final String SLASH = "/";
    public static final String COMMON = "[\\s+/]";
    public static final String DEFAULT_PAGE = "0";
    public static final String DEFAULT_SIZE = "10";
    private CommonConstant() {
    }

    public static final class UPDATE_REDIS {
        public static final Long TRUE = 1L;
        public static final Long FALSE = 0L;
    }

    public static final class IS_ACTIVE {
        public static final Long INACTIVE = 0L;
        public static final Long ACTIVE = 1L;
        public static final Long ERRORED = 2L;
    }

}
