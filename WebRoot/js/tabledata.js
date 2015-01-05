var  tHead = [{
        						'name':'业务系统',
        						'id':'buss',
        						'host-mapping-name':'buss',
        						'fn':function(host){
        							var value = host[this['host-mapping-name']];
        							return value?value:"未知";
        						}
        					}];	


/**
* 从detail 去取属性名为pName的值，若detail为null 返回NONE
*/
function detailDot(pName,detail){
	return detail?detail[pName]:"未知";
}
/**
*	计算中间件数量
*/
function appCountOf(middlewares){
	var count = 0;
	for(var i = 0,size = middlewares.length;i < size;i++){
		count += mw.appList.length;
	}
	return count;
}
/**
产生可显示的表格数据
*/
function produceTableBy(host){
	var tds = [];
	for(var i = 0,size = tHead.length;i < size;i++){
		 
	}
}