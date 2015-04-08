var PagingTool = (function() {
	return {
		'removePagingTags': function() {
			$('div#paging').remove();
		},
		'addPagingTags': function() {
			$('div#collectresult').after([
				'<div id="paging" class="row">',
				'<nav>',
				'<ul class ="pager">',
				'<li class="previous"><a href ="####">上一页</a></li>',
				'<li class="next"><a href ="####">下一页</a></li>',
				'</ul>',
				'</nav>',
				'</div>',
			].join(''));
		},
		'paging': function() {

			var $totalRows = $('table.sortable tbody tr');
			var numberPerPage = 12;
			var totalNumber = $('table.sortable tbody tr').toArray().length;
			var pageNumber = Math.ceil(totalNumber / numberPerPage);
			var currentPage = 1;

			var showRowsToCurrentPage = function() {
				$totalRows.hide();
				$totalRows.each(function(index, el) {

					var upRangePage = currentPage * numberPerPage
					var lowRangePage = upRangePage - numberPerPage + 1;
					var rowNumber = index + 1;
					if (rowNumber >= lowRangePage && rowNumber <= upRangePage) {
						$(this).show();
					}
				});
			};
			showRowsToCurrentPage();
			this.addPagingTags();
			//分页功能中的上一页操作
			$('li.previous').bind('click', function(event) {
				if (currentPage == 1) {
					return;
				}
				currentPage--;
				showRowsToCurrentPage();
			});
			//分页功能中的下一页操作
			$('li.next').bind('click', function(event) {
				if (currentPage == pageNumber) {
					return;
				}
				currentPage++;
				showRowsToCurrentPage();
			});

		}

	};

})();