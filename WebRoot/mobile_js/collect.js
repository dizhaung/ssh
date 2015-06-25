 //实时显示采集进度
 function show(msg) {

     msg = JSON.parse(msg);

     var progressBar = $("#progressBar");
     var progressBarLabel = progressBar;
     progressBarLabel.text(msg.msg + msg.nowNum + "/" + msg.maxNum + "       " + msg.ip);
     msg.maxNum == 0 ? progressBar.css('width', '100%') : progressBar.css('width', msg.nowNum / msg.maxNum * 100 + '%')
 }
 $(document).ready(function() {

    //表示允许使用推送技术
     dwr.engine.setActiveReverseAjax(true);
     
     $('#collect').click(function() {

         var that = this;
         //将上一次采集到的主机表格清空
         var $dataContainer = $('#dataContainer');
         $dataContainer.empty();

         $(that).addClass('ui-state-disabled');//采集过程中不可重复点击
         $.mobile.loading('show');
         $.ajax({
             url: '/ssh/collect',
             success: function(data, textStatus) {

                 /**
                 * 主机无法采集的情况下，detail为null
                 各要显示的主机详情项单元格显示"未知"
                 */
                 function detailDot(pName, detail) {
                     return detail ? detail[pName] : "未知";
                 }
                 /**
                  *   计算应用的数量
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
                     $(data).each(function(index, host) {
                         var $hostItem = $(['<li data-theme="a">',
                             '<a href="overview.html" data-transition="slide"><img src="image/chrome.png" >',
                             '<h2>',
                             host.buss,
                             '</h2>',
                             '<p>IP',
                             host.ip,
                             '</p>',
                             '<span class="ui-li-count">应用',
                             appCountOf(host.mList),
                             '</span>',
                             '</a>',
                             '<a href="./overview.html">Some Text</a>',
                             '</li>'
                         ].join(''));
                         $dataContainer.append($hostItem);
                         /*$('<td></td>').text(index + 1).appendTo($tr); //序号

                     $('<td></td>').text(el.buss).appendTo($tr); //业务名称
                     $('<td></td>').text(detailDot("hostName", el.detail)).appendTo($tr); //主机名
                     $('<td></td>').text((el.dList.length > 0 ? '数据库服务器' : '') + ' ' + (el.mList.length > 0 ? '应用服务器' : '')).appendTo($tr); //类型（数据库服务器or应用服务器）

                     $('<td></td>').text(el.ip).appendTo($tr); //IP
                     $('<td></td>').text(el.os ? el.os : "未知").appendTo($tr); //操作系统

                     $('<td></td>').text(detailDot("isLoadBalanced", el.detail)).appendTo($tr); //是否负载均衡
                     $('<td></td>').text(detailDot("isCluster", el.detail)).appendTo($tr); //是否双机
                     $('<td></td>').text(appCountOf(el.mList)).appendTo($tr); //应用系统个数
                     $('<td></td>').text(allDbTypeAndVersionOf(el.dList)).appendTo($tr); //各个数据库名称及版本


                     $('<td></td>').html('<a target="_blank" href="hostdetail?ip=' + el.ip + '">详细信息</a>').appendTo($tr); //详细信息
*/
                     });
                     $dataContainer.listview('refresh');
                     PagingTool.paging();
                 } else { //没有服务器被采集  不导出文件  后续可添加提示或其他操作

                 }


             },
             jsonpCallback: "callBackFun",
             dataType: "json"
         }).error(function(xhr, msg, e) {
             /* Act on the event */
             //$('#errorPromptView').modal('show').find('div.alert').text('网络连接失败,错误原因:' + msg);

         }).complete(function() {
             $(that).removeClass('ui-state-disabled');
             $.mobile.loading('hide');
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