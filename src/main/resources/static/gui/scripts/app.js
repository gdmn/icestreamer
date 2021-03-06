(function ($) {
	// `Backbone.sync`: Overrides persistence storage with dummy function. This enables use of `Model.destroy()` without raising an error.
	Backbone.sync = function (method, model, success, error) {
		success();
	};

	var Item = Backbone.Model.extend({
		defaults: {
			name: '',
			hashcode: '',
			tags: {}
		}
	});

	var List = Backbone.Collection.extend({
		data: '',
		page: 0,
		pageSize: 10,
		alreadyRendered: 0,
		exhausted: true,
		model: Item,
		initialize: function () {
			_.bindAll(this, 'fetchMoreData', 'setData');
			this.on('reset', this.resetEvent, this);
			//this.trigger('reset');
		},
		resetEvent: function () {
			this.data = '';
			this.page = 0;
			this.alreadyRendered = 0;
			this.exhausted = true;
		},
		setData: function (json) {
			this.data = json;
			this.page = 0;
			this.alreadyRendered = 0;
			this.exhausted = false;
		},
		fetchMoreData: function () {
			if (this.exhausted) {
				return false;
			}
			var that = this;

			$.each(_.slice(this.data.data, this.alreadyRendered, this.alreadyRendered + this.pageSize), function (index, i) {
				var item = new Item();
				item.set({
					name: i.name,
					hashcode: i.hashcode,
					tags: i
				});
				that.add(item);
			});
			this.alreadyRendered += this.pageSize;
			if (this.alreadyRendered >= this.data.total) {
				this.exhausted = true;
			}
			return true;
		},
	});

	var ItemView = Backbone.View.extend({
		tagName: 'div',
		template: _.template($('#item-template').html()),
		initialize: function () {
			_.bindAll(this, 'render', 'unrender'); // every function that uses 'this' as the current object should be in here

			this.model.bind('change', this.render);
			this.model.bind('remove', this.unrender);
		},
		render: function () {
			this.$el.html(this.template(this.model.attributes));
			return this;
		},
		unrender: function () {
			$(this.el).remove();
		},
		remove: function () {
			this.model.destroy();
		}
	});

	var FoundCounterModel = Backbone.Model.extend({
		defaults: {
			count: 0,
			queryTime: 0,
			visible: false,
			searching: false
		}
	});

	var FoundCounterView = Backbone.View.extend({
		tagName: 'span', // name of tag to be created
		el: $('#found-placeholder'), // el attaches to existing element
		template: _.template($('#found-template').html()),
		// `initialize()` now binds model change/removal to the corresponding handlers below.
		initialize: function () {
			_.bindAll(this, 'render', 'unrender'); // every function that uses 'this' as the current object should be in here

			this.model.bind('change', this.render);
			this.model.bind('remove', this.unrender);
		},
		// `render()` now includes two extra `span`s corresponding to the actions swap and delete.
		render: function () {
			this.$el.html(this.template(this.model.attributes));
			//$(this.el).html(this.foundTemplate({count: this.model.get('count')}));
			return this; // for chainable calls, like .render().el
		},
		// `unrender()`: Makes Model remove itself from the DOM.
		unrender: function () {
			$(this.el).remove();
		},
		// `remove()`: We use the method `destroy()` to remove a model from its collection. Normally this would also delete the record from its persistent storage, but we have overridden that (see above).
		remove: function () {
			this.model.destroy();
		}
	});

	var StatusModel = Backbone.Model.extend({
		defaults: {
			status: ''
		},
		initialize: function () {
			var that = this;
			$.ajax({
				type: 'GET',
				url: '../status',
				dataType: 'text',
				timeout: 3000,
				context: $('div.status'),
				success: function (data) {
					that.set('status', data);
				},
			});
		}
	});

	var StatusView = Backbone.View.extend({
		tagName: 'span',
		el: $('#status-placeholder'),
		template: _.template($('#status-template').html()),
		initialize: function () {
			_.bindAll(this, 'render', 'unrender');
			this.model.bind('change', this.render);
			this.model.bind('remove', this.unrender);
		},
		render: function () {
			this.$el.html(this.template(this.model.attributes));
			return this;
		},
		unrender: function () {
			$(this.el).remove();
		},
		remove: function () {
			this.model.destroy();
		}
	});

	// Because the new features (swap and delete) are intrinsic to each `Item`, there is no need to modify `ListView`.
	var ListView = Backbone.View.extend({
		divStatus: $('div.status'),
		divContent: $('div.content'),
		el: $('div.content'), // el attaches to existing element
		itemsListPlaceHolder: $('#items-list-placeholder'),
		template: _.template($('#list-template').html()),
		statusTemplate: _.template($('#status-template').html()),
		events: {
			'click button#filterButton': 'filterButtonClick',
			'click button#clearButton': 'clearButtonClick',
			'click button#m3uButton': 'm3uButtonClick',
			'click button#playItButton': 'playItButtonClick',
			'click button#rawButton': 'rawButtonClick',
			'keypress #filterInput': "updateOnEnter",
			'click button#renderAllButton': 'renderAllButtonClick',
		},
		initialize: function () {
			_.bindAll(this, 'render', 'appendItem', 'ajaxLoadList'); // every function that uses 'this' as the current object should be in here

			this.collection = new List();
			this.collection.bind('add', this.appendItem); // collection event binder

			this.found = new FoundCounterView({model: new FoundCounterModel()});
			this.found.render();
			this.status = new StatusView({model: new StatusModel()});
			this.status.render();

			var self = this;
			var win = $(window);
			var doc = $(document);

			var infinityScrollHandler = function (e) {
				if (!self.collection.exhausted) {
					if ((win.scrollTop() + win.height()) > (doc.height() - win.height())) {
						var result = self.collection.fetchMoreData();
						return result;
					}
				} else {
					$('button#renderAllButton').hide();
				}
				return false;
			};
			this.infinityScrollHandler = infinityScrollHandler;

			//start of scroll event for touch devices
			if (document.addEventListener) {
				document.addEventListener("touchmove", infinityScrollHandler, false);
				document.addEventListener("scroll", infinityScrollHandler, false);
			}
			window.onresize = function (event) {
				self.infinityScrollHandler();
			};

			this.render();
			this.clearButtonClick();
		},
		render: function () {
			var that = this;
			this.itemsListPlaceHolder.html(this.template());
			_(this.collection.models).each(function (item) { // in case collection is not empty
				that.appendItem(item);
			}, this);

			return this;
		},
		appendItem: function (item) {
			var itemView = new ItemView({
				model: item
			});
			$('#items-list', this.el).append(itemView.render().el);
		},
		clearButtonClick: function () {
			this.clearItems();
			var filterInput = document.getElementById('filterInput');
			filterInput.value = '';
			this.focusOnInput();
		},
		clearItems: function () {
			//_.invoke(this.collection.models, 'destroy');
			this.collection.reset();
			$('#items-list', this.el).empty();
			this.found.model.set({searching: false, visible: false});
			$('button#renderAllButton').hide();
		},
		ajaxLoadList: function (data, success2) {
			var that = this;
			that.found.model.set({searching: true, visible: true});
			$.ajax({
				type: 'GET',
				url: '../list',
				data: data,
				dataType: 'json',
				timeout: 20000,
				success: function (data) {
					success2(data);
					that.found.model.set({searching: false, visible: true});
				},
				error: function (xhr, type) {
					that.found.model.set({searching: false, visible: false});
					alert('Ajax error!');
				}
			});
		},
		m3uButtonClick: function () {
			var filterInput = $('#filterInput');
			this.focusOnInput();
			window.open('../list?format=m3u&s=' + filterInput.val());
		},
		playItButtonClick: function () {
			var filterInput = $('#filterInput');
			this.focusOnInput();
			window.open('player.html?' + $.param({m3u: '../list?format=m3u&s=' + filterInput.val()}));
		},
		rawButtonClick: function () {
			var filterInput = $('#filterInput');
			this.focusOnInput();
			window.open('../list?format=raw&s=' + filterInput.val());
		},
		renderAllButtonClick: function () {
			while (this.collection.fetchMoreData()) {
				_.noop();
			}
			$('button#renderAllButton').hide();
		},
		filterButtonClick: function () {
			var filterInput = $('#filterInput');
			this.clearItems();
			var that = this;
			this.ajaxLoadList({format: 'names', s: filterInput.val()}, function (data) {
				console.log('queryTime: '+data['icestreamer-query-time']);
				var t0 = performance.now();
				that.found.model.set({count: data.total, queryTime: data['icestreamer-query-time']});

				that.collection.setData(data);
				var t1 = performance.now();
				console.log('setData took: '+(t1-t0));
				
				t0 = performance.now();				
				while (that.infinityScrollHandler()) {
					_.noop();
				}
				t1 = performance.now();
				console.log('infinityScrollHandler took: '+(t1-t0));
				
				$('button#renderAllButton').show();
			});
			this.focusOnInput();
		},
		updateOnEnter: function (e) {
			// zepto takes care of this...
			//if (e.keyCode == 13) this.filterButtonClick();
		},
		focusOnInput: function () {
			var filterInput = document.getElementById('filterInput');
			filterInput.focus();
			filterInput.select();
		}
	});

	var listView = new ListView();
})($);
