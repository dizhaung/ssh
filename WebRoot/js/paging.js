var PagingTool = {
	'removePagingTags': function() {
		$('div#pagenation').remove();
	},
	'addPagingTags': function() {
		$('div#collectresult').after([
			'<div id="pagenation" class="row">',
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
		var pageNumber = Math.ceil(totalNumber / PagingTool.numberPerPage);
		var currentPage = 1;

		var showRowsToCurrentPage = function() {
			$totalRows.hide();
			$totalRows.each(function(index, el) {

				var upRangePage = currentPage * PagingTool.numberPerPage
				var lowRangePage = upRangePage - PagingTool.numberPerPage + 1;
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

	},
	'addPageNumberTags': function() {

	},
	'pagingWithPageNumber': function() {
		var $totalRows = $('table.sortable tbody tr');
		//var PagingTool.numberPerPage = 12;
		PagingTool.totalNumber = $totalRows.toArray().length;
		PagingTool.pageNumber = Math.ceil(PagingTool.totalNumber / PagingTool.numberPerPage);
		PagingTool.currentPage = 1;

		var showRowsToCurrentPage = function() {
			$totalRows.hide();
			$totalRows.each(function(index, el) {

				var upRangePage = PagingTool.currentPage * PagingTool.numberPerPage
				var lowRangePage = upRangePage - PagingTool.numberPerPage + 1;
				var rowNumber = index + 1;
				if (rowNumber >= lowRangePage && rowNumber <= upRangePage) {
					$(this).show();
				}
			});
		};
		if (PagingTool.pageNumber > 0) {
			$('<div class="row" id="pagenation">' + '<nav>' + '<ul class="pagination">'

					+ '</ul>' + '</nav>' + '</div>')
				.insertAfter('div#collectresult').find('ul')
				.each(function(index, el) {

					var $tagsContainer = $(el),
						$currentPageTag;
					//首页,点击末页标签 第一个数字标签被激活，其他的标签都失活
					$('<li><a href="#" aria-label="Previous"><span aria-hidden="true">&laquo;</span></a></li>').appendTo($tagsContainer)
						.click(function(event) {
							/* Act on the event */
							PagingTool.currentPage = 1;
							$currentPageTag.removeClass('active');
							$currentPageTag = $(this).next().addClass('active');
							showRowsToCurrentPage();
						});
					for (var i = 1; i <= PagingTool.pageNumber; i++) {

						var $pageTag = $('<li><a href="#">' + i + '</a></li>').appendTo($tagsContainer).click((function(i) {
							return function(event) {
								/* Act on the event */
								$currentPageTag.removeClass('active');
								$currentPageTag = $(this).addClass('active');
								PagingTool.currentPage = i;
								showRowsToCurrentPage();
							}
						})(i));
						if (PagingTool.currentPage == i) {
							$currentPageTag = $pageTag;
						}
					}
					//末页,点击末页标签 最后一个数字标签被激活，其他的标签都失活
					$('<li><a href="#" aria-label="Next"><span aria-hidden="true">&raquo;</span></a></li>').appendTo($tagsContainer)
						.click(function(event) {
							/* Act on the event */
							PagingTool.currentPage = PagingTool.pageNumber;
							$currentPageTag.removeClass('active');
							$currentPageTag = $(this).prev().addClass('active');
							showRowsToCurrentPage();
						});
				});

		}

	}

};
PagingTool.numberPerPage = 12;
PagingTool.pageNumber = 0;
PagingTool.currentPage = 0;
PagingTool.totalNumber = 0;