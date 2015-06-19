var PagingTool = {
	'removePagingTags': function() {
		$('div#pagenation').remove();
	},
	
	'paging': function() {

		var $totalRows = $('#dataContainer > li');
		var that = this;
		that.totalNumber = $totalRows.toArray().length;
		that.pageNumber = Math.ceil(that.totalNumber / that.numberPerPage);
		that.currentPage = 1;

		var showRowsToCurrentPage = function() {
			$totalRows.hide();
			$totalRows.each(function(index, el) {

				var upRangePage = that.currentPage * that.numberPerPage
				var lowRangePage = upRangePage - that.numberPerPage + 1;
				var rowNumber = index + 1;
				if (rowNumber >= lowRangePage && rowNumber <= upRangePage) {
					$(this).show();
				}
			});
		};
		showRowsToCurrentPage();
		
		//分页功能中的上一页操作
		$('li#previous').bind('click', function(event) {
			if (that.currentPage == 1) {
				return;
			}
			that.currentPage--;
			showRowsToCurrentPage();
		});
		//分页功能中的下一页操作
		$('li#next').bind('click', function(event) {
			if (that.currentPage == that.pageNumber) {
				return;
			}
			that.currentPage++;
			showRowsToCurrentPage();
		});

	},

};
PagingTool.numberPerPage = 6;
PagingTool.pageNumber = 0;
PagingTool.currentPage = 0;
PagingTool.totalNumber = 0;
PagingTool.hostDataArray = []; 