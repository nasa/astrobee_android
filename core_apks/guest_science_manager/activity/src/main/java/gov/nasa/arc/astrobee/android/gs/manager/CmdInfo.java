package gov.nasa.arc.astrobee.android.gs.manager;

import android.util.Log;

/**
 * Created by kmbrowne on 12/12/17.
 */

public class CmdInfo {
    public String mId;
    public String mApkName;
    public CmdType mType;

    public CmdInfo() {
        resetCmd();
    }

    public String getCmdType () {
        if (mType == CmdType.START) {
            return "start";
        } else if (mType == CmdType.CUSTOM) {
            return "custom";
        } else if (mType == CmdType.STOP) {
            return "stop";
        } else if (mType == CmdType.RESTART) {
            return "restart";
        }
        return "none";
    }

    public boolean isCmdEmpty() {
        if (mId.length() == 0 && mApkName.length() == 0 && mType == CmdType.NONE) {
            return true;
        }
        return false;
    }

    public void resetCmd() {
        mId = "";
        mApkName = "";
        mType = CmdType.NONE;
    }

    public void setCmd(String id, String origin, String apkName, CmdType type) {
        mId = id;
        mApkName = apkName;
        mType = type;
    }
}
