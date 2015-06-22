 $(document).ready(function() {

    
     $('#collect').click(function() {

         var that = this;

         //单击执行按钮进入采集过程，这个过程中所有按钮不能操作
         CollectTool.disableButtons(['#collect', '#export']);
         $(that).button('disable');
         //将上一次采集到的主机表格清空
         var $dataContainer = $('#dataContainer');
         $dataContainer.empty();
          $.mobile.loading('show');   
         $.post('/ssh/collect', function(data, textStatus) {
              $.mobile.loading('hide');  
            console.log('data='+data+"\ntextStatus="+textStatus); 
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
                    '</li>'].join(''));
                   $dataContainer.append($hostItem);
                    
                 });
                 $dataContainer.listview('refresh');
                 PagingTool.paging();
             } else { //没有服务器被采集  不导出文件  后续可添加提示或其他操作

             }


         }, "json").error(function(xhr, msg, e) {
             /* Act on the event */
             $('#errorPromptView').modal('show').find('div.alert').text('网络连接失败,错误原因:' + msg);

         }).complete(function() {
             //采集完毕，恢复按钮
             $(that).button('enable');
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

    $(document).on("pageload", function(event, data) {
        alert("触发 pageload 事件！\nURL: " + data.url);
    });
 });