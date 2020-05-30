package mmn16_1;

import java.util.Vector;

public class PartyVec extends Vector<String>{

	/**
	 * Construct a new PartyVec so that its internal data array<br>has size 10 and its standard capacity increment is zero.
	 */
	public PartyVec() {
		super();
	}
	
	/**
	 * Constructs an empty vector with the specified initial capacity and 
	 * <br>with its capacity increment equal to zero
	 * @param party - participate's name array
	 * @param initialCapacity - initial capacity 
	 * @Throws IllegalArgumentException - if the specified initial capacityis negative
	 */
	public PartyVec(String[] party,int initialCapacity) {
		super(initialCapacity);
		for(int i = 0;i<party.length;i++) {
			this.add(party[i]);
		}
	}
	
	public String toString() {
		String toString = "";
		for(int i = 0;i<this.size();i++) {
			toString += (this.get(i)+"#");
		}
		return toString;
	}
	
	/**Appends the specified element's array to the end of this PartyVec. 
	 * @param appendList list of names to append
	 * @return true only if all the array's string added successfully else return false
	 */
	public boolean add(String[] appendList) {
		boolean beenAdded = true;
		for (int i = 0; i < appendList.length; i++) {
			if(!this.add(appendList[i])) {
				beenAdded = false;
			}
		}
		return beenAdded;
	}
}
