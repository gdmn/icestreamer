(function ($) {
	// `Backbone.sync`: Overrides persistence storage with dummy function. This enables use of `Model.destroy()` without raising an error.
	Backbone.sync = function (method, model, success, error) {
		success();
	};

	var Item = Backbone.Model.extend({
		defaults: {
			name: 'name...',
			part1: 'hello',
			part2: 'world'
		}
	});

	var List = Backbone.Collection.extend({
		model: Item
	});

	var ItemView = Backbone.View.extend({
		tagName: 'li', // name of tag to be created
		// `initialize()` now binds model change/removal to the corresponding handlers below.
		initialize: function () {
			_.bindAll(this, 'render', 'unrender'); // every function that uses 'this' as the current object should be in here

			this.model.bind('change', this.render);
			this.model.bind('remove', this.unrender);
		},
		// `render()` now includes two extra `span`s corresponding to the actions swap and delete.
		render: function () {
			//$(this.el).html('<span style="color:black;">' + this.model.get('name') + ' ' + this.model.get('part1') + '</span> &nbsp; &nbsp; <span class="swap" style="font-family:sans-serif; color:blue; cursor:pointer;">[swap]</span> <span class="delete" style="cursor:pointer; color:red; font-family:sans-serif;">[delete]</span>');
			$(this.el).html(this.model.get('name'));
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

	// Because the new features (swap and delete) are intrinsic to each `Item`, there is no need to modify `ListView`.
	var ListView = Backbone.View.extend({
		divStatus: $('div.status'),
		divContent: $('div.content'),
		el: $('div.content'), // el attaches to existing element
		template: _.template($('#main-template').html()),
		statusTemplate: _.template($('#status-template').html()),
		foundTemplate: _.template($('#found-template').html()),
		events: {
			'click button#filterButton': 'filterButtonClick',
			'click button#clearButton': 'clearButtonClick',
			'click button#m3uButton': 'm3uButtonClick',
			'click button#rawButton': 'rawButtonClick',
			'keypress #filterInput': "updateOnEnter",
		},
		initialize: function () {
			_.bindAll(this, 'render', 'appendItem'); // every function that uses 'this' as the current object should be in here

			this.collection = new List();
			this.collection.bind('add', this.appendItem); // collection event binder

			this.render();

			this.loadList();
			this.focusOnInput();
		},
		render: function () {
			var that = this;
			this.$el.html(this.template());
			_(this.collection.models).each(function (item) { // in case collection is not empty
				that.appendItem(item);
			}, this);

			$.ajax({
				type: 'GET',
				url: '../status',
				dataType: 'text',
				timeout: 3000,
				context: $('div.status'),
				success: function (data) {
					that.divStatus.html(that.statusTemplate({status: data}));
				},
			});

			return this;
		},
		addItemFromLine: function (line) {
			var item = new Item();
			item.set({
				name: line,
			});
			this.collection.add(item);
		},
		appendItem: function (item) {
			var itemView = new ItemView({
				model: item
			});
			$('ul', this.el).append(itemView.render().el);
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
			$('ul', this.el).empty();
		},
		ajaxLoad: function (data, success2) {
			$.ajax({
				type: 'GET',
				url: '../list',
				data: data,
				dataType: 'text',
				timeout: 3000,
				success: function (data) {
					success2(data);
				},
				error: function (xhr, type) {
					alert('Ajax error!')
				}
			});
		},
		m3uButtonClick: function () {
			var filterInput = $('#filterInput');
			this.focusOnInput();
			window.open('../list?format=m3u&s=' + filterInput.val());
		},
		rawButtonClick: function () {
			var filterInput = $('#filterInput');
			this.focusOnInput();
			window.open('../list?format=raw&s=' + filterInput.val());
		},
		filterButtonClick: function () {
			var filterInput = $('#filterInput');
			console.log(': %s', filterInput.val());
			this.clearItems();
			var that = this;
			this.ajaxLoad({format: 'names', s: filterInput.val()}, function (data) {
				var countLines = 0;
				$.each(data.split("\n"), function (index, item) {
					if (!_.isEmpty(item)) {
						that.addItemFromLine(item);
						countLines += 1;
					}
				});				
				$('div.found').html(that.foundTemplate({count: countLines}));
			});
			this.focusOnInput();
		},
		updateOnEnter: function (e) {
			if (e.keyCode == 13)
				this.filterButtonClick();
		},
		focusOnInput: function () {
			var filterInput = document.getElementById('filterInput');
			filterInput.focus();
			filterInput.select();
		},
		loadList: function () {
			console.log('hostname: ' + window.location.hostname);
			console.log('host: ' + window.location.host);
			console.log('href: ' + window.location.href);
		}
	});

	var listView = new ListView();
})($);
