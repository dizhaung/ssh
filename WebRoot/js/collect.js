 //实时显示采集进度
 function show(msg) {
     console.log(msg);
     msg = JSON.parse(msg);

     var progressBar = $("#progressBar");
     var progressBarLabel = progressBar;
     progressBarLabel.text(msg.msg + msg.nowNum + "/" + msg.maxNum + "       " + msg.ip);
     msg.maxNum == 0 ? progressBar.css('width', '100%') : progressBar.css('width', msg.nowNum / msg.maxNum * 100 + '%')
 }
 $(document).ready(function() {

     //表示允许使用推送技术
     dwr.engine.setActiveReverseAjax(true);
     CollectTool.toggleAutoBind('#autoChkbox');
     $('#poll').click(function() {

         var that = this;

         //单击执行按钮进入采集过程，这个过程中所有按钮不能操作
         CollectTool.disableButtons(['#collect', '#export']);
         //将上一次采集到的主机表格清空
         var $tbody = $('table tbody');
         $tbody.empty();
         //去掉分页组件
         PagingTool.removePagingTags();
         $.post('/ssh/collect', function(data) {
             data = JSON.parse(data);
             /**
             * 主机无法采集的情况下，detail为null
             各要显示的主机详情项单元格显示"未知"
             */
             function detailDot(pName, detail) {
                     return detail ? detail[pName] : "未知";
                 }
                 /**
                  *   计算中间件数量
                  */
             function appCountOf(middlewares) {
                 var count = 0;
                 for (var i = 0, size = middlewares.length; i < size; i++) {
                     count += middlewares[i].appList.length;
                 }
                 return count;
             }

             function allDbTypeAndVersionOf(dList) {
                 var typeAndVersion = "";
                 if (dList.length == 0) typeAndVersion = "无";
                 for (var i = 0, size = dList.length; i < size; i++) {
                     typeAndVersion += dList[i].type + " " + dList[i].version;
                     if ((i + 1) < size) typeAndVersion += ','; //不是最后一数据库
                 }
                 return typeAndVersion;
             }
             if (data.length > 0) { //采集之后并且有服务器被采集
                 //显示采集到的服务器列表
                 for (var i = 0; i < data.length; i++) {
                     var tr = $('<tr></tr>');
                     tr.appendTo($tbody);

                     $('<td></td>').text(i + 1).appendTo(tr); //序号

                     $('<td></td>').text(data[i].buss).appendTo(tr); //业务名称
                     $('<td></td>').text(detailDot("hostName", data[i].detail)).appendTo(tr); //主机名
                     $('<td></td>').text((data[i].dList.length > 0 ? '数据库服务器' : '') + ' ' + (data[i].mList.length > 0 ? '应用服务器' : '')).appendTo(tr); //类型（数据库服务器or应用服务器）

                     $('<td></td>').text(data[i].ip).appendTo(tr); //IP
                     $('<td></td>').text(data[i].os ? data[i].os : "未知").appendTo(tr); //操作系统

                     $('<td></td>').text(detailDot("isLoadBalanced", data[i].detail)).appendTo(tr); //是否负载均衡
                     $('<td></td>').text(detailDot("isCluster", data[i].detail)).appendTo(tr); //是否双机
                     $('<td></td>').text(appCountOf(data[i].mList)).appendTo(tr); //应用系统个数
                     $('<td></td>').text(allDbTypeAndVersionOf(data[i].dList)).appendTo(tr); //各个数据库名称及版本


                     $('<td></td>').html('<a target="_blank" href="hostdetail?ip=' + data[i].ip + '">详细信息</a>').appendTo(tr); //详细信息


                 }
                 //分页显示
                 PagingTool.paging();
                 if (CollectTool.isAutoExport()) {
                     //下载文件
                     CollectTool.exportFileFrom({
                         url: '/ssh/export',
                         buttonIds: ['#collect', '#export']
                     });
                 }

             } else { //没有服务器被采集  不导出文件  后续可添加提示或其他操作

             }

             //采集完毕，恢复按钮
             CollectTool.enableButtons(['#export', '#collect']);
         });
     });

     //导出采集到的主机信息到文件
     var exportButton = $('#export').click(function() {
         //下载文件
         CollectTool.exportFileFrom({
             url: '/ssh/export',
             buttonIds: ['#collect', '#export']
         });
     });

 });