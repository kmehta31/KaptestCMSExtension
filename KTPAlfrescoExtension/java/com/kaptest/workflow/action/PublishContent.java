package com.kaptest.workflow.action;
import java.io.Serializable;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.transaction.Status;
import javax.transaction.UserTransaction;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.transfer.TransferServiceImpl2;
import org.alfresco.repo.workflow.jbpm.JBPMNode;
import org.alfresco.repo.workflow.jbpm.JBPMSpringActionHandler;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.coci.CheckOutCheckInService;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.transfer.TransferDefinition;
import org.alfresco.service.cmr.transfer.TransferEndEvent;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.cmr.version.VersionService;
import org.alfresco.service.namespace.QName;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.Logger;
import org.jbpm.graph.exe.ExecutionContext;
import org.springframework.beans.factory.BeanFactory;

import com.kaptest.model.KaptestModel;

public class PublishContent extends JBPMSpringActionHandler{	
	private static final long serialVersionUID = 1L;
	private static Logger logger = Logger.getLogger(PublishContent.class);
	private static final String PUBLISH_TARGET_PROCESS_VARIABLE = "publishTarget";
	private NodeService nodeService;
	private TransferServiceImpl2 transferService;
	private VersionService versionService;	
	private ServiceRegistry serviceRegistry;
	private CheckOutCheckInService checkOutCheckInService;
	
	private static final String PUBLISH_QA_STATUS_VARIABLE = "publishQAStatus";
	private static final String PUBLISH_PROD_STATUS_VARIABLE = "publishPRODStatus";
	private static String PUBLISH_STATUS_SUCCESS = "success";
	private static String PUBLISH_STATUS_FAILED = "failed";
	
	
	@Override
	protected void initialiseHandler(BeanFactory factory){		
		nodeService = (NodeService)factory.getBean("nodeService");		
		transferService = (TransferServiceImpl2)factory.getBean("transferService2");		
		versionService = (VersionService)factory.getBean("versionService");		
		serviceRegistry = (ServiceRegistry)factory.getBean("ServiceRegistry");		
		checkOutCheckInService = serviceRegistry.getCheckOutCheckInService();
	}

	public void execute(ExecutionContext executionContext){
		logger.debug("Inside PublishContent execute");
		System.out.println("Inside PublishContent execute");
		Set<NodeRef> nodesInTree = null;
		String target= "";		
		try{		
			target = (String) executionContext.getVariable(PublishContent.PUBLISH_TARGET_PROCESS_VARIABLE);			
			Object object = executionContext.getVariable( "bpm_package");
			NodeRef objInitiator = ((JBPMNode)executionContext.getVariable("initiator")).getNodeRef();		
			String initiator = (String)nodeService.getProperty(objInitiator, ContentModel.PROP_USERNAME);
			logger.debug("Initiator:"+initiator);				
			System.out.println("Initiator:"+initiator);
			long processId = executionContext.getProcessInstance().getId();
			String wfProcessId = "jbpm$"+processId;				
			if(object!= null){					
				NodeRef bpmPackageNodeRef = ((JBPMNode) object).getNodeRef();				
				nodesInTree = new HashSet<NodeRef>();				
				//1. Add all package items to Set for publishing			
				List<ChildAssociationRef> children = nodeService.getChildAssocs(bpmPackageNodeRef);
				logger.debug("Total files/folders in wf to publish:"+children.size());	
				System.out.println("Total files/folders in wf to publish:"+children.size());
				for (ChildAssociationRef childAssoc : children) {
					NodeRef childNodeRef = childAssoc.getChildRef();
					nodesInTree.add(childNodeRef);			
				}		        
				if(nodesInTree.size()>0){
					if(target.equalsIgnoreCase("ABORT_POSTQADEPLOY")){
						System.out.println("Get Working copy & Cancel CHeckout on Abort");
						
						/** Cancel checkout as System user*/
						final Set<NodeRef> treeNodes = nodesInTree;						
						AuthenticationUtil.runAs(new RunAsWork<String>() {
				            public String doWork() throws Exception {
				            	AuthenticationUtil.setFullyAuthenticatedUser(AuthenticationUtil.getSystemUserName());
				            	String message = cancelCheckoutNodes(treeNodes);
				            	System.out.println("Status message from cancelCheckout:"+message);
				            	return message;
				            }
				        }, AuthenticationUtil.getSystemUserName());	
						
						
						/** Add isEdited Aspect back for all items with normal user*/
		            	for(NodeRef nodeRef : nodesInTree){			            	
			            	if(!nodeService.hasAspect(nodeRef, KaptestModel.ASPECT_IS_EDITED)){			            		
			            		Map<QName,Serializable> propMap = new HashMap<QName,Serializable>();
								nodeService.addAspect(nodeRef, KaptestModel.ASPECT_IS_EDITED, propMap);	
								System.out.println("Aspect isEdited added to the content successful");
							}		            	
		            	}
					}else{				
						String status = publish(target, nodesInTree);
						if(target.equalsIgnoreCase("FSTR_QA")){
							executionContext.setVariable(PUBLISH_QA_STATUS_VARIABLE, status);
						}else if(target.equalsIgnoreCase("FSTR_PROD")){
							executionContext.setVariable(PUBLISH_PROD_STATUS_VARIABLE, status);
						}
							
						if(status.equalsIgnoreCase(PUBLISH_STATUS_SUCCESS)){						
							final String strTarget = target;
							final Set<NodeRef> treeNodes = nodesInTree;
							final String strWFProcessID = wfProcessId;						
							AuthenticationUtil.runAs(new AuthenticationUtil.RunAsWork<String>(){				        
								public String doWork() throws Exception{
						        	if(serviceRegistry!=null){				        	
						        	UserTransaction trx_A = serviceRegistry.getTransactionService().getUserTransaction();
						        	try{
						        		 trx_A.begin();					        		 
						        		 AuthenticationUtil.setFullyAuthenticatedUser(AuthenticationUtil.getSystemUserName());					        		 
						        		 applyAspectsAfterPublish(strTarget,treeNodes,strWFProcessID);					        		 
						        		 trx_A.commit();					        		 					        		 
						        	}catch(Exception e){
						        		try{
						        			if (trx_A.getStatus() == Status.STATUS_ACTIVE){
						        	    	   trx_A.rollback();
						        			}
						        		}
						        	    catch(Throwable ee){
						        	       ee.printStackTrace();
						        	    }
										throw new AlfrescoRuntimeException(e.getLocalizedMessage());
						        	 }					        	
						        }
						        	 return "";
						        }
						    }, AuthenticationUtil.getSystemUserName());
							
							//Refresh Kaptest Environments
							refreshKaptestEnv(strTarget);
						}
					}
				}			
			}	
		}catch (Exception ex){
			ex.printStackTrace();		
		}
	}
	
	public String publish(String target, Set<NodeRef> nodesInTree) throws Exception{		
		final TransferDefinition transferDef = new TransferDefinition();
		final String strTarget = target;
		transferDef.setNodes(nodesInTree);			
		String status = AuthenticationUtil.runAs(new RunAsWork<String>() {
            public String doWork() throws Exception {
            	TransferEndEvent transferEvent = transferService.transfer(strTarget, transferDef);
            	if(transferEvent.getMessage().equalsIgnoreCase("success")){				
    				return PUBLISH_STATUS_SUCCESS;				
    			}else{        				
    				return PUBLISH_STATUS_FAILED+"-" +transferEvent.getMessage();        				
    			}			
            }
        }, AuthenticationUtil.getAdminUserName());		
		if(!status.equalsIgnoreCase(PUBLISH_STATUS_SUCCESS)){
			throw new AlfrescoRuntimeException(status);
		}		
		return status;
	}
	
	public String cancelCheckoutNodes(Set<NodeRef> nodesInTree){
		FileFolderService fileFolderService = serviceRegistry.getFileFolderService();
		int intSetSize = nodesInTree.size();
		System.out.println("Into Cancel Checkout Documents");
		String message = "";
		boolean isError = false;
		for(NodeRef nodeRef : nodesInTree){       	
        	if(intSetSize < 51){
            	if(!fileFolderService.getFileInfo(nodeRef).isFolder()){          	
	            	NodeRef workingCopy = checkOutCheckInService.getWorkingCopy(nodeRef);
	            	if(workingCopy!=null){
		            	checkOutCheckInService.cancelCheckout(workingCopy);			            	
	            	}else{
	            		isError = true;
	            		message = message + "Cancel CHeckout has no checked out document for:"+nodeRef+"\n";
	            	}
            	}
        	}else{
        		message = "No cancel checkout for set of more than 50";
        	}
        	if(!isError){
        		message = "All Documents executed cancel checkout successfully"; 
        	}        	
    	}
		return message;
	}
	
	public void refreshKaptestEnv(String target){
		Properties props = new Properties();			
		HttpClient httpclient1 = null;		
		if(target.equalsIgnoreCase("FSTR_QA")){		
			try {
				httpclient1 = new DefaultHttpClient();
				props.load(this.getClass().getResourceAsStream("/com/kaptest/workflow/action/kaptest.properties"));
				String QAURL = props.getProperty("kaptest_qa_refesh_url");				
				HttpGet httpChannel = new HttpGet(QAURL.trim());
				HttpResponse responseChannels = httpclient1.execute(httpChannel);			
				System.out.println("Ticket Response:"+responseChannels.getStatusLine());
				if(responseChannels.getStatusLine().getStatusCode() == 200){				
					System.out.println("QA Refresh URL Successful");
				}else{				
					System.out.println("QA Refresh URL failed");
				}
			}catch(Exception e){
				e.printStackTrace();			
			}
		}
		if(target.equalsIgnoreCase("FSTR_PROD")){		
			try {
				props.load(this.getClass().getResourceAsStream("/com/kaptest/workflow/action/kaptest.properties"));			
				for(int i=1;i<4;i++){
					httpclient1 = new DefaultHttpClient();
					String propkey = "kaptest_prod_refesh_url"+i;
					String PRODURL = props.getProperty(propkey.trim());
					System.out.println("URL to refresh is:"+PRODURL);
					HttpGet httpChannel = new HttpGet(PRODURL.trim());
					HttpResponse responseChannels = httpclient1.execute(httpChannel);			
					System.out.println("Ticket Response:"+responseChannels.getStatusLine());
					if(responseChannels.getStatusLine().getStatusCode() == 200){				
						System.out.println("Prod Refresh URL Successful");
					}else{				
						System.out.println("Prod Refresh URL failed:"+PRODURL);
					}					
				}
			}catch(Exception e){
				e.printStackTrace();			
			}
		}
	}
	public void applyAspectsAfterPublish(String target, Set<NodeRef> nodesInTree,String wfProcessId){		
		if(target.equalsIgnoreCase("FSTR_PROD")){
			String message = cancelCheckoutNodes(nodesInTree);
			System.out.println("Status message from cancelCheckout:"+message);
			for(NodeRef nodeRef : nodesInTree){				
				//DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");				
				Date date = new Date();	
				GregorianCalendar cal = new GregorianCalendar();
				cal.setTime(date);			    
            	if(!nodeService.hasAspect(nodeRef, KaptestModel.ASPECT_IS_PUBLISHED)){								
					Map<QName,Serializable> propMap = new HashMap<QName,Serializable>();
					Version version =  versionService.getCurrentVersion(nodeRef);
					if(version!=null){
						propMap.put(KaptestModel.PROPERTY_KTP_VERSION, version.getVersionLabel());
						propMap.put(KaptestModel.PROPERTY_KTP_WORKFLOW_ID, wfProcessId);
						propMap.put(KaptestModel.PROPERTY_KTP_PUBLISHED_DATE, cal);
						nodeService.addAspect(nodeRef, KaptestModel.ASPECT_IS_PUBLISHED, propMap);						
					}else{						
						logger.debug("Version is null for:"+nodeRef);
					}
				}else{
					Map<QName,Serializable> propMap = new HashMap<QName,Serializable>();
					propMap.put(KaptestModel.PROPERTY_KTP_VERSION, versionService.getCurrentVersion(nodeRef).getVersionLabel());
					propMap.put(KaptestModel.PROPERTY_KTP_WORKFLOW_ID, wfProcessId);					
					propMap.put(KaptestModel.PROPERTY_KTP_PUBLISHED_DATE, cal);
					nodeService.addProperties(nodeRef, propMap);
				}           	
			}
		}else if(target.equalsIgnoreCase("FSTR_QA")){
			int intSetSize = nodesInTree.size();
			FileFolderService fileFolderService = serviceRegistry.getFileFolderService();										
			for(NodeRef nodeRef : nodesInTree){				
				if(nodeService.hasAspect(nodeRef, KaptestModel.ASPECT_IS_EDITED)){					
					nodeService.removeAspect(nodeRef, KaptestModel.ASPECT_IS_EDITED);					
				}
				if(intSetSize < 51){
					System.out.println("Checkout Documents");
	            	if(!fileFolderService.getFileInfo(nodeRef).isFolder()){
						if(!checkOutCheckInService.isCheckedOut(nodeRef)){
							checkOutCheckInService.checkout(nodeRef);
						}
	            	}
				}
			}
		}
	}

}
