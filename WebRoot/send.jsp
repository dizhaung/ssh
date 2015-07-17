<%@ page language="java" contentType="text/html; charset=utf-8"  
    pageEncoding="utf-8" import="collect.dwr.ProgressIndicator"%> 
    <%!
    	int count = 1;
    %> 
<%
  	//DwrPageContext.run();
	String callback = request.getParameter("callback");
	String cb= request.getParameter("cb");
	System.out.println(callback+"   "+cb);
	%>
	<%="request"+count++ %> 