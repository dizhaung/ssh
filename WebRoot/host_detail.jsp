<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8" errorPage="error.jsp"%>
    <%@ page import="java.util.List" %>
    <%@ page import="host.Host,collect.*" %>
    <%
    String ip = request.getParameter("ip");
    if(ip==null) return;
    List<Host> list =(List<Host>)request.getSession().getServletContext().getAttribute("host");
    System.out.println("list="+list);
    Host host = null;
    //执行采集前或者执行采集过程中，查看任意一个IP主机的信息，进入错误页面提示没有主机信息
    if(list == null) {
   		throw new HostListAccessException("主机信息还未采集到");
    }
    	
    //查找是这个IP的主机
    for(Host h : list){
    	if(ip.trim().endsWith(h.getIp().trim())){
    		host = h;
    		break;
    	}
    }
    %>
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title><%=host.getBuss() %>信息采集页</title>

    <!-- Bootstrap -->
    <link href="bootstrap/css/bootstrap.min.css" rel="stylesheet">

    <!-- HTML5 shim and Respond.js for IE8 support of HTML5 elements and media queries -->
    <!-- WARNING: Respond.js doesn't work if you view the page via file:// -->
    <!--[if lt IE 9]>
      <script src="js/html5css3comp/html5shiv.min.js"></script>
      <script src="js/html5css3comp/respond/1.4.2/respond.min.js"></script>
    <![endif]-->
  </head>
  <body>
  	<div class="container">
  	
	  	<div class="page-header">
	  		<h1><%=host.getBuss() %>的基本信息 <small>IP:<%=ip %></small></h1>
		</div>
    	<div class="row">   
    		<div class="col-sm-12">
    			<h2 class="text-center">服务器的基本信息</h2>
    			<div class="row">
    			
    				<div class="col-sm-12">
    					<table class="table table-striped">
              <%
              	Host.HostDetail hostDetail = host.getDetail();
        
              %>
              <tbody>
                <tr>
                  <td>主机类型:<%=hostDetail.getHostType() %></td>
                  <td>操作系统:<%=hostDetail.getOs() %></td>
                  <td>服务器IP:<%=ip %></td>
                  <td>
                   </td>
                </tr>
                <tr>
                
                  <td>主机名:<%=hostDetail.getHostName() %></td>
                  <td colspan="3">主机操作系统版本:<%=hostDetail.getOsVersion() %></td>
                
                  
                </tr>
                <tr>
              
                  <td colspan="2">是否双机:<%=hostDetail.getIsCluster() %></td>
                   
                  <td colspan="2">双机虚地址:<%=hostDetail.getClusterServiceIP() %></td>
                  
                </tr>
                <tr>
                 
                  <td>是否负载均衡</td>
                  <td>负载均衡虚地址:</td>
                   <td>
                   </td>
                   <td>
                   </td>
                   <td>
                   </td>
                </tr>
                <tr>
                 
                  <td>内存大小:<%=hostDetail.getMemSize() %></td>
                  <td>CPU个数:<%=hostDetail.getCPUNumber() %></td>
                   <td>
                   CPU主频:<%=hostDetail.getCPUClockSpeed() %></td>
                    <td>
                    CPU核数:<%=hostDetail.getLogicalCPUNumber() %></td>
                </tr>
              </tbody>
            </table><!-- 内存等表格 -->
    				</div>
    			
    			</div><!-- 服务器 类型版本信息    行结束 -->
    			
			 <div class="row">
					     	<div class="col-sm-4">
					     
					     <table class="table table-bordered table-hover">
					      <thead>
					                <tr>
					                  <th>序号</th>
									  <th>网卡名称</th>
					                  <th>网卡类型</th>
					                  
					                </tr>
					              </thead>
					              <tbody>
					              <%
					              	int i = 1;
					              	List<Host.HostDetail.NetworkCard> cardList = hostDetail.getCardList();
					              	for(Host.HostDetail.NetworkCard card : cardList){
					              		
					              %>
					                <tr>
					                  <td><%=i++ %></td>
					                  <td><%=card.getCardName() %></td>
					                  <td><%=card.getIfType() %></td>
					                </tr>
					                
					            <%
					              	}
					            %>
					                </tbody>
					            </table>
					     </div><!-- col-sm-4 -->
					     <div class="col-sm-2"></div>
					     <div class="col-sm-6">
					     	 <table class="table table-bordered table-hover">
					      			<thead>
					                <tr class="info">
					                  <th>文件系统序号</th>
					                  <th>挂载点</th>
					                  <th>大小</th>
					                  <th>利用率</th>
					                  
					                </tr>
					              </thead>
					              <tbody>
					              <%
					              int index = 1;
					              List<Host.HostDetail.FileSystem> fsList = host.getDetail().getFsList();
					              	for(Host.HostDetail.FileSystem fs : fsList){
					              		
					              	
					              %>
					                <tr>
					                <td><%=index++ %></td>
					                  <td><%=fs.getMountOn() %></td>
					                  <td><%=fs.getBlocks() %></td>
					                  <td><%=fs.getUsed() %></td>
					                  
					                </tr>
					                <%
					              		}
					                %>
					                
					                </tbody>
					            </table>
					     </div>
          
    			 </div><!--服务器表格       行结束 -->
     
     
     
    		</div>
    	</div> <!-- 服务器基本信息    行结束 -->
	<%
		List<Host.Database> dList = host.getdList();
		if(dList != null){
		for(Host.Database db:dList){
			List<Host.Database.DataFile> dfList = db.getDfList();
		
	%>
     <div class="row">
     	<div class="col-sm-12">
     		<h2 class="text-center"><%=db.getType() %>数据库的基本信息</h2>
     		<div class="row">
     			<div class="col-sm-12">
	  			
			 	<table class="table  table-striped table-hover">
			 		<%
			 		
			 		%>
	      			<tbody>
	      			<tr>
	      			<td>数据库类型:<%=db.getType() %></td>
	      			<td>数据库版本号:<%=db.getVersion() %></td>
	      			<td>服务器IP:<%=db.getIp() %></td>
	      			<td>数据库名称:<%=db.getDbName() %></td>
	      			</tr>
	      			<tr><td colspan="4">数据库部署路径:<%=db.getDeploymentDir() %></td></tr>
	              </tbody>
	              </table>
	             </div> 
             </div> <!-- 数据库 版本等信息行结束 -->
             
             <div class="row">
             	<div class="col-sm-8">
             		
             		<table class="table  table-bordered table-hover">
             		<caption>数据库数据文件列表</caption>
             		<thead>
             			<tr>
	             			<th>序号</th>
	             			<th>文件路径</th>
	             			<th>文件大小</th>
             		    </tr>
             		</thead>
	      			<tbody>
	      				<%
	      					int j =1;
	      					if(dfList != null){
	      					for(Host.Database.DataFile dataFile:dfList){
	      						
	      				%>
		      			<tr>
			      			 <td><%=j++ %></td>
			      			 <td><%=dataFile.getFileName() %></td>
			      			 <td><%=dataFile.getFileSize() %></td>
		      			 </tr>
		      			 <%

	      					}
	      				}
		      			 %>
	              	</tbody>
	              </table>
             	</div>
             </div><!-- 数据库数据文件列表行结束 -->
     	</div>
     </div><!--数据库 行结束 -->
     
     <%   }
		}
		
		List<Host.Middleware> mList = host.getmList();
		if(mList != null){
		for(Host.Middleware mw:mList){
			
	
     %>
     <div class="row">
     	<div class="col-sm-12">
     		 
  			<h2 class="text-center">中间件详情</h2>
  			<div class="row">
  				<div class="col-sm-12">
  					<table class="table table-striped table-hover">
  						<tbody>
  						<tr>
  								<td>中间件类型:<%= mw.getType()%></td>
  								<td>中间件版本号:<%=mw.getVersion() %></td>
  								<td>服务器IP:<%=mw.getIp() %></td>
  							
  							</tr>
  							<tr>
  								<td>中间件部署路径:<%=mw.getDeploymentDir() %></td>
  								<td>中间件应用部署路径:</td>
  								<td>JDK版本:<%=mw.getJdkVersion() %></td>
  							
  							</tr>
  							
  							
  						</tbody>
  					</table>
  				</div>
  			</div><!-- 中间件版本等      行结束 -->
		 	<div class="row">
		 		<div class="col-sm-8">
		 			
		 			<table class="table table-bordered table-hover">
		 			<caption>应用列表</caption>
		 				<thead>
		 					<tr>
		 						<th>序号</th>
		 						<th>应用名称</th>
		 						<th>部署路径</th>
		 					</tr>
		 				</thead>
  						<tbody>
  						<%
  							List<Host.Middleware.App> appList = mw.getAppList();
  							if(appList.size()>0){
  								int i = 1;
  								for(Host.Middleware.App app:appList){
  						%>
  							<tr>
  								<td><%=i++ %></td>
  								<td><%=app.getAppName() %></td>
  								<td><%=app.getDir() %></td> 
  							</tr> 
  							<%
  								}
  							}else{
  							%>
  							<tr>
  								<td colspan="3">无</td>
  							</tr>
  							<%
  							}
  							%>
  						</tbody>
  					</table>
		 		</div>
		 	</div><!-- 中间件 应用列表     行结束-->
     	</div>
    </div><!-- 应用 row结束 -->
    	<%
    	 	}
		}
    	%>
    </div>
    <!-- jQuery (necessary for Bootstrap's JavaScript plugins) -->
    <script src="jquery/jquery-1.11.1.js"></script>
    <!-- Include all compiled plugins (below), or include individual files as needed -->
    <script src="bootstrap/js/bootstrap.min.js"></script>
    <script src="js/json/json_parse.js"></script>
  </body>
</html>