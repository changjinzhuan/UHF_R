package com.handheld.uhfrdemo.cn.kcrxorg.pasmutil.cn.kcrx.bean;

public class TagUserdata {
    UserTraceData[] userTraceData;
    TagEpcData tagEpcDatabak;
    Boolean[] userErrorData;
    String hardwareVersion;
    String softVersion;
    String logCount;

    public String getHardwareVersion() {
        return hardwareVersion;
    }

    public void setHardwareVersion(String hardwareVersion) {
        this.hardwareVersion = hardwareVersion;
    }

    public String getSoftVersion() {
        return softVersion;
    }

    public void setSoftVersion(String softVersion) {
        this.softVersion = softVersion;
    }

    public String getLogCount() {
        return logCount;
    }

    public void setLogCount(String logCount) {
        this.logCount = logCount;
    }

    public UserTraceData[] getUserTraceData() {
        return userTraceData;
    }

    public void setUserTraceData(UserTraceData[] userTraceData) {
        this.userTraceData = userTraceData;
    }

    public TagEpcData getTagEpcDatabak() {
        return tagEpcDatabak;
    }

    public void setTagEpcDatabak(TagEpcData tagEpcDatabak) {
        this.tagEpcDatabak = tagEpcDatabak;
    }

    public Boolean[] getUserErrorData() {
        return userErrorData;
    }

    public void setUserErrorData(Boolean[] userErrorData) {
        this.userErrorData = userErrorData;
    }
}
