package com.handheld.uhfrdemo.cn.kcrxorg.pasmutil.cn.kcrx.bean;

public class TagEpcData {
    public long getTagid() {
        return tagid;
    }

    public void setTagid(long tagid) {
        this.tagid = tagid;
    }

    public int getVersionid() {
        return versionid;
    }

    public void setVersionid(int versionid) {
        this.versionid = versionid;
    }

    public int getPervalueid() {
        return pervalueid;
    }

    public void setPervalueid(int pervalueid) {
        this.pervalueid = pervalueid;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public String getRandom() {
        return random;
    }

    public void setRandom(String random) {
        this.random = random;
    }

    public int getOperatecount() {
        return operatecount;
    }

    public void setOperatecount(int operatecount) {
        this.operatecount = operatecount;
    }

    public String getCheckcode() {
        return checkcode;
    }

    public void setCheckcode(String checkcode) {
        this.checkcode = checkcode;
    }

    public String getLockstuts() {
        return lockstuts;
    }

    public void setLockstuts(String lockstuts) {
        this.lockstuts = lockstuts;
    }

    public Boolean getLockeEx() {
        return lockeEx;
    }

    public void setLockeEx(Boolean lockeEx) {
        this.lockeEx = lockeEx;
    }

    public Boolean getEpcEx() {
        return epcEx;
    }

    public void setEpcEx(Boolean epcEx) {
        this.epcEx = epcEx;
    }

    public Boolean getHasElec() {
        return hasElec;
    }

    public void setHasElec(Boolean hasElec) {
        this.hasElec = hasElec;
    }

    public Boolean getJobstuts() {
        return jobstuts;
    }

    public void setJobstuts(Boolean jobstuts) {
        this.jobstuts = jobstuts;
    }

    long tagid;
    int versionid;
    int pervalueid;
    int amount;
    String random;
    int operatecount;
    String checkcode;
    String lockstuts;
    Boolean lockeEx;
    Boolean epcEx;
    Boolean hasElec;
    Boolean jobstuts;

    public String getEpcString() {
        return EpcString;
    }

    public void setEpcString(String epcString) {
        EpcString = epcString;
    }

    String EpcString;
}
