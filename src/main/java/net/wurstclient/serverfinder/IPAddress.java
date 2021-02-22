package net.wurstclient.serverfinder;

public class IPAddress {
	
	private int[] octets;
	
	private int port;
	
	public IPAddress(int o1, int o2, int o3, int o4, int port) {
		this.octets = new int[] { o1, o2, o3, o4 };
		this.port = port;
	}
	
	public IPAddress(int[] octets, int port) {
		this.octets = octets;
		this.port = port;
	}
	
	public static IPAddress fromText(String ip) {
		String[] sections = ip.split(":");
		if (sections.length < 1 || sections.length > 2)
			return null;
		
		int port = 25565;
		if (sections.length == 2) {
			try {
				port = Integer.parseInt(sections[1].trim());
			}
			catch (NumberFormatException e) {
				return null;
			}
		}
		
		int[] octets = new int[4];
		
		String[] address = sections[0].trim().split("\\.");
		if (address.length != 4)
			return null;
		
		for (int i = 0; i < 4; i++) {
			try {
				octets[i] = Integer.parseInt(address[i].trim());
			}
			catch (NumberFormatException e) {
				return null;
			}
		}
		
		return new IPAddress(octets, port);
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof IPAddress))
			return false;
		
		IPAddress other = (IPAddress)o;
		
		if (octets.length != other.octets.length)
			return false;
		
		if (port != other.port)
			return false;
		
		for (int i = 0; i < octets.length; i++)
			if (octets[i] != other.octets[i])
				return false;
		
		return true;
	}
	
	@Override
	public int hashCode() {
		assert (octets.length == 4);
		
		int hash = 43;
		hash = hash * 59 + octets[0];
		hash = hash * 83 + octets[1];
		hash = hash * 71 + octets[2];
		hash = hash * 17 + octets[3];
		hash = hash * 31 + port;
		return hash;
	}
	
	@Override
	public String toString() {
		if (octets.length == 0)
			return null;
		
		String result = "" + octets[0];
		for (int i = 1; i < octets.length; i++) {
			result += "." + octets[i];
		}
		result += ":" + port;
		return result;
	}
}
