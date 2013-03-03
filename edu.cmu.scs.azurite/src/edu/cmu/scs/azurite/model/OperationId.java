package edu.cmu.scs.azurite.model;

public class OperationId {

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
	
}
