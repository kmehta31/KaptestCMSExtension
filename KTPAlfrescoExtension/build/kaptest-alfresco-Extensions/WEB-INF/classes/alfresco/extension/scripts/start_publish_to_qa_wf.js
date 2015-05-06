var wfdef = workflow.getDefinitionByName("jbpm$ktpwf:publish");
if(wfdef){
    var wfparams = new Array();
	var currentUser = person.properties.userName; 
	logger.log("Debug1");
    wfparams["bpm:description"] = "Review items to QA";
    wfparams["bpm:workflowDescription"] = "Publish Content workflow started";
    
    var wfpackage = workflow.createPackage();    
	var query = "TYPE:\"ktp:kaptestContent\" AND ASPECT:\"ktp:isEdited\" AND @cm\\:modifier:\""+currentUser+"\"";	
	var nodes = search.luceneSearch("workspace://SpacesStore",query,"@cm:modified",true);
	logger.log("Total items found:"+nodes.length);
    for (var i=0; i<nodes.length; i++){		
		logger.log("NodeRef:"+nodes[i].nodeRef);
		var child = nodes[i];
		wfpackage.addNode(child);				
	}
	if(nodes.length > 0){
		logger.log("Debug4: add items to workflow def");
		wfdef.startWorkflow(wfpackage,wfparams);
	}
}