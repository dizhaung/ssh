$(document).ready(function() {
	var sortedColumn = null;	
	$('table.sortable thead tr th:gt(0)').attr('title', '点击排序')
	.append('<span class="caret"></span>').each(function(index, el) {


		$(this).css({
				'cursor': 'pointer'
			}).hover(function() {
				/* Stuff to do when the mouse enters the element */
				$(this).addClass('success');
			}, function() {
				$(this).removeClass('success');
			})
			.click(function(event) {

				var comparableColumnArray = $('table.sortable tbody tr').find('td:eq(' + (index + 1) + ')').toArray();

				//按照列的值先对这一列的单元格进行排序
				comparableColumnArray.sort(
					function(a, b) {
						var textA = $(a).text().toUpperCase();
						var textB = $(b).text().toUpperCase();

						if (textA < textB) return -1;
						else if (textA > textB) return 1;
						else return 0;

					});
				//去掉前一次排序的列的高亮
				if (sortedColumn) {
					for (var i = 0; i < sortedColumn.length; i++) {
						$(sortedColumn[i]).removeClass('success');
					}
				};
				//将排好序的单元格所在的行调换顺序
				for (var i = 0; i < comparableColumnArray.length; i++) {
					$(comparableColumnArray[i]).addClass('success').parent().appendTo('table.sortable tbody');
				}
				sortedColumn = comparableColumnArray;
			});
	});


});