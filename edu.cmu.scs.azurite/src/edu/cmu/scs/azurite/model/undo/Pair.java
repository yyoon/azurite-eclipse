package edu.cmu.scs.azurite.model.undo;

/**
 * A simple pair class which is NOT compatible with Hash collections.
 * (i.e. doesn't implement hashCode() or equals())
 * 
 * @author YoungSeok Yoon
 *
 * @param <T1> Type of the first argument
 * @param <T2> Type of the second argument
 */
public class Pair<T1, T2> {
	
	private T1 mFirst;
	private T2 mSecond;
	
	public Pair(T1 first, T2 second) {
		mFirst = first;
		mSecond = second;
	}
	
	public T1 getFirst() {
		return mFirst;
	}
	
	public void setFirst(T1 first) {
		mFirst = first;
	}
	
	public T2 getSecond() {
		return mSecond;
	}
	
	public void setSecond(T2 second) {
		mSecond = second;
	}

}
