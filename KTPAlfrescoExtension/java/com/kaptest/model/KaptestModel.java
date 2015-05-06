package com.kaptest.model;

import org.alfresco.service.namespace.QName;

public interface KaptestModel {
	//NameSpaces
	public static final String 	KAPTEST_NAMESPACE  = "http:/www.kaptest.com/model/content/1.0";
	
	//Types	
	public static final QName 	TYPE_KAPTEST_CONTENT = QName.createQName(KAPTEST_NAMESPACE, "kaptestContent");
	public static final QName	PROPERTY_KTP_ID = QName.createQName(KAPTEST_NAMESPACE, "id");
	
	//Marker Aspects
	public static final QName 	ASPECT_IS_READY_TO_QA = QName.createQName(KAPTEST_NAMESPACE, "isReadyToQA");
	public static final QName 	ASPECT_IS_READY_TO_PUBLISH = QName.createQName(KAPTEST_NAMESPACE, "isReadyToPublish");
	public static final QName 	ASPECT_IS_EDITED = QName.createQName(KAPTEST_NAMESPACE, "isEdited");
	
	//Aspect isPublished
	public static final QName 	ASPECT_IS_PUBLISHED = QName.createQName(KAPTEST_NAMESPACE, "isPublished");
	public static final QName 	PROPERTY_KTP_WORKFLOW_ID = QName.createQName(KAPTEST_NAMESPACE, "workflowID");
	public static final QName 	PROPERTY_KTP_VERSION = QName.createQName(KAPTEST_NAMESPACE, "version");
	public static final QName 	PROPERTY_KTP_PUBLISHED_DATE = QName.createQName(KAPTEST_NAMESPACE, "publishedDate");
	public static final String  WORKFLOW_KTP_PUBLISH_DEFINITION_NAME = "jbpm$ktpwf:publish";	
}
