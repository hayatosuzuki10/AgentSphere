/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package primula.api.core.network.dhtmodule.routing.impl;


import primula.api.core.network.dhtmodule.utill.Logger;


/**
 *
 * @author VENDETTA
 */
final class StabilizeTask  implements Runnable{


    protected final static Logger logger = Logger.getLogger(StabilizeTask.class);

    
    @Override
    public void run() {
//        try{
//            final boolean debugEnabled = StabilizeTask.logger.isEnabledFor(Logger.LogLevel.DEBUG);
//            final boolean infoEnabled = StabilizeTask.logger.isEnabledFor(Logger.LogLevel.INFO);
//            
//            if(debugEnabled){
//                StabilizeTask.logger.debug("Stablize method has been invoked periodically");
//            }
//            
//            Node successor = this.references.getPredecessor();
//            if(successor==null){
//                if(infoEnabled){
//                    StabilizeTask.logger.info("Nothing to stabilize,as successor is null");
//                    return;
//                }
//            }else{
//                List<Node> mySuccessorsPredecessorAndSuccessorList;
//                try{
//                    mySuccessorsPredecessorAndSuccessorList= successor.notify(this.parent);
//                    if(infoEnabled){
//                        StabilizeTask.logger.info("Recieved response to notify request from successor"+successor.getNodeID());
//                    }
//                }catch(CommunicationException e){
//                    if(debugEnabled){
//                        StabilizeTask.logger.debug("Invocation of notify on node "+successor.getNodeID()+" was not succesful due to a commmunication failure!"
//                                + "Successor has failed during stabilization! Removing successor!",e);
//                    }
//                    this.references.removeReference(successor);
//                    return;
//                }
//                
//                if(mySuccessorsPredecessorAndSuccessorList.size()>0&&
//                        mySuccessorsPredecessorAndSuccessorList.get(0)!=null){
//                    if(this.parent.getNodeID().equals(mySuccessorsPredecessorAndSuccessorList.get(0).getNodeID())){
//                        RefsAndEntries refsAndEntries = successor.notifyAndCopyEntries(this.parent);
//                        mySuccessorsPredecessorAndSuccessorList = refsAndEntries.getRefs();
//                        this.entries.addAll(refsAndEntries.getEntries());
//                    }
//                }
//                for(Node newReference : mySuccessorsPredecessorAndSuccessorList){
//                    this.references.addReference(newReference);
//                    if(debugEnabled){
//                        logger.debug("Added new reference; "+newReference);
//                    }
//                }
//                if(infoEnabled){
//                    StabilizeTask.logger.info("Invocation of notify on node "+successor.getNodeID()+" was successful");
//                }
//                
//            }
//        }catch(Exception e){
//            StabilizeTask.logger.warn("Unexpected Exception caught in Stabilize Task",e);
//            e.printStackTrace();
//        }
        
    }
    
}
