angular.module('tripedit', [ 'ui.bootstrap', 'ng-selectize', 'ui.select', 'ngSanitize', 'ng' ]);
angular.module('tripedit').controller('TripEditCtrl',
		[ '$http', '$scope', '$window', function($http, $scope, $window) {			
			$scope.trip = initialTripEditModel;
			$scope.removeActivity = function(day, act) {
				day.activities = day.activities.filter(function(act1) {
					return act.order != act1.order;
				});
				$scope.updateEverything();
			};
									
			var arraycopy = function(src,dest) {
				if(!angular.isArray(src) || !angular.isArray(dest)) return;
				dest.length = src.length;
				for(k in src) {
					dest[k] = src[k];
				}
			};
						
			var makeRefresher = function(options) {
				options = $.extend({
					extract: function(d) { return d; },
					data: null,					
					queryMin: 3,
					clearResults: function(d,setResult) {
					   setResult([],d);
					}
				},options);
				var setResult = options.setResult;
				if (angular.isArray(setResult)) {
					var r = setResult;
					setResult = function(res) {
						arraycopy(res,r);
					};
				}
				var queryMin = !angular.isNumber(options.queryMin) ? 3 : options.queryMin;
				return function(query) {
					if(angular.isDefined(query) && 
					   typeof(query) === "string" &&
					   query.length >= queryMin) {
						var url = options.makeUrl(query,options.data);
						$http.get(url).then(function(response) {
							var result = options.extract(response.data);
							setResult(result,options.data);							
						});						
					} else {
						options.clearResults(options.data,setResult);
					}
				};
			};
			
			$scope.trip._regions=[];
			$scope.trip._cities=[];
			
			$scope.refreshRegions = makeRefresher({
				makeUrl: function(s) { return '/search/region/byName/' + encodeURIComponent(s); },
				setResult: function(res) {
					$scope.trip._regions = res;
				}
			});
			
			var makeCityRefresher = function(setResult) {
				return makeRefresher({
					makeUrl: function(s) { 
						return '/search/city/byName/' + $scope.trip.regionRef.id + 
								'/' + encodeURIComponent(s); 
					},
					setResult: setResult
				});
			};
			
			$scope.mainCityRefresher = makeCityRefresher(
				function(r) { $scope.trip._cities = r; });
			
			var actCityRefresher = function(act) {
				return makeCityRefresher(function(r) { act._cities = r; });
			};
			
			var makePoiRefresher = function(act) {
				return makeRefresher({
					makeUrl: function(s) { 
						return '/search/place/byName/' + act._startCityRef.id + 
								'/' + encodeURIComponent(s); 
					},
					setResult: function(res) {
						act._pois = res;
					}
				});				
			};
			
			
			$scope.$watch('trip.regionRef.id',function(newValue,oldValue) {				
				if(newValue != oldValue) {
					$scope.trip.cityRef.id=-1;
					$scope.trip.cityRef.name="";
					$scope.updateEverything();
				}				
			});
			
			$scope.$watch('trip.cityRef.id',function(newValue,oldValue) {				
				if(newValue != oldValue) {
					$scope.updateEverything();					
				}				
			});
						
			
			$scope.addVisit = function(day) {				
				var act = $scope.addActivity(day,'visit');				
				act.poiRef = {
					id: -1,
					name: ""
				};
				act._poiRefresher = makePoiRefresher(act);
				act._pois = [];				
			};
			$scope.addMove = function(day) {
				var act = $scope.addActivity(day,'transport');				
				act.toCityRef = {
						id: -1,
						name: ""
					};
				act._cities = [];				
				act._endCityRef = act.toCityRef;
				var refresher = actCityRefresher(act);
				act._cityRefresher = function(query) { console.log('cityRefresher: ',query); refresher(query); };
				var activityExpression = "trip.days[" + (day.num - 1)+ "].activities["+(act.order - 1) + "].toCityRef.id"				 					
				console.log('activityExpression = ', activityExpression);
				$scope.$watch(activityExpression, function(oldValue,newValue) {
					console.log("watching change on activity: day.num = " + day.num + ", act.order =  " + act.order)
					if(oldValue !== newValue) {
						$scope.computeCities();
						$scope.computeMovements();
					};
				});								
				act.modalityRef = {
					id : 'car',
					name : 'Car'
				};
			};			
			$scope.addFree = function(day) {
				$scope.addActivity(day,'free');				
			};
			$scope.addDay = function(trip) {
				var num = trip.days.length + 1;
				var cityRef = trip.cityRef;
				var lastDay = null;
				if(num > 1) {
					lastDay = trip.days[num-2];
					cityRef = lastDay._endCityRef;
				}
				var day = {
					_startCityRef: cityRef,
					_endCityRef: cityRef,
					_canMoveUp : false,
					_canMoveDown : false,
					num : num,
					description : '',
					activities : []
				};
				if(lastDay != null && 
					lastDay._startCityRef.id == day._startCityRef.id &&
					lastDay._endCityRef.id == day._endCityRef.id) {
					
					lastDay._canMoveDown = true;
					day._canMoveUp = true;											
				}
				trip.days.push(day);
			};
			$scope.removeDay = function(trip, day) {
				$scope.trip.days.splice(day.num-1,1);				
				$scope.updateEverything();
			};
			$scope.saveTrip = function() {
				var service = jsRoutes.controllers.ApplicationController.saveTrip();			
				var trip = cleanUp($scope.trip);
				var tripJson = JSON.stringify(trip);
				$http.put(service.url, tripJson).then(function(result) {					
					console.log('put finished');
					console.dir(result);
					var url = jsRoutes.controllers.ApplicationController.index().absoluteURL();
					console.log('url = ' + url);
					$window.location.href = url;					
				});
			};
			
			$scope.computeMovements = function() {
				console.log('Computing movements');
				var lastDay = null;
				$.each($scope.trip.days,function(dayKey,day) {
					day._canMoveUp = false;
					day._canMoveDown = false;
					if(lastDay !== null && 
					   lastDay._startCityRef.id == day._startCityRef.id &&
					   lastDay._endCityRef.id == day._endCityRef.id) {
						console.log('enabling movement for dayKey: ',dayKey);
						lastDay._canMoveDown = true;
						day._canMoveUp = true;
					} 
					console.log('lastDay = ', lastDay, ', day = ', day);
					lastDay = day;
					
					var lastTypeCanMove = false;
					var lastActivity = null;
					$.each(day.activities, function(activityKey,activity) {
						var activity = day.activities[activityKey];
						activity._canMoveUp = false;
						activity._canMoveDown = false;
						if (activity.type === "visit" || activity.type === "free") {
						    activity._canMoveUp = lastTypeCanMove;													
						    lastTypeCanMove = true;
						    if (lastActivity !== null) {
						    	lastActivity._canMoveDown = true;
						    }
						} else {
							lastActivity = null;
							lastTypeCanMove = false;
						}						
					});					
				});				
			};
			$scope.computeCities = function() {
				console.log("computeCities");
				var cityRef = $scope.trip.cityRef;
				console.log("computeCities: starting with ", cityRef.id);
				$.each($scope.trip.days,function (dayKey,day) {		
					console.log('day[' + dayKey + ']._startCityRef changing from ' + day._startCityRef.id + ' to ' + cityRef.id);
					day._startCityRef = cityRef;
					var obsoleteActivities = [];
					$.each(day.activities, function(activityKey,activity) {			
						var startChanged = activity._startCityRef.id != cityRef.id;
						activity._startCityRef = cityRef;
						console.log('day[' + dayKey + '].activities[' + activityKey + ']._startCityRef changing from ' + activity._startCityRef.id + ' to ' + cityRef.id);
						if(activity.type === "visit" && activity.poiRef.id >=0 && startChanged) {
							obsoleteActivities.push(activityKey);							
						} else if(activity.type === 'transport') {
							if(cityRef.id == activity.toCityRef.id) {
								obsoleteActivities.push(activityKey);									
							} else {
								cityRef = activity.toCityRef;
							}
						}
						console.log('day[' + dayKey + '].activities[' + activityKey + ']._endCityRef changing from ' + activity._endCityRef.id + ' to ' + cityRef.id);
						activity._endCityRef = cityRef;
					});
					while(obsoleteActivities.length > 0) {
						var ak = obsoleteActivities.pop();
						day.activities.splice(ak,1);
					}
					console.log('day[' + dayKey + ']._endCityRef changing from ' + day._endCityRef.id + ' to ' + cityRef.id);
					day._endCityRef = cityRef;
				});				
			};
			$scope.moveActivity = function(day,act,delta) {
				if(delta != -1 && delta != 1) { return; }
				if(delta == 1 && !act._canMoveDown) { return; }
				if(delta == -1 && !act._canMoveUp) { return; }
				var i = act.order -1;
				var j = i + delta;
				if(j >=0 && j < day.activities.length) {
					var act1 = day.activities[j];
					day.activities[j] = act;
					day.activities[i] = act1;
					act.order = j + 1;
					act1.order = i + 1;
					var x = act._canMoveUp;
					act._canMoveUp = act1._canMoveUp;
					act1._canMoveUp = x;
					x = act._canMoveDown;
					act._canMoveDown = act1._canMoveDown;
					act1._canMoveDown = x;
				}							
			}; 
			$scope.moveDay = function(day,delta) {
				if(delta != -1 && delta != 1) { return; }
				if(delta == 1 && !day._canMoveDown) { return; }
				if(delta == -1 && !day._canMoveUp) { return; }
				var i = day.num -1;
				var j = i + delta;
				var trip = $scope.trip;
				if(j >=0 && j < trip.days.length) {
					var day1 = trip.days[j];
					trip.days[j] = day;
					trip.days[i] = day1;
					day.num = j + 1;
					day1.num = i + 1;
					var x = day._canMoveUp;
					day._canMoveUp = day1._canMoveUp;
					day1._canMoveUp = x;
					x = day._canMoveDown;
					day._canMoveDown = day1._canMoveDown;
					day1._canMoveDown = x;
				}							
			}; 
			$scope.addActivity = function(day,type) {
				var order = day.activities.length + 1;
				var cityRef = day._startCityRef;
				var canMoveUp = false;				
				if(order > 1) {
					var la = day.activities[order-2];
					cityRef = la._endCityRef;
					canMoveUp = (la.type === "visit" || la.type === "free") &&
					            (type === "visit" || type === "free");
					if(canMoveUp) {
						la._canMoveDown = true;
					}
				}				
				var act = {
					_dayNum : day.num,
					_startCityRef : cityRef,
					_endCityRef : cityRef,
					_canMoveUp : canMoveUp,
					_canMoveDown : false,
					type : type,
					order : order,
					description : '',
					length : 1
				};
				day.activities.push(act);				
				return act;
			};
			
			$scope.reorderStuff = function() {
				$.each($scope.trip.days, function(pos, day) {
					day.num = pos + 1;
					$.each(day.activities, function(pos, act) {
						act.order = pos + 1;
					});
				});
            };
			
			$scope.updateEverything = function() {
				$scope.computeCities();
				$scope.computeMovements();
				$scope.reorderStuff();
			};
		} ]);


function cleanUp(_obj) {
	var clean = function(path,obj) {
		console.log("cleanning " + path);
		for(key in obj) {
			if(key.startsWith("_")) {
				delete obj[key];
			} else if (typeof(obj[key]) === "object") {				
				clean(path + "." + key, obj[key]);
			}
		}		
	};
	var obj = JSON.parse(JSON.stringify(_obj));
	clean('',obj);
	return obj;
}




