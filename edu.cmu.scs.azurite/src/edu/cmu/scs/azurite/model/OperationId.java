package edu.cmu.scs.azurite.model;

import java.util.ArrayList;
import java.util.List;

import edu.cmu.scs.azurite.commands.runtime.RuntimeDC;

public class OperationId implements Comparable<OperationId> {

	public final long sid;
	public final long id;
	
	public OperationId(long sid, long id) {
		this.sid = sid;
		this.id = id;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (id ^ (id >>> 32));
		result = prime * result + (int) (sid ^ (sid >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		OperationId other = (OperationId) obj;
		if (id != other.id)
			return false;
		if (sid != other.sid)
			return false;
		return true;
	}

	@Override
	public int compareTo(OperationId other) {
		if (this.sid < other.sid) { return -1; }
		if (this.sid > other.sid) { return 1; }
		if (this.id < other.id) { return -1; }
		if (this.id > other.id) { return 1; }
		
		return 0;
	}

	public static List<OperationId> getOperationIdsFromRuntimeDCs(List<RuntimeDC> dcs) {
		List<OperationId> ids = new ArrayList<OperationId>();
		for (RuntimeDC dc : dcs) {
			ids.add(dc.getOperationId());
		}
		return ids;
	}

	@Override
	public String toString() {
		return Long.toString(this.sid) + "_" + Long.toString(this.id);
	}
	
}
