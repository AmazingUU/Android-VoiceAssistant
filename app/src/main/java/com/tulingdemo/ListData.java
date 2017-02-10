package com.tulingdemo;

//listview的item类
public class ListData {
	
	public static final int SEND = 1;      // 发送
	public static final int RECEIVER = 2;  // 接收
	private String content;
	// 标识，判断是左边，还是右边。
	private int flag;    
	private String time;
	
	public ListData(String content,int flag) {
		setContent(content);
		setFlag(flag);
	}
	
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
	public int getFlag() {
		return flag;
	}
	public void setFlag(int flag) {
		this.flag = flag;
	}
}

