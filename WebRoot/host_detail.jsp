<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
    <%@ page import="java.util.List" %>
    <%@ page import="host.Host" %>
    <%
    String ip = request.getParameter("ip");
    if(ip==null) return;
    List<Host> list =(List<Host>)request.getSession().getServletContext().getAttribute("host");
    Host host = null;
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
              
                  <td>是否双机:</td>
                  
                  <td></td>
                  <td>双机虚地址:</td>
                  <td>
                   </td>
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
     </div>
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
          
     </div>
    </div><!-- 行结束 -->
    <!-- jQuery (necessary for Bootstrap's JavaScript plugins) -->
    <script src="jquery/jquery-1.11.1.js"></script>
    <!-- Include all compiled plugins (below), or include individual files as needed -->
    <script src="bootstrap/js/bootstrap.min.js"></script>
    <script src="js/json/json_parse.js"></script>
  </body>
</html>