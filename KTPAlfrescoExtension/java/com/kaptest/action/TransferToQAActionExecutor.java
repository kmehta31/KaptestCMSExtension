package com.kaptest.action;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetRow;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.transfer.NodeCrawler;
import org.alfresco.service.cmr.transfer.NodeCrawlerFactory;
import org.alfresco.service.cmr.transfer.TransferDefinition;
import org.alfresco.service.cmr.transfer.TransferEndEvent;
import org.alfresco.service.cmr.transfer.TransferFailureException;
import org.alfresco.service.cmr.transfer.TransferService2;
import com.kaptest.model.KaptestModel;

public class TransferToQAActionExecutor extends ActionExecuterAbstractBase
{   
	private ServiceRegistry registry;
	private NodeService nodeService;
	private TransferService2 transferService;
	private NodeCrawlerFactory nodeCrawlerFactory;
	
	public void setNodeService(NodeService nodeService){
      this.nodeService = nodeService;
	} 
	public void setTransferService(TransferService2 transferService){
	      this.transferService = transferService;
	}	
	public void setNodeCrawlerFactory(NodeCrawlerFactory nodeCrawlerFactory){
	       this.nodeCrawlerFactory = nodeCrawlerFactory;
	}
	public void setServiceRegistry(ServiceRegistry registry) {
		this.registry = registry;
	}	

	protected void executeImpl(Action action, NodeRef actionedUponNodeRef){
		System.out.println("Ready for Transfer Service to QA");		
		ResultSet results = null;
		Set<NodeRef> nodesInTree = new HashSet<NodeRef>();		
		SearchParameters sp = new SearchParameters();
    	sp.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
    	sp.setLanguage(SearchService.LANGUAGE_LUCENE);
    	sp.setQuery("TYPE:\"ktp:kaptestContent\" AND ASPECT:\"ktp:isReadyToQA\"");	  	    	     
        results = registry.getSearchService().query(sp);
        if(results!=null){
        	System.out.println("Total Nodes to Transfer:"+results.length());        		
	        for(ResultSetRow row : results){
	        	nodesInTree.add(row.getNodeRef());                
	        }        
			TransferDefinition transferDef = new TransferDefinition();
			transferDef.setNodes(nodesInTree);
			try{
				System.out.println("Start content Transfer to FSTR_QA");
				TransferEndEvent transferEvent = transferService.transfer("FSTR_QA", transferDef);	
				if(transferEvent.getMessage().equalsIgnoreCase("success")){
					System.out.println("Start content Transfer to FSTR_QA");	
					for(NodeRef nodeRef : nodesInTree){
						if(nodeService.hasAspect(nodeRef, KaptestModel.ASPECT_IS_EDITED)){
					       	nodeService.removeAspect(nodeRef, KaptestModel.ASPECT_IS_EDITED);
					    }
						if(nodeService.hasAspect(nodeRef, KaptestModel.ASPECT_IS_READY_TO_QA)){
					       	nodeService.removeAspect(nodeRef, KaptestModel.ASPECT_IS_READY_TO_QA);
					    }
						nodeService.addAspect(nodeRef, KaptestModel.ASPECT_IS_READY_TO_PUBLISH, null);
					}
				}else{
					System.out.println("Transfer not successful."+transferEvent.getMessage());
				}
			}catch (Exception ex){
				ex.printStackTrace();
			}
        }else{
        	System.out.println("No Node To Transfer to QA");
        }
		
	}
   
	@Override
	protected void addParameterDefinitions(List<ParameterDefinition> paramList){
      // there are no parameters
	}
}