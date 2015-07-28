'use strict';

window.ngSelectize = angular.module('ng-selectize', ['ng']);


window.ngSelectize.directive('selectize', function($timeout) {
  console.log("inside directive constructor");  
  return {
    restrict: 'A',
    require: 'ngModel',
    scope: {
      selectize: '&',
      options: '&',
      ngModelOptions: '&'
    },
    link: function(scope, element, attrs, ngModel) {
      var changing, options, selectize, invalidValues = [];
      var data = scope.options();
      var settings = scope.selectize();

      // Default options
      options = angular.extend({
        delimiter: ',',
        persist: true,
        mode: (element[0].tagName === 'SELECT') ? ((element[0].hasAttribute("multiple")) ? 'multi' : 'single') : 'multi' // if element isn't select or has attr 'multiple' set mode 'multi'
      }, settings || {});

      // Activate the widget
      selectize = element.selectize(options)[0].selectize;

      selectize.on('change', function() {
        setModelValue(selectize.getValue());
      });

      function setModelValue(value) {
        if (changing) {
          return;
        }
        /*var oldValue = ngModel.$viewValue;
        if(typeof(oldValue) === "object") {
            oldValue = $(oldValue).clone()[0];
        }
        if(options.valueField) {
            var v = value;
            value = oldValue;
            value[options.valueField] = value;            
            if(options.labelField) {
                var item = selectize.getItem(v);
                var text = item[0].innerText;
                value[options.labelField] = text;
            }
        }*/
        //scope.$parent.$apply(function() {        
          ngModel.$setViewValue(value);
        //});

        if (options.mode === 'single') {
          selectize.blur();
        }
      }

      // Normalize the model value to an array
      function parseValues(value) {
        if(typeof(value) === "function") {
            value = value();
        }
        if (angular.isArray(value)) {
       /*   if(options.valueField) {
        	  return $.map(value, function(v) { return v[options.valueField];});
          } */
          return value;
        }
        if (!value) {
          return [];
        }
        /*if(options.valueField && typeof(value) === "object") {
        	return [value[options.valueField]];
        }*/
        return String(value).split(options.delimiter);
      }

      // Non-strict indexOf
      function indexOfLike(arr, val) {
        for (var i=0; i < arr.length; i++) {
          if (arr[i] == val) {
            return i;
          }
        }
        return -1;
      }

      // Boolean wrapper to indexOfLike
      function contains(arr, val) {
        return indexOfLike(arr, val) !== -1;
      }

      // Store invalid items for late-loading options
      function storeInvalidValues(values, resultValues) {
        values.map(function(val) {
          if (!(contains(resultValues, val) || contains(invalidValues, val))) {
            invalidValues.push(val);
          }
        });
      }

      function restoreInvalidValues(newOptions, values) {
        var i, index;
        for (i=0; i < newOptions.length; i++) {
          index = indexOfLike(invalidValues, newOptions[i][selectize.settings.valueField]);
          if (index !== -1) {
            values.push(newOptions[i][selectize.settings.valueField]);
            invalidValues.splice(index, 1);
          }
        }
      }

      function setSelectizeValue(value) {
        var values = parseValues(value);

        if (changing || values === parseValues(selectize.getValue())) {
          return;
        }

        changing = true;
        $timeout(function() {
            selectize.setValue(values);
            storeInvalidValues(values, parseValues(selectize.getValue()));

            changing = false;    
        });
        
      }

      function setSelectizeOptions(newOptions) {
        var values = parseValues(ngModel.$viewValue);

        if (options.mode === 'multi' && newOptions) {
          restoreInvalidValues(newOptions, values);
        };

        selectize.addOption(newOptions);
        selectize.refreshOptions(false);
        setSelectizeValue(values);
      }

      scope.$parent.$watch(attrs.ngModel, setSelectizeValue);

      if (attrs.options) {
        scope.$parent.$watch(attrs.options, setSelectizeOptions, true);
      }

      scope.$parent.$watch(data, setSelectizeOptions(data), true);

      scope.$on('$destroy', function() {
        selectize.destroy();
      });
    }
  };
});