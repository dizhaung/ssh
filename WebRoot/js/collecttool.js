var CollectTool = (function(){
	
	var isAutoExport  =  true;
	return {
		
		validate:function(command){
			var reg = /^\s*$|reboot/;
			return !(reg.test(command));
		},
		isAutoExport:function(){
			return isAutoExport;
		},
		/*
			导出并下载文件到本地
		 */ 
		exportFileFrom:function(url){
			this.disableButtons(url.buttonIds);//禁用导出按钮
			$.get(url.url,function(data){
				 	console.log(data);
				$('#exportIframe').attr('src',data);
				
			});
		 
			this.enableButtons(url.buttonIds);
		},
		'disableButtons':function(buttonIds){
			if(buttonIds.length <= 0 ) return;
			
			for(var i = 0,size = buttonIds.length;i<size;i++){
				$(buttonIds[i]).attr('disabled',true);
			}
		},
		'enableButtons':function(buttonIds){
			if(buttonIds.length <= 0 ) return;
			
			for(var i = 0,size = buttonIds.length;i<size;i++){
				$(buttonIds[i]).attr('disabled',false);
			}
			
		},
		/*
			切换采集完毕后是否自动导出到文件
		*/
		toggleAutoBind:function(chkboxId){
			
    		$(chkboxId).click(function(){
    			console.log(isAutoExport);
    			isAutoExport = !isAutoExport;
    		});
		}
	};
	
})();