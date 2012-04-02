package com.android.BluetoothManager.Routing.Packet_types;

/* Represents a data packet in inter node communication
 */
public class DataPacket {
	String dest_addr;
	String src_name;
	String msg;
	
	public DataPacket(String dest_addr, String src_name, String msg) {
		super();
		this.dest_addr = dest_addr;
		this.src_name= src_name;
		this.msg = msg;
	}

	public String getDest_addr() {
		return dest_addr;
	}

	public String getSrc_Name()
	{
		return src_name;
	}
	
	public void setDest_addr(String dest_addr) {
		this.dest_addr = dest_addr;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}
	
	public String toString()
	{
		return "4,"+dest_addr+","+src_name+","+msg;
	}
	
}
