package com.tulingdemo;

/**
 * Created by Administrator on 2017/2/9.
 */

//联系人信息类
public class ContactInfo {
    private String name;    //联系人名字
    private String number;  //联系人电话
    public ContactInfo(String name, String number) {
        this.name = name;
        this.number = number;
    }
    public String getName() {
        return name;
    }
    public String getNumber() {
        return number;
    }
}

