var CollectTool = (function() {

	var isAutoExport = true;
	return {

		validate: function(command) {
			var reg = /^\s*$|reboot/;
			return !(reg.test(command));
		},
		isAutoExport: function() {
			return isAutoExport;
		},
		/*
			导出并下载文件到本地
		 */
		exportFileFrom: function(url) {
			this.disableButtons(url.buttonIds); //禁用导出按钮
			var that = this;
			$.ajax({
				url: url.url,
				type: "GET",
				dataType:'text',
				async: false, //服务器端生成数据文件后再下载
				success: function(data) {
					$('#exportIframe').attr('src', data);
				},
				error: function(xhr, msg, e) {
					$('#errorPromptView').modal('show').find('div.alert').text('网络连接失败,错误原因:' + msg);
				},
				complete: function() {
					that.enableButtons(url.buttonIds);
				}
			});

		},
		'disableButtons': function(buttonIds) {
			if (buttonIds.length <= 0) return;

			for (var i = 0, size = buttonIds.length; i < size; i++) {
				$(buttonIds[i]).attr('disabled', true);
			}
		},
		'enableButtons': function(buttonIds) {
			if (buttonIds.length <= 0) return;

			for (var i = 0, size = buttonIds.length; i < size; i++) {
				$(buttonIds[i]).attr('disabled', false);
			}

		},
		/*
			切换采集完毕后是否自动导出到文件
		*/
		toggleAutoBind: function(chkboxId) {

			$(chkboxId).click(function() {
				console.log(isAutoExport);
				isAutoExport = !isAutoExport;
			});
		}
	};

})();