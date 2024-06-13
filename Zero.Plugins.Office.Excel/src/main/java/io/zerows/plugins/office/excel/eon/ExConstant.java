package io.zerows.plugins.office.excel.eon;

/**
 * @author lang : 2024-06-13
 */
public interface ExConstant {

    String K_TYPE = "__type__";
    String K_CONTENT = "__content__";

    interface CELL {
        String UUID = "{UUID}";
        String PWD = "PWD";
        String NAME_CONFIG = "NAME:config";
        String CODE_CONFIG = "CODE:config";
        String CODE_CLASS = "CODE:class";
        String CODE_NAME_CONFIG = "CODE:NAME:config";

        String P_JSON = "JSON";
        String P_FILE = "FILE";

        String[] PREFIX = new String[]{P_FILE, P_JSON};
    }
}
