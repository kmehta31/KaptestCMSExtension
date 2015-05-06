package com.kaptest.action;

import java.util.List;

import org.alfresco.repo.action.ParameterDefinitionImpl;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;

public class DeleteExpiredContentActionExecutor extends ActionExecuterAbstractBase{   
   private NodeService nodeService;  
   public static final String NAME = "add-aspect";
   public static final String PARAM_ASPECT_NAME = "aspect-name";
   public void setNodeService(NodeService nodeService){
      this.nodeService = nodeService;
   }  
   
   protected void executeImpl(Action action, NodeRef actionedUponNodeRef){
	   	System.out.println("Execute DeleteExpiredContentActionExecutor");
	   	if (this.nodeService.exists(actionedUponNodeRef)){
	   		System.out.println("Node being Deleted:"+actionedUponNodeRef);
	   		nodeService.deleteNode(actionedUponNodeRef);
	   		System.out.println("Node Deleted successfully..");
	   	}
   }   
   
   @Override
   protected void addParameterDefinitions(List<ParameterDefinition> paramList)
   {
	   /*paramList.add(new ParameterDefinitionImpl(PARAM_ASPECT_NAME,
			   DataTypeDefinition.QNAME, true,
			   getParamDisplayLabel(PARAM_ASPECT_NAME)));*/
   }
   
   


}