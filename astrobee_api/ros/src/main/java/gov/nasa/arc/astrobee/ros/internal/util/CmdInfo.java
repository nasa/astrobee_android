package gov.nasa.arc.astrobee.ros.internal.util;

public class CmdInfo {
    public String mId;
    public String mOrigin;
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
        }
        return "none";
    }

    public boolean isCmdEmpty() {
        if (mId.length() == 0 && mOrigin.length() == 0 &&
                mApkName.length() == 0 && mType == CmdType.NONE) {
            return true;
        }
        return false;
    }

    public void resetCmd() {
        mId = "";
        mOrigin = "";
        mApkName = "";
        mType = CmdType.NONE;
    }

    public void setCmd(String id, String origin, String apkName, CmdType type) {
        mId = id;
        mOrigin = origin;
        mApkName = apkName;
        mType = type;
    }
}

