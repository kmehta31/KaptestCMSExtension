package com.kaptest.action;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.repo.workflow.WorkflowModel;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetRow;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.workflow.WorkflowDefinition;
import org.alfresco.service.cmr.workflow.WorkflowService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;

import com.kaptest.model.KaptestModel;
import com.kaptest.workflow.action.PublishContent;


public class StartPublishWorkflowActionExecutor extends ActionExecuterAbstractBase{   
   private NodeService nodeService;
   private WorkflowService  workflowService;
   private SearchService searchService;
   private AuthenticationService authenticationService;
   private static Logger logger = Logger.getLogger(StartPublishWorkflowActionExecutor.class);
   public void setAuthenticationService(AuthenticationService authenticationService) {
	   this.authenticationService = authenticationService;
   }
   public void setNodeService(NodeService nodeService){
      this.nodeService = nodeService;
   }
   public void setWorkflowService(WorkflowService workflowService){
	      this.workflowService = workflowService;
   }
   public void setSearchService(SearchService searchService){
	      this.searchService = searchService;
   }
   
   protected void executeImpl(Action action, NodeRef actionedUponNodeRef){
	   try{		   
		   String currentUser = authenticationService.getCurrentUserName();   	
		   WorkflowDefinition ktpWFDefinition = workflowService.getDefinitionByName(KaptestModel.WORKFLOW_KTP_PUBLISH_DEFINITION_NAME);	   	
		   NodeRef wfPackage = workflowService.createPackage(null);	   	
		   Map<QName, Serializable> properties = new HashMap<QName, Serializable>();
		   properties.put(WorkflowModel.ASSOC_PACKAGE, wfPackage);		   
		   properties.put(WorkflowModel.PROP_DUE_DATE, new Date());		
		   properties.put(WorkflowModel.PROP_START_DATE, new Date().getTime());
		   properties.put(WorkflowModel.PROP_DESCRIPTION, "Publish Content");
		   properties.put(WorkflowModel.PROP_WORKFLOW_DESCRIPTION, "Publish Content");	
		   
		   logger.debug("Started Publish Workflow for:"+currentUser);
		   System.out.println("Started Publish Workflow for:"+currentUser);
		   List<NodeRef> itemsToAdd = (ArrayList<NodeRef>) getInitialEditedItemsList(currentUser);
		   logger.debug("Total items to added to List:"+itemsToAdd.size());
		   System.out.println("Total items to added to List:"+itemsToAdd.size());
		   if(itemsToAdd.size()>0){	
			   Iterator<NodeRef> itemsToAddIterator = itemsToAdd.iterator();	
			   while(itemsToAddIterator.hasNext()){
				   NodeRef itemNodeRef = itemsToAddIterator.next();				
				   this.nodeService.addChild( 
				    wfPackage, 
					itemNodeRef, 
					WorkflowModel.ASSOC_PACKAGE_CONTAINS, 
					QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, QName 
						.createValidLocalName((String) this.nodeService.getProperty(itemNodeRef, 
						ContentModel.PROP_NAME)))); 		   
			   }			   
			   workflowService.startWorkflow(ktpWFDefinition.getId(), properties);
		   }
		}catch(Exception e){
			e.printStackTrace();
		}
   } 
   
   public List<NodeRef> getInitialEditedItemsList(String initiator){
	   	List<NodeRef> itemsToAddToWF = new ArrayList<NodeRef>();
		ResultSet results = null;
		String QUERY = "TYPE:\"ktp:kaptestContent\" AND ASPECT:\"ktp:isEdited\" AND @cm\\:modifier:\""+initiator+"\"";
		logger.debug("Query String:"+QUERY);
		System.out.println("Query String:"+QUERY);
		SearchParameters sp = new SearchParameters();
		sp.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
		sp.setLanguage(SearchService.LANGUAGE_LUCENE);		
		sp.setQuery(QUERY);	  	    	     		
		results = searchService.query(sp);
		if(results!=null){
			logger.debug("Total Files found to Transfer to QA:"+results.length());        		
			System.out.println("Total Files found to Transfer to QA:"+results.length());
	        for(ResultSetRow row : results){	        	
	        	itemsToAddToWF.add(row.getNodeRef());
	        }        
		}
		ResultSet resultsFolders = null;		
		
		SearchParameters spFolder = new SearchParameters();
		spFolder.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
		spFolder.setLanguage(SearchService.LANGUAGE_LUCENE);    	
    	String query = "TYPE:\"cm:folder\" AND ASPECT:\"ktp:isEdited\" AND @cm\\:modifier:\""+initiator+"\"";    	
    	spFolder.setQuery(query);    	
    	resultsFolders = searchService.query(spFolder);
        if(resultsFolders!=null){
        	logger.debug("Total new Folders to publish:"+resultsFolders.length()); 
        	System.out.println("Total new Folders to publish:"+resultsFolders.length()); 
	        for(ResultSetRow row : resultsFolders){
	        	itemsToAddToWF.add(row.getNodeRef());                
	        }        
        }		
		return itemsToAddToWF;
	}
   
   
   
   @Override
   protected void addParameterDefinitions(List<ParameterDefinition> paramList)
   {
      // there are no parameters
   }
   
   


}