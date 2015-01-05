<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8" errorPage="error.jsp"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions"%>

<%@ page import="java.util.List"%>
<%@ page import="host.Host,collect.*"%>

<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>${host.buss}信息采集页</title>

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
			<h1>
				${host.buss}的基本信息 <small>IP:${param.ip}</small>
			</h1>
		</div>
		<div class="row">
			<div class="col-sm-12">
				<h2 class="text-center">服务器的基本信息</h2>
				<div class="row">

					<div class="col-sm-12">
						<table class="table table-striped">

							<tbody>
								<tr>
									<td>主机类型:${host.detail.hostType}</td>
									<td>操作系统:${host.detail.os}</td>
									<td>服务器IP:${host.ip}</td>
									<td></td>
								</tr>
								<tr>

									<td>主机名:${host.detail.hostName }</td>
									<td colspan="3">主机操作系统版本:${host.detail['osVersion'] }</td>


								</tr>
								<tr>

									<td colspan="2">是否双机:${host.detail.isCluster }</td>

									<td colspan="2">双机虚地址:${host.detail.clusterServiceIP}</td>

								</tr>
								<tr>

									<td>是否负载均衡</td>
									<td>负载均衡虚地址:</td>
									<td></td>
									<td></td>
									<td></td>
								</tr>
								<tr>

									<td>内存大小:${host.detail.memSize }</td>
									<td>CPU个数:${host.detail.CPUNumber}</td>
									<td>CPU主频:${host.detail.CPUClockSpeed }</td>
									<td>CPU核数:${host.detail.logicalCPUNumber }</td>
								</tr>
							</tbody>
						</table>
						<!-- 内存等表格 -->
					</div>

				</div>
				<!-- 服务器 类型版本信息    行结束 -->

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

								<%-- 由scriptlet改为jstl --%>
								<c:forEach var="card" items="${host.detail.cardList }"
									varStatus="loopIndex">
									<tr>
										<td>${loopIndex.count }</td>
										<td>${card.cardName }</td>
										<td>${card.ifType }</td>
									</tr>
								</c:forEach>

							</tbody>
						</table>
					</div>
					<!-- col-sm-4 -->
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

								<c:forEach var="fileSystem" items="${host.detail.fsList }"
									varStatus="loopIndex">
									<tr>
										<td>${loopIndex.count }</td>
										<td>${fileSystem.mountOn }</td>
										<td>${fileSystem.blocks }</td>
										<td>${fileSystem.used }</td>

									</tr>
								</c:forEach>


							</tbody>
						</table>
					</div>

				</div>
				<!--服务器表格       行结束 -->



			</div>
		</div>
		<!-- 服务器基本信息    行结束 -->

		<c:forEach var="db" items="${host.dList }" varStatus="loop1">
			<div class="row">
				<div class="col-sm-12">
					<h2 class="text-center">
						<%-- ${db.type} --%>
						数据库的基本信息
					</h2>
					<div class="row">
						<div class="col-sm-12">

							<table class="table  table-striped table-hover">

								<tbody>
									<tr>
										<td>数据库类型:${db.type }</td>
										<td>数据库版本号:${db.version }</td>
										<td>服务器IP:${db.ip }</td>
										<td>数据库名称:${db.dbName }</td>
									</tr>
									<tr>
										<td colspan="4">数据库部署路径:${db.deploymentDir }</td>
									</tr>
								</tbody>
							</table>
						</div>
					</div>
					<!-- 数据库 版本等信息行结束 -->

					<div class="row">
						<div class="col-sm-8">

							<table class="table  table-bordered table-hover">
								<caption>数据库数据文件列表</caption>
								<thead>
									<tr>
										<th>序号</th>
										<th>文件路径</th>
										<th>文件大小(MB)</th>
									</tr>
								</thead>
								<tbody>

									<c:forEach var="dataFile" items="${db.dfList }"
										varStatus="loop2">
										<tr>
											<td>${loop2.count }</td>
											<td>${dataFile.fileName }</td>
											<td>${dataFile.fileSize }</td>
										</tr>
									</c:forEach>

								</tbody>
							</table>
						</div>
					</div>
					<!-- 数据库数据文件列表行结束 -->
				</div>
			</div>
			<!--数据库 行结束 -->
		</c:forEach>

		<c:forEach var="mw" items="${host.mList }" varStatus="">
			<div class="row">
				<div class="col-sm-12">

					<h2 class="text-center">中间件详情</h2>
					<div class="row">
						<div class="col-sm-12">
							<table class="table table-striped table-hover">
								<tbody>
									<tr>
										<td>中间件类型:${mw.type}</td>
										<td>中间件版本号:${mw.version}</td>
										<td>服务器IP:${mw.ip}</td>

									</tr>
									<tr>
										<td>中间件部署路径:${mw.deploymentDir }</td>
										<td>中间件应用部署路径:</td>
										<td>JDK版本:${mw.jdkVersion}</td>

									</tr>


								</tbody>
							</table>
						</div>
					</div>
					<!-- 中间件版本等      行结束 -->
					<div class="row">
						<div class="col-sm-8">

							<table class="table table-bordered table-hover">
								<caption>应用列表</caption>
								<thead>
									<tr>
										<th>序号</th>
										<th>应用名称</th>
										<th>部署路径</th>
										<th>端口</th>
										<th>虚地址</th>
										<th>虚端口</th>

									</tr>
								</thead>
								<tbody>
									<c:choose>
										<c:when test="${fn:length(mw.appList) > 0 }">
											<c:forEach var="app" items="${mw.appList }" varStatus="loop2">
												<tr>
													<td>${loop2.count }</td>
													<td>${app.appName }</td>
													<td>${app.dir }</td>
													<td>${app.port }</td>
													<td>${app.serviceIp }</td>
													<td>${app.servicePort }</td>
												</tr>
											</c:forEach>
										</c:when>
										<c:otherwise>
											<tr>
												<td colspan="3">无</td>
											</tr>
										</c:otherwise>
									</c:choose>
								</tbody>
							</table>
						</div>
					</div>
					<!-- 中间件 应用列表     行结束-->
				</div>
			</div>
			<!-- 应用 row结束 -->
		</c:forEach>

	</div>
	<!-- jQuery (necessary for Bootstrap's JavaScript plugins) -->
	<script src="jquery/jquery-1.11.1.js"></script>
	<!-- Include all compiled plugins (below), or include individual files as needed -->
	<script src="bootstrap/js/bootstrap.min.js"></script>
	<script src="js/json/json_parse.js"></script>
</body>
</html>