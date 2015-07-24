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
                $('#previous').click(function() {
                    $('#toFirstTip').popup('open');
                });
                $('#command').click(function() {
                    $(":mobile-pagecontainer").pagecontainer("change", "#retypeCommandDialog");
                });
                $('#finalCommandBut').click(function() {
                    $('#command').val($('#finalCommandText').val());
                    $(":mobile-pagecontainer").pagecontainer("change", "#home");
                });
                //表示允许使用推送技术
                dwr.engine.setActiveReverseAjax(true);
                dwr.engine.setNotifyServerOnPageUnload(true);
                dwr.engine.setErrorHandler(function(errorString,exception){
                    console.log(errorString+' '+exception);
                    alert('与服务器断开的了连接，无法更新进度指示条');
                });

                $('#collect').click(function() {
                    var that = this;
                    //将上一次采集到的主机表格清空
                    var $tBody = $('table tbody');
                    $tBody.html('');
                    var command = $('#command').val();
                    //检查输入命令的合法性
                    if (!CollectTool.validate(command)) {
                        $('#invalidCommandAlert').popup('open');
                        return;
                    }
                    //单击执行按钮进入采集过程，这个过程中所有按钮不能操作
                    CollectTool.disableButtons(['#collect', '#export']);
                    var url = '/ssh/collect/bat?command=' + command;

                    CommandExecutor.batCommand(command, {
                        callback: function(data) {
                            data = JSON.parse(data);
                            if (data.length > 0) { //采集之后并且有服务器被采集
                                /**
                                * 主机无法采集的情况下，detail为null
                                各要显示的主机详情项单元格显示"未知"
                                */
                                function hostDot(pName, host) {
                                    return host[pName] ? host[pName] : "未知";
                                }
                                //显示采集到的服务器列表
                                for (var i = 0; i < data.length; i++) {
                                    var tr = $('<tr></tr>');
                                    var host = data[i];
                                    tr.appendTo($tBody);
                                    $('<td></td>').text(i + 1).appendTo(tr); //序号
                                    $('<td></td>').text(hostDot('buss', host)).appendTo(tr); //业务名称
                                    $('<td></td>').text(hostDot('hostName', host)).appendTo(tr); //主机名
                                    $('<td></td>').text(hostDot('hostType', host)).appendTo(tr); //类型（数据库服务器or应用服务器）
                                    $('<td></td>').text(hostDot('ip', host)).appendTo(tr); //IP
                                    $('<td></td>').text(hostDot('os', host)).appendTo(tr); //操作系统
                                    $('<td style="white-space:pre;"></td>').text(data[i].commandResult).appendTo(tr); //命令执行的返回结果
                                }
                                $tBody.parent().table('rebuild');

                            } else { //没有服务器被采集  不导出文件  后续可添加提示或其他操作
                            }
                            //采集完毕，恢复按钮
                            CollectTool.enableButtons(['#export', '#collect']);
                        },
                        errorHandler:function(){
                            alert('与服务器断开连接');
                        }
                    });

                });

            });