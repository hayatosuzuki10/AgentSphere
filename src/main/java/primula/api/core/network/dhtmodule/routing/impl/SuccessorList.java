
package primula.api.core.network.dhtmodule.routing.impl;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;

import primula.api.core.network.dhtmodule.address.Address;
import primula.api.core.network.dhtmodule.nodefunction.CommunicationException;
import primula.api.core.network.dhtmodule.nodefunction.Node;
import primula.api.core.network.dhtmodule.nodefunction.impl.SocketProxy;
import primula.api.core.network.dhtmodule.utill.Logger;


public final class SuccessorList implements Serializable{

	/**
	 * List storing the successor references in correct order.
	 */
	private LinkedList<Node> successors = null;

        
	/**
	 * Maximum number of references - initialized in constructor.
	 */
	private int capacity;


	/**
	 * Object logger.
	 */
	private Logger logger;
        
                private Node directSuccessor=null;
                
                private Node directPredecessor=null;

	/**
	 * Creates an empty list of successors.
	 * 
	 * @param localID
	 *            This node's ID; is used for comparison operations of other
	 *            node references.
	 * @param numberOfEntries
	 *            Number of entries to be stored in this successor list.
	 * @param parent
	 *            Reference on this objects parent.
	 * @param entries
	 *            Reference on the entry repository for replication purposes.
	 */
	 public SuccessorList( int numberOfEntries) {
		this.logger = Logger.getLogger(SuccessorList.class);
		this.logger.debug("Logger initialized.");

		if (numberOfEntries < 1) {
			throw new IllegalArgumentException(
					"SuccessorList has to be at least of length 1! "
							+ numberOfEntries + "is not a valid value!");
		}
		this.capacity = numberOfEntries;
		this.successors = new LinkedList<Node>();

	}
        
                public void initializeSuccessorList(Node localNode){
                    successors.add(localNode);
                }

                public void setDirectSuccessr(Node successor){
                    this.directPredecessor=successor;
                    this.addSuccessor(successor);
                }
                
                

                
	public synchronized final void addSuccessor(Node nodeToAdd) {
                        if(nodeToAdd==null){
                            NullPointerException e = new NullPointerException("Successor to add must not be null!");
                            this.logger.error("Nullpointer", e);
                            throw e;
                        }
                        this.successors.push(nodeToAdd);
	}
        
        public synchronized final void addSuccessor(Address addressOfLocalNode,Address targetAddress){
            if(addressOfLocalNode==null||targetAddress==null){
                NullPointerException e = new NullPointerException("Address must not be null!");
                this.logger.error("Null pointer", e);
                throw e;
            }
            Node successor = null;
        try {
            successor = SocketProxy.create(addressOfLocalNode, targetAddress);
        } catch (CommunicationException ex) {
            java.util.logging.Logger.getLogger(SuccessorList.class.getName()).log(Level.SEVERE, null, ex);
        }

            this.successors.push(successor);
        }
        
      public  final List<Node> getSuccessors(){
            return this.successors;
        }
      
      public final int size(){
          return this.successors.size();
      }
        
    public   final boolean containsSuccessor(Node successor){
            if(successor==null){
            NullPointerException   e = new NullPointerException("successor to check must not be null! ");
            this.logger.error("Nullpointer", e);
            throw e;
            }
            boolean isContains=false;
            isContains=this.successors.contains(successor);
            return isContains;
       }
        
      public  synchronized final void removeSuccessor(Node successorToDelete){
            if(successorToDelete==null){
                NullPointerException e = new NullPointerException("Successor to delete must not be null");
                this.logger.error("NullPointer", e);
                throw e;
            }
            boolean removeSuccessful=true;
            removeSuccessful=this.successors.remove(successorToDelete);
            if(!removeSuccessful){
                this.logger.error("There is no reference to delete!:"+successorToDelete.toString());
            }
        }

//   public void addSuccessor(Address newSuccessorAddress) {
//        if(newSuccessorAddress==null){
//                            NullPointerException e = new NullPointerException("Address of Successor to add must not be null!");
//                            this.logger.error("Nullpointer", e);
//                            throw e;
//        }
//        Node successor = new NodeImpl(null, localAddress);
//        this.successors.add(successor);
//    }        
        
	/**
	 * Returns the reference on the direct successor; may be <code>null</code>,
	 * if the list is empty.
	 * 
	 * @return Direct successor (or <code>null</code> if list is empty).
	 */
	public final Node getDirectSuccessor() {
		if (this.successors.size() == 0) {
			return null;
		}
		return this.successors.get(0);
	}

	/**
	 * @return the capacity
	 */
	public final int getCapacity() {
		return capacity;
	}

	public final int getSize() {
		return this.successors.size();
	}



}