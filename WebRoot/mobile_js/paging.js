/**
 * 分页工具
 * 时间:2015-06-29
 */

var PagingTool = {
	'removePagingTags': function() {
		$('div#pagenation').remove();
	},
	
	'paging': function() {

		var $totalRows = $('#dataContainer > li');
		var that = this;
		that.totalRowsCount = $totalRows.toArray().length;
		that.pageCount = Math.ceil(that.totalRowsCount / that.countPerPage);
		that.currentPageNumber = 1;

		var showRowsToCurrentPage = function() {
			$totalRows.hide();
			$totalRows.each(function(index, el) {

				var upRangePage = that.currentPageNumber * that.countPerPage;
				var lowRangePage = upRangePage - that.countPerPage + 1;
				var rowNumber = index + 1;
				if (rowNumber >= lowRangePage && rowNumber <= upRangePage) {
					$(this).show();
				}
			});
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
				return;
			}
			that.currentPageNumber--;
			showRowsToCurrentPage();
			showCurrentAndTotalPageNumber();
		});
		//分页功能中的下一页操作
		$('li#next').bind('click', function(event) {
			if (that.currentPageNumber == that.pageCount) {
				return;
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