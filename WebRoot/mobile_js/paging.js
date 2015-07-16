/**
 * 分页工具
 * 时间:2015-06-29
 */

var PagingTool = {
	'removePagingTags': function() {
		$('div#pagenation').remove();
	},
	/**
	 * 添加分页指示器到主页面
	 * @return {[type]} [description]
	 */
	'addPagingTags':function(){
		var $pagingToolbar = $('<div  data-role="footer"  id="pagenation"  data-position="fixed">'
          
          +'<h5 id="pageSign">0/0</h5>'
          +'<div data-role="navbar" data-iconpos="top">'
            +'<ul>'
              +'<li id="previous">'
                +'<a href="#page1" data-transition="fade"  data-icon="arrow-l" data-theme="b">上一页</a>'
              +'</li>'
              +'<li id="next">'
                +'<a href="#page1" data-transition="fade"  data-icon="arrow-r" data-theme="b">下一页</a>'
              +'</li>'
            +'</ul>'
          +'</div>'
        +'</div>').appendTo($('div[data-role="page"]').filter('.withPagingWidget'));

       
        $pagingToolbar.toolbar({
        	defaults:true
        });

        $('<div data-role="popup" id="toFirstTip"><p>已经是第一页了</p></div>').popup({
        	defaults:true,
        	positionTo:'window',
        	transition:'fade',
        	overlayTheme:'b'

        });
        $('<div data-role="popup" id="toLastTip"><p>已经是最后一页了</p></div>').popup({
        	defaults:true,
        	positionTo:'window',
        	transition:'fade',
        	overlayTheme:'b'

        });
    },
	'paging': function() {
		this.addPagingTags();
		var $totalRows = $('#dataContainer > li');
		var that = this;
		that.totalRowsCount = $totalRows.toArray().length;
		that.pageCount = Math.ceil(that.totalRowsCount / that.countPerPage);
		that.currentPageNumber = 1;

		var showRowsToCurrentPage = function() {

			$totalRows.hide();
			var upRange = (that.currentPageNumber - 1) * that.countPerPage;
			var lowRange = that.currentPageNumber * that.countPerPage;
			$totalRows.slice(upRange,lowRange).show();
		};
		var showCurrentAndTotalPageNumber = function(){
			var $pageSign = $('#pageSign');
			$pageSign.html(that.currentPageNumber+'/'+that.pageCount);
		};
		showRowsToCurrentPage();
		showCurrentAndTotalPageNumber();
		//分页功能中的上一页操作
		$('li#previous').bind('click', function(event) {
			if (that.currentPageNumber == 1) {

				$('#toFirstTip').popup('open');
     		   return false;
			}
			that.currentPageNumber--;
			showRowsToCurrentPage();
			showCurrentAndTotalPageNumber();
		});
		//分页功能中的下一页操作
		$('li#next').bind('click', function(event) {
			if (that.currentPageNumber == that.pageCount) {
				$('#toLastTip').popup('open');

				return false;
			}
			that.currentPageNumber++;
			showRowsToCurrentPage();
			showCurrentAndTotalPageNumber();
		});

	},

};
PagingTool.countPerPage = 6;
PagingTool.pageCount = 0;
PagingTool.currentPageNumber = 0;
PagingTool.totalRowsCount = 0;
PagingTool.hostDataArray = []; 