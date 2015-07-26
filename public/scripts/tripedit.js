angular.module('tripedit', [ 'ui.bootstrap' ]);
angular.module('tripedit').controller('TripEditCtrl',
		[ '$http', '$scope', function($http, $scope) {
			$scope.trip = initialTripEditModel;
			$scope.removeActivity = function(day, act) {
				day.activities = day.activities.filter(function(act1) {
					return act.order != act1.order;
				});
				reorderActivities(day);
			};

			$scope.addVisit = function(day) {
				var act = addActivity(day);
				act.type = 'visit';
				act.poiRef = null;
			};
			$scope.addMove = function(day) {
				var act = addActivity(day);
				act.type = 'transport';
				act.fromCityRef = null;
				act.toCityRef = null;
				act.modalityRef = {
					id : 0,
					name : 'by car'
				};
			};
			$scope.addFree = function(day) {
				var act = addActivity(day);
				act.type = 'free';
			};
			$scope.addDay = function(trip) {
				var num = trip.days.length + 1;
				var day = {
					num : num,
					description : '',
					activities : []
				};
				trip.days.push(day);
			};
			$scope.removeDay = function(trip, day) {
				trip.tripDays = trip.days.filter(function(day1) {
					return day1.num != day.num;
				});
				reorderDays(trip);
			};
			$scope.saveTrip = function() {
				$http.post('/test', {
					'trip' : $scope.trip
				});
			};
			$scope.getCity = function(val) {
				return $http.get('/search/city/byName/' + val).then(function(response) {
					return response.data;
				});
			};

		} ]);

function addActivity(day) {
	var order = day.activities.length + 1;
	var act = {
		order : order,
		description : '',
		length : 1
	};
	day.activities.push(act);
	return act;
}

function reorderActivities(day) {
	$.each(day.activities, function(pos, act) {
		act.order = pos + 1;
	});
}

function reorderDays(trip) {
	$.each(trip.days, function(pos, day) {
		day.num = pos + 1;
	});
}
