/**
 * 遮罩和弹出视窗
 */
var AlphaLayerTool = {};

AlphaLayerTool.showBy=function(o){  
 
/* 	$("#overlay").height($(window.top).height());
	$("#overlay").width($(window.top).width()); */
	// fadeTo第一个参数为速度，第二个为透明度
	// 多重方式控制透明度，保证兼容性，但也带来修改麻烦的问题
	var popWin = new PopWindowTool(o);
	var alpha = $("#alphalayer");
	this.popWin = popWin;
	this.alpha = alpha;
	alpha.fadeTo(200, 0.5).click(
		AlphaLayerTool.hideOverlay
	);
	
	popWin.show();
};
/* 隐藏覆盖层 */
AlphaLayerTool.hideOverlay = function(){
	AlphaLayerTool.popWin.hidden();
	AlphaLayerTool.alpha.fadeOut(200);
};

var PopWindowTool = function(o){
	this.winProperties = o;
	this.win;
};
PopWindowTool.prototype.open = function(){
	this.win.find('iframe').attr('src',this.winProperties.url);
};
PopWindowTool.prototype.hidden = function(){
	  this.win.css({display:'none'});
};
/* 定位到页面中心 */
PopWindowTool.prototype.show = function() {
	//浏览器视口的高度
	function windowHeight() {
	    var de = document.documentElement;
	    return self.innerHeight || (de && de.clientHeight) || document.body.clientHeight;
	}
	//浏览器视口的宽度
	function windowWidth() {
	    var de = document.documentElement;
	    return self.innerWidth || (de && de.clientWidth) || document.body.clientWidth;
	}
	/* 浏览器垂直滚动位置 */
	function scrollY() {
	    var de = document.documentElement;
	    return self.pageYOffset || (de && de.scrollTop) || document.body.scrollTop;
	}
	/* 浏览器水平滚动位置 */
	function scrollX() {
	    var de = document.documentElement;
	    return self.pageXOffset || (de && de.scrollLeft) || document.body.scrollLeft;
	}
    var w = parseInt(this.winProperties.width);
    var h = parseInt(this.winProperties.height);
    
    var t = scrollY() + (windowHeight()/2) - (h/2);
    if(t < 0) t = 0;
    
    var l = scrollX() + (windowWidth()/2) - (w/2);
    if(l < 0) l = 0;
   	this.win =  $(this.winProperties.id);
    this.win.css({display:'block',left: l+'px', top: t+'px',width:w+'px',height:h+'px'});
 // this.open();
	return this;
};