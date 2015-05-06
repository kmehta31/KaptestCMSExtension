package com.kaptest.action;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.repo.transfer.ChildAssociatedNodeFinder;
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
import org.alfresco.service.cmr.transfer.TransferService2;
import org.alfresco.service.namespace.QName;

import com.kaptest.model.KaptestModel;

public class TransferAllToQAActionExecutor extends ActionExecuterAbstractBase
{   
	private ServiceRegistry registry;
	private NodeService nodeService;
	private TransferService2 transferService;
	private NodeCrawlerFactory nodeCrawlerFactory;
	
	private final static Set<QName> DEFAULT_ASPECTS_TO_EXCLUDE = new TreeSet<QName>();
    
    static 
    {
        DEFAULT_ASPECTS_TO_EXCLUDE.add(KaptestModel.ASPECT_IS_EDITED);
        DEFAULT_ASPECTS_TO_EXCLUDE.add(KaptestModel.ASPECT_IS_READY_TO_QA);
        DEFAULT_ASPECTS_TO_EXCLUDE.add(KaptestModel.ASPECT_IS_READY_TO_PUBLISH);
        DEFAULT_ASPECTS_TO_EXCLUDE.add(KaptestModel.ASPECT_IS_PUBLISHED);      
    }
	
	
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
		SearchParameters sp = new SearchParameters();
    	sp.addStore(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE);
    	sp.setLanguage(SearchService.LANGUAGE_LUCENE);
    	sp.setQuery("TYPE:\"cm:folder\" AND  @cm\\:name:\"KAPTEST EDITORIAL\"");	  	    	     
        results = registry.getSearchService().query(sp);
        NodeRef rootNode = null;
        if(results!=null){
        	System.out.println("Total Root Nodes found:"+results.length());        		
	        for(ResultSetRow row : results){
	        	rootNode = row.getNodeRef();                
	        }      
        }
        
		NodeCrawler crawler = nodeCrawlerFactory.getNodeCrawler();
		crawler.setNodeFinders(new ChildAssociatedNodeFinder(ContentModel.ASSOC_CONTAINS));		
		Set<NodeRef> nodesInTree = crawler.crawl(rootNode);		
        if(nodesInTree!=null){
        	System.out.println("Total Nodes to Transfer:"+nodesInTree.size());        	
			TransferDefinition transferDef = new TransferDefinition();
			transferDef.setNodes(nodesInTree);
			//transferDef.setExcludedAspects(DEFAULT_ASPECTS_TO_EXCLUDE);
			try{
				System.out.println("Start All Transfer to FSTR_QA Repository");
				TransferEndEvent transferEvent = transferService.transfer("FSTR_QA", transferDef);				
				if(transferEvent.getMessage().equalsIgnoreCase("success")){
					System.out.println("Start Transfer to FSTR_QA is Successful");	
					for(NodeRef nodeRef : nodesInTree){
						if(nodeService.hasAspect(nodeRef, KaptestModel.ASPECT_IS_EDITED)){
					       	nodeService.removeAspect(nodeRef, KaptestModel.ASPECT_IS_EDITED);
					    }
						if(nodeService.hasAspect(nodeRef, KaptestModel.ASPECT_IS_READY_TO_QA)){
					       	nodeService.removeAspect(nodeRef, KaptestModel.ASPECT_IS_READY_TO_QA);
					    }
						if(!nodeService.hasAspect(nodeRef, KaptestModel.ASPECT_IS_READY_TO_PUBLISH)){
					       	nodeService.addAspect(nodeRef, KaptestModel.ASPECT_IS_READY_TO_PUBLISH,null);
					    }																	
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